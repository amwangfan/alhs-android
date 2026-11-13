package com.ailihaosi.data.repo

import android.content.Context
import com.ailihaosi.data.api.AlhsApi
import com.ailihaosi.data.api.NetworkModule
import com.ailihaosi.data.local.ListSnapshotCache
import com.ailihaosi.data.model.AlhsPost
import com.ailihaosi.data.model.AnnouncementDto
import com.ailihaosi.data.model.BootstrapDto
import com.ailihaosi.data.model.PublicUserDto
import com.ailihaosi.data.model.RankingDto
import com.ailihaosi.data.model.SeriesChapter
import com.ailihaosi.data.model.SeriesNav
import com.ailihaosi.data.model.TermRef
import com.ailihaosi.data.model.toDomain
import com.ailihaosi.data.model.toPost
import kotlin.math.ceil

data class PageResult(
    val posts: List<AlhsPost>,
    val totalPages: Int,
    val total: Int,
    val fromCache: Boolean = false
)

class PostRepository(
    private val api: AlhsApi = NetworkModule.api,
    private val appContext: Context? = null
) {
    private val snapshots: ListSnapshotCache?
        get() = appContext?.let { ListSnapshotCache.get(it) }

    @Volatile private var categoryCache: List<TermRef>? = null

    private fun <T> unwrap(resp: retrofit2.Response<com.ailihaosi.data.model.ApiEnvelope<T>>): T {
        if (!resp.isSuccessful) error("HTTP ${resp.code()}: ${resp.message()}")
        val env = resp.body() ?: error("Empty body")
        if (!env.ok) error(env.error?.message ?: env.error?.code ?: "请求失败")
        return env.data ?: error("Empty data")
    }

    suspend fun fetchPosts(
        page: Int,
        perPage: Int = 20,
        category: Long? = null,
        author: Long? = null,
        tag: Long? = null,
        series: String? = null,
        search: String? = null,
        type: String = "post",
        orderby: String = "date",
        order: String = "desc",
        preferCache: Boolean = true
    ): Result<PageResult> = runCatching {
        val cacheKey = "posts_p${page}_n${perPage}_c${category}_a${author}_t${tag}_s${series}_q${search}_y${type}_o${orderby}_r${order}"
        val resp = api.listPosts(
            page = page,
            perPage = perPage,
            type = type,
            orderby = orderby,
            order = order,
            author = author,
            category = category,
            tag = tag,
            series = series,
            search = search
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
        val cacheKey = "posts_p${page}_n${perPage}_c${category}_a${author}_t${tag}_s${series}_q${search}_y${type}_o${orderby}_r${order}"
        val snap = if (preferCache) snapshots?.get(cacheKey) else null
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

    fun cachedHomePosts(perPage: Int = 20, orderby: String = "date"): PageResult? {
        val snap = snapshots?.get("posts_p1_n${perPage}_cnull_anull_tnull_snull_qnull_ypost_o${orderby}_rdesc")
            ?: return null
        if (snap.posts.isEmpty()) return null
        return PageResult(snap.posts, snap.totalPages, snap.total, fromCache = true)
    }

    suspend fun fetchBootstrap(): Result<BootstrapDto> = runCatching {
        unwrap(api.bootstrap()).also { boot ->
            val ranked = boot.postRanking?.items?.map { it.toPost() }.orEmpty()
            if (ranked.isNotEmpty()) {
                snapshots?.save(
                    "rankings_posts_month",
                    ListSnapshotCache.Snapshot(posts = ranked, total = ranked.size, totalPages = 1)
                )
            }
        }
    }

    suspend fun fetchRankedPosts(period: String = "month", limit: Int = 8): Result<List<AlhsPost>> = runCatching {
        val key = "rankings_posts_$period"
        val cached = snapshots?.get(key)
        try {
            val ranking = unwrap(api.rankings("posts", period = period, limit = limit))
            val posts = ranking.items.map { it.toPost() }
            snapshots?.save(
                key,
                ListSnapshotCache.Snapshot(posts = posts, total = posts.size, totalPages = 1)
            )
            posts
        } catch (e: Exception) {
            cached?.posts?.takeIf { it.isNotEmpty() } ?: throw e
        }
    }

    suspend fun fetchRankings(mode: String, period: String, limit: Int = 30): Result<RankingDto> = runCatching {
        unwrap(api.rankings(mode, period = period, limit = limit))
    }

    suspend fun fetchAnnouncements(): Result<List<AnnouncementDto>> = runCatching {
        unwrap(api.announcements())
    }

    suspend fun fetchActivities(): Result<List<AnnouncementDto>> = runCatching {
        unwrap(api.activityBar())
    }

    suspend fun fetchPostWithBody(id: Long): Result<AlhsPost> = runCatching {
        val meta = unwrap(api.getPost(id))
        val body = unwrap(api.getPostBody(id))
        meta.toDomain(contentHtml = body.bodyHtml)
    }

    suspend fun fetchPostMeta(id: Long): Result<AlhsPost> = runCatching {
        unwrap(api.getPost(id)).toDomain()
    }

    suspend fun fetchRandomPost(): Result<AlhsPost> = runCatching {
        val recent = runCatching { unwrap(api.recentPosts(limit = 50)) }.getOrNull()
        val fromRecent = recent?.map { it.toDomain() }.orEmpty()
        if (fromRecent.isNotEmpty()) return@runCatching fromRecent.random()

        val cached = snapshots?.get("posts_p1_n20_cnull_anull_tnull_snull_qnull_ypost_odate_rdesc")?.posts.orEmpty()
        if (cached.isNotEmpty()) return@runCatching cached.random()

        val page = fetchPosts(page = 1, perPage = 20, preferCache = false).getOrThrow()
        page.posts.randomOrNull() ?: error("没有可用文章")
    }

    suspend fun fetchAuthorPosts(authorId: Long, page: Int, perPage: Int = 20): Result<PageResult> =
        fetchPosts(page = page, perPage = perPage, author = authorId, preferCache = page == 1)

    suspend fun fetchUser(id: Long): Result<PublicUserDto> = runCatching {
        unwrap(api.getUser(id))
    }

    suspend fun categories(force: Boolean = false): Result<List<TermRef>> = runCatching {
        val cached = categoryCache
        if (!force && cached != null) return@runCatching cached
        unwrap(api.categories()).also { categoryCache = it }
    }

    suspend fun resolveCategory(name: String): Result<TermRef> = runCatching {
        val list = categories().getOrThrow()
        list.find { it.name == name || it.slug == name }
            ?: unwrap(api.categories(q = name)).find { it.name == name || it.slug == name }
            ?: error("未找到栏目「$name」")
    }

    suspend fun resolvePage(title: String): Result<AlhsPost> = runCatching {
        val page = fetchPosts(
            page = 1,
            perPage = 50,
            type = "page",
            search = title,
            preferCache = false
        ).getOrThrow()
        page.posts.find { it.title == title } ?: error("未找到页面「$title」")
    }

    suspend fun fetchSeriesNav(series: TermRef): Result<SeriesNav> = runCatching {
        val key = series.key?.takeIf { it.isNotBlank() }
            ?: series.id.toString()
        val collected = mutableListOf<AlhsPost>()
        var pageNo = 1
        var totalPages = 1
        while (pageNo <= totalPages && pageNo <= 20) {
            val chunk = fetchPosts(
                page = pageNo,
                perPage = 100,
                series = key,
                orderby = "date",
                order = "asc",
                preferCache = false
            ).getOrThrow()
            collected += chunk.posts
            totalPages = chunk.totalPages
            pageNo++
        }
        val unique = collected.distinctBy { it.id }
        SeriesNav(
            id = series.id,
            name = series.name.ifBlank { "系列" },
            chapters = unique.map { SeriesChapter(id = it.id, title = it.title) }
        )
    }

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
