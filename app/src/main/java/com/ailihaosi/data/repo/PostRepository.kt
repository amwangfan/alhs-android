package com.ailihaosi.data.repo

import android.content.Context
import com.ailihaosi.data.SiteUrls
import com.ailihaosi.data.api.AlhsApi
import com.ailihaosi.data.api.NetworkModule
import com.ailihaosi.data.api.StaticApi
import com.ailihaosi.data.local.ListSnapshotCache
import com.ailihaosi.data.model.AlhsPost
import com.ailihaosi.data.model.AnnouncementDto
import com.ailihaosi.data.model.BootstrapDto
import com.ailihaosi.data.model.toDomain
import kotlin.math.ceil

data class PageResult(
    val posts: List<AlhsPost>,
    val totalPages: Int,
    val total: Int,
    val fromCache: Boolean = false
)

class PostRepository(
    private val api: AlhsApi = NetworkModule.api,
    private val staticApi: StaticApi = NetworkModule.staticApi,
    private val appContext: Context? = null
) {
    private val snapshots: ListSnapshotCache?
        get() = appContext?.let { ListSnapshotCache.get(it) }

    private fun <T> unwrap(resp: retrofit2.Response<com.ailihaosi.data.model.ApiEnvelope<T>>): T {
        if (!resp.isSuccessful) error("HTTP ${resp.code()}: ${resp.message()}")
        val env = resp.body() ?: error("Empty body")
        if (!env.ok) error(env.error?.message ?: env.error?.code ?: "请求失败")
        return env.data ?: error("Empty data")
    }

    suspend fun fetchPosts(
        page: Int,
        perPage: Int = 20,
        category: Int? = null,
        author: Int? = null,
        preferCache: Boolean = true
    ): Result<PageResult> = runCatching {
        val cacheKey = "posts_p${page}_n${perPage}_c${category}_a${author}"
        if (preferCache) {
            snapshots?.get(cacheKey)?.takeIf { it.posts.isNotEmpty() }?.let { snap ->
                // Stale-while-revalidate: return cache immediately only when caller
                // wants cache-first AND we later revalidate from UI refresh.
                // For loadMore we still hit network; for page-1 we may show stale.
            }
        }
        val resp = api.listPosts(
            page = page,
            perPage = perPage,
            category = category,
            author = author
        )
        val data = unwrap(resp)
        val meta = resp.body()?.meta
        val total = meta?.total ?: data.size
        val totalPages = ceil(total.toDouble() / perPage).toInt().coerceAtLeast(1)
        val posts = data.map { it.toDomain() }
        snapshots?.save(
            cacheKey,
            ListSnapshotCache.Snapshot(posts = posts, total = total, totalPages = totalPages)
        )
        PageResult(posts = posts, totalPages = totalPages, total = total)
    }.recoverCatching { e ->
        val cacheKey = "posts_p${page}_n${perPage}_c${category}_a${author}"
        val snap = snapshots?.get(cacheKey)
        if (snap != null && snap.posts.isNotEmpty()) {
            PageResult(
                posts = snap.posts,
                totalPages = snap.totalPages,
                total = snap.total,
                fromCache = true
            )
        } else {
            throw e
        }
    }

    /** Cached page-1 snapshot for instant home paint (may be stale). */
    fun cachedHomePosts(perPage: Int = 20): PageResult? {
        val snap = snapshots?.get("posts_p1_n${perPage}_cnull_anull") ?: return null
        if (snap.posts.isEmpty()) return null
        return PageResult(snap.posts, snap.totalPages, snap.total, fromCache = true)
    }

    suspend fun fetchBootstrap(preferCache: Boolean = true): Result<BootstrapDto> = runCatching {
        val key = "bootstrap"
        if (preferCache) {
            // Soft hint only — still fetch below unless we add typed bootstrap cache.
        }
        unwrap(api.bootstrap()).also { boot ->
            // Persist ranking items as sticky substitutes.
            val ranked = boot.postRanking?.items?.map { it.toDomain() }.orEmpty()
            if (ranked.isNotEmpty()) {
                snapshots?.save(
                    "rankings_posts_month",
                    ListSnapshotCache.Snapshot(posts = ranked, total = ranked.size, totalPages = 1)
                )
            }
        }
    }

    suspend fun fetchRankedPosts(limit: Int = 8): Result<List<AlhsPost>> = runCatching {
        val cached = snapshots?.get("rankings_posts_month")
        try {
            val ranking = unwrap(api.rankings("posts", period = "month", limit = limit))
            val posts = ranking.items.map { it.toDomain() }
            snapshots?.save(
                "rankings_posts_month",
                ListSnapshotCache.Snapshot(posts = posts, total = posts.size, totalPages = 1)
            )
            posts
        } catch (e: Exception) {
            cached?.posts?.takeIf { it.isNotEmpty() } ?: throw e
        }
    }

    suspend fun fetchAnnouncements(): Result<List<AnnouncementDto>> = runCatching {
        unwrap(api.announcements())
    }

    /**
     * One-shot article open: prefer static R2 JSON (zero D1), else /api/v1 body + meta.
     * Never re-fetched on scroll — caller caches locally.
     */
    suspend fun fetchPostWithBody(id: Int): Result<AlhsPost> = runCatching {
        // 1) Static public article (preferred)
        val staticResp = runCatching { staticApi.article(id) }.getOrNull()
        if (staticResp != null && staticResp.isSuccessful) {
            val body = staticResp.body()
            if (body != null && body.available && body.bodyHtml.isNotBlank()) {
                return@runCatching body.toDomain()
            }
        }
        // 2) API meta + body
        val meta = unwrap(api.getPost(id))
        val body = unwrap(api.getPostBody(id))
        meta.toDomain(contentHtml = body.bodyHtml)
    }

    suspend fun fetchPostMeta(id: Int): Result<AlhsPost> = runCatching {
        unwrap(api.getPost(id)).toDomain()
    }

    /** Random from a single recent/list page — no spam of catalog fetches. */
    suspend fun fetchRandomPost(): Result<AlhsPost> = runCatching {
        val recent = runCatching { unwrap(api.recentPosts(limit = 50)) }.getOrNull()
        val fromRecent = recent?.map { it.toDomain() }.orEmpty()
        if (fromRecent.isNotEmpty()) return@runCatching fromRecent.random()

        val cached = snapshots?.get("posts_p1_n20_cnull_anull")?.posts.orEmpty()
        if (cached.isNotEmpty()) return@runCatching cached.random()

        val page = fetchPosts(page = 1, perPage = 20, preferCache = false).getOrThrow()
        page.posts.randomOrNull() ?: error("没有可用文章")
    }

    suspend fun fetchAuthorPosts(authorId: Int, page: Int, perPage: Int = 20): Result<PageResult> =
        fetchPosts(page = page, perPage = perPage, author = authorId, preferCache = page == 1)

    suspend fun fetchInteractive(page: Int = 1): Result<PageResult> =
        fetchPosts(page = page, perPage = 20, category = SiteUrls.INTERACTIVE_CATEGORY_ID)

    suspend fun fetchNoticesAsPosts(page: Int = 1): Result<PageResult> =
        fetchPosts(page = page, perPage = 20, category = SiteUrls.NOTICE_CATEGORY_ID)

    /** Session probe — Custom Tabs login won't share cookies; always null unless native login used. */
    suspend fun currentUser(): Result<com.ailihaosi.data.model.AuthUserDto?> = runCatching {
        val resp = api.me()
        if (resp.code() == 401) return@runCatching null
        unwrap(resp)
    }

    suspend fun logout(): Result<Unit> = runCatching {
        runCatching { api.logout() }
        NetworkModule.clearCookies()
    }
}
