package com.ailihaosi.data.model

import com.ailihaosi.data.SiteUrls
import com.google.gson.annotations.SerializedName

data class ApiEnvelope<T>(
    val ok: Boolean = false,
    val data: T? = null,
    val meta: ApiMeta? = null,
    val error: ApiError? = null
)

data class ApiMeta(
    val page: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null,
    val total: Int? = null,
    @SerializedName("total_is_approximate") val totalIsApproximate: Boolean? = null,
    @SerializedName("refresh_seconds") val refreshSeconds: Int? = null,
    @SerializedName("generated_at") val generatedAt: String? = null,
    val stale: Boolean? = null
)

data class ApiError(
    val code: String? = null,
    val message: String? = null
)

data class AuthorRef(
    val id: Long = 0L,
    @SerializedName("display_name") val displayName: String = ""
)

data class TermRef(
    val id: Long = 0L,
    val name: String = "",
    val slug: String = "",
    @SerializedName("post_count") val postCount: Int = 0,
    val key: String? = null,
    val source: String? = null
)

data class PostDto(
    val id: Long = 0L,
    val title: String = "",
    val slug: String = "",
    val excerpt: String = "",
    val author: AuthorRef? = null,
    val status: String = "publish",
    val type: String = "post",
    @SerializedName("date_gmt") val dateGmt: String = "",
    @SerializedName("modified_gmt") val modifiedGmt: String = "",
    @SerializedName("comment_count") val commentCount: Int = 0,
    @SerializedName("view_count") val viewCount: Int = 0,
    @SerializedName("like_count") val likeCount: Int = 0,
    val categories: List<TermRef> = emptyList(),
    val tags: List<TermRef> = emptyList(),
    val series: List<TermRef> = emptyList(),
    @SerializedName("body_pending_r2") val bodyPendingR2: Boolean = false
)

data class BodyDto(
    @SerializedName("post_id") val postId: Long = 0L,
    @SerializedName("body_html") val bodyHtml: String = "",
    @SerializedName("body_source") val bodySource: String = ""
)

data class RecentItemDto(
    val id: Long = 0L,
    val title: String = "",
    val excerpt: String = "",
    @SerializedName("author_id") val authorId: Long? = null,
    val author: String? = null,
    @SerializedName("published_at") val publishedAt: String = "",
    @SerializedName("updated_at") val updatedAt: String = "",
    val type: String = "post",
    @SerializedName("update_kind") val updateKind: String = "",
    val tags: List<TermRef> = emptyList()
)

data class RankingItemDto(
    val id: Long = 0L,
    val title: String = "",
    val name: String = "",
    @SerializedName("author_id") val authorId: Long? = null,
    val author: String? = null,
    @SerializedName("date_gmt") val dateGmt: String = "",
    @SerializedName("published_count") val publishedCount: Int = 0,
    val views: Int = 0,
    val likes: Int = 0
)

data class RankingDto(
    val period: String = "month",
    @SerializedName("range_label") val rangeLabel: String = "",
    val items: List<RankingItemDto> = emptyList()
)

data class AnnouncementDto(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val pinned: Int = 0,
    @SerializedName("created_at") val createdAt: String = ""
)

data class BootstrapDto(
    val recent: List<RecentItemDto> = emptyList(),
    @SerializedName("latest_posts") val latestPosts: List<RecentItemDto> = emptyList(),
    @SerializedName("post_ranking") val postRanking: RankingDto? = null,
    @SerializedName("author_ranking") val authorRanking: RankingDto? = null,
    val activities: List<AnnouncementDto> = emptyList(),
    val announcements: List<AnnouncementDto> = emptyList(),
    val settings: Map<String, Any?> = emptyMap()
)

data class PublicUserDto(
    val id: Long = 0L,
    @SerializedName("display_name") val displayName: String = "",
    val bio: String? = null,
    val publications: Int = 0
)

data class AuthUserDto(
    val id: Long = 0L,
    val login: String = "",
    @SerializedName("display_name") val displayName: String? = null,
    val email: String? = null,
    val role: String = "reader"
)

data class AuthSessionDto(
    val user: AuthUserDto? = null,
    @SerializedName("csrf_token") val csrfToken: String? = null,
    @SerializedName("expires_in") val expiresIn: Int? = null,
    @SerializedName("logged_out") val loggedOut: Boolean? = null
)

data class SeriesChapter(
    val id: Long = 0L,
    val title: String = ""
)

data class SeriesNav(
    val id: Long = 0L,
    val name: String = "",
    val chapters: List<SeriesChapter> = emptyList()
) {
    fun indexOf(postId: Long): Int = chapters.indexOfFirst { it.id == postId }
    fun previous(postId: Long): SeriesChapter? {
        val i = indexOf(postId)
        return if (i > 0) chapters[i - 1] else null
    }
    fun next(postId: Long): SeriesChapter? {
        val i = indexOf(postId)
        return if (i >= 0 && i < chapters.lastIndex) chapters[i + 1] else null
    }
}

data class AlhsPost(
    val id: Long = 0L,
    val title: String = "",
    val excerpt: String = "",
    val contentHtml: String = "",
    val date: String = "",
    val modified: String = "",
    val link: String = "",
    val authorId: Long = 0L,
    val authorName: String = "",
    val categoryIds: List<Long> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val tags: List<TermRef> = emptyList(),
    val series: List<TermRef> = emptyList(),
    val type: String = "post",
    val sticky: Boolean = false,
    val viewCount: Int = 0,
    val likeCount: Int = 0
) {
    val isInteractive: Boolean
        get() = categoryNames.any { it == SiteUrls.SECTION_INTERACTIVE || it.contains("互动") } ||
            title.contains("互动") ||
            contentHtml.contains("ink.js", ignoreCase = true) ||
            contentHtml.contains("story.js", ignoreCase = true)

    val isPage: Boolean get() = type == "page"
}

fun PostDto.toDomain(contentHtml: String = ""): AlhsPost = AlhsPost(
    id = id,
    title = title,
    excerpt = excerpt,
    contentHtml = contentHtml,
    date = dateGmt.ifBlank { modifiedGmt },
    modified = modifiedGmt,
    link = SiteUrls.postUrl(id),
    authorId = author?.id ?: 0L,
    authorName = author?.displayName?.takeIf { it.isNotBlank() } ?: "作者 #${author?.id ?: 0L}",
    categoryIds = categories.map { it.id },
    categoryNames = categories.map { it.name },
    tags = tags,
    series = series,
    type = type,
    viewCount = viewCount,
    likeCount = likeCount
)

fun RecentItemDto.toDomain(): AlhsPost = AlhsPost(
    id = id,
    title = title,
    excerpt = excerpt,
    date = publishedAt.ifBlank { updatedAt },
    modified = updatedAt,
    link = SiteUrls.postUrl(id),
    authorId = authorId ?: 0L,
    authorName = author?.takeIf { it.isNotBlank() } ?: "作者 #${authorId ?: 0L}",
    tags = tags,
    type = type
)

fun RankingItemDto.toPost(): AlhsPost = AlhsPost(
    id = id,
    title = title,
    date = dateGmt,
    link = SiteUrls.postUrl(id),
    authorId = authorId ?: 0L,
    authorName = author?.takeIf { it.isNotBlank() } ?: "作者 #${authorId ?: 0L}",
    sticky = true,
    viewCount = views,
    likeCount = likes
)

fun RankingItemDto.toAuthor(): PublicUserDto = PublicUserDto(
    id = id,
    displayName = name.ifBlank { author.orEmpty() },
    publications = publishedCount
)
