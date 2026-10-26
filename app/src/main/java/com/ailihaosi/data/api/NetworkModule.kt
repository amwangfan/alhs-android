package com.ailihaosi.data.api

import android.content.Context
import com.ailihaosi.data.SiteUrls
import okhttp3.Cache
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object NetworkModule {
    const val API_BASE = SiteUrls.API_V1
    const val STATIC_BASE = SiteUrls.STATIC
    const val USER_AGENT = "AlhsReader/0.3 (Android; com.ailihaosi)"

    @Volatile private var appContext: Context? = null

    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            return cookieStore[url.host].orEmpty().filter { it.expiresAt >= now }
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun cacheDir(): File? {
        val ctx = appContext ?: return null
        return File(ctx.cacheDir, "okhttp_api").also { it.mkdirs() }
    }

    private val okHttp: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
        cacheDir()?.let { dir ->
            builder.cache(Cache(dir, 10L * 1024L * 1024L))
        }
        builder.build()
    }

    val api: AlhsApi by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AlhsApi::class.java)
    }

    val staticApi: StaticApi by lazy {
        Retrofit.Builder()
            .baseUrl(STATIC_BASE)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StaticApi::class.java)
    }

    fun clearCookies() {
        cookieStore.clear()
    }
}
