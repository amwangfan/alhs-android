package com.ailihaosi.update

import android.content.Context
import com.ailihaosi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Checks GitHub Releases (never alhs servers) for a newer APK.
 * Unauthenticated (60 req/h/IP), throttled to once per 24h for the automatic check.
 */
object UpdateChecker {
    const val LATEST_URL = "https://api.github.com/repos/amwangfan/alhs-android/releases/latest"
    const val THROTTLE_MS = 24L * 60L * 60L * 1000L
    private const val PREFS = "update_check"
    private const val KEY_LAST_CHECK = "last_check_ms"
    private const val KEY_IGNORED_TAG = "ignored_tag"
    private val USER_AGENT = "AlhsReader/${BuildConfig.VERSION_NAME} (Android; com.ailihaosi; update-check)"

    sealed interface Result {
        data class Newer(val release: ReleaseInfo) : Result
        data class UpToDate(val current: String, val latest: String) : Result
        data class Failed(val message: String) : Result
        /** Auto check skipped (throttled / ignored tag). */
        data object Skipped : Result
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun ignoreTag(context: Context, tag: String) {
        prefs(context).edit().putString(KEY_IGNORED_TAG, tag).apply()
    }

    /** Automatic check on start: throttled, silent on failure, honours ignored tag. */
    suspend fun autoCheck(context: Context): Result {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        val last = p.getLong(KEY_LAST_CHECK, 0L)
        if (last in 1..now && now - last < THROTTLE_MS) return Result.Skipped
        // Record on attempt (even if it later fails) → no retry storm.
        p.edit().putLong(KEY_LAST_CHECK, now).apply()
        val r = fetch()
        if (r is Result.Newer && r.release.tagName == p.getString(KEY_IGNORED_TAG, null)) {
            return Result.Skipped
        }
        return r
    }

    /** Manual check from drawer: not throttled, ignores the ignored tag. */
    suspend fun manualCheck(): Result = fetch()

    private suspend fun fetch(): Result = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(LATEST_URL)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.Failed("HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val info = ReleaseInfo.parse(body)
                    ?: return@withContext Result.Failed("无法解析版本信息")
                val current = BuildConfig.VERSION_NAME
                if (VersionCompare.isNewer(info.version, current)) Result.Newer(info)
                else Result.UpToDate(current, info.version)
            }
        } catch (e: Exception) {
            Result.Failed(e.message ?: e.javaClass.simpleName)
        }
    }
}
