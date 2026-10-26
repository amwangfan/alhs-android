package com.ailihaosi.data.local

import android.content.Context
import com.ailihaosi.data.model.AlhsPost
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Long-TTL disk snapshots for list/bootstrap payloads (stale-while-revalidate).
 * No polling; callers decide when to revalidate.
 */
class ListSnapshotCache(context: Context) {
    private val app = context.applicationContext
    private val gson = Gson()
    private val dir = File(app.filesDir, "list_snapshots").also { it.mkdirs() }
    private val snapshotType = object : TypeToken<Snapshot>() {}.type

    data class Snapshot(
        val posts: List<AlhsPost> = emptyList(),
        val total: Int = 0,
        val totalPages: Int = 1,
        val savedAtEpochMs: Long = System.currentTimeMillis()
    )

    fun save(key: String, snapshot: Snapshot) {
        File(dir, safe(key)).writeText(gson.toJson(snapshot.copy(savedAtEpochMs = System.currentTimeMillis())))
    }

    fun get(key: String, maxAgeMs: Long = DEFAULT_TTL_MS): Snapshot? {
        val f = File(dir, safe(key))
        if (!f.exists()) return null
        return runCatching {
            gson.fromJson<Snapshot>(f.readText(), snapshotType)
        }.getOrNull()
    }

    fun isFresh(key: String, maxAgeMs: Long = DEFAULT_TTL_MS): Boolean {
        val snap = get(key) ?: return false
        return System.currentTimeMillis() - snap.savedAtEpochMs < maxAgeMs
    }

    fun ageMs(key: String): Long? {
        val snap = get(key) ?: return null
        return System.currentTimeMillis() - snap.savedAtEpochMs
    }

    private fun safe(key: String) = key.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".json"

    companion object {
        /** 6 hours — list snapshots are cheap to keep; revalidate on pull-to-refresh. */
        const val DEFAULT_TTL_MS = 6L * 60L * 60L * 1000L
        const val BOOTSTRAP_TTL_MS = 30L * 60L * 1000L

        @Volatile private var instance: ListSnapshotCache? = null
        fun get(context: Context): ListSnapshotCache =
            instance ?: synchronized(this) {
                instance ?: ListSnapshotCache(context).also { instance = it }
            }
    }
}
