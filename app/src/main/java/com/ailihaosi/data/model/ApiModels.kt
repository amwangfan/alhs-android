package com.ailihaosi.data.model

import com.ailihaosi.data.SiteUrls
import com.google.gson.annotations.SerializedName

/** Envelope used by /api/v1 success/fail helpers. */
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
    val id: Int = 0,
    @SerializedName("display_name") val displayName: String = ""
)

data class TermRef(
    val id: Int = 0,
    val name: String = "",
    val slug: String = ""
)

/** Public post metadata from GET /api/v1/posts[/:id]. */
data class PostDto(
    val id: Int = 0,
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
    @SerializedName("post_id") val postId: Int = 0,
    @SerializedName("body_html") val bodyHtml: String = "",
    @SerializedName("body_source") val bodySource: String = ""
)

/** Static R2 article JSON at static-preview.alhs.xyz/v1/articles/{id}.json */
data class StaticArticleDto(
    val id: Int = 0,
    val title: String = "",
    val excerpt: String = "",
    @SerializedName("author_id") val authorId: Int? = null,
    val author: String? = null,
    @SerializedName("date_gmt") val dateGmt: String = "",
    @SerializedName("date_local") val dateLocal: String = "",
    @SerializedName("modified_gmt") val modifiedGmt: String = "",
    val type: String = "post",
    val available: Boolean = true,
    @SerializedName("body_html") val bodyHtml: String = "",
    @SerializedName("body_source") val bodySource: String = ""
)

data class RecentItemDto(
    val id: Int = 0,
    val title: String = "",
    val excerpt: String = "",
    @SerializedName("author_id") val authorId: Int? = null,
    val author: String? = null,
    @SerializedName("published_at") val publishedAt: String = "",
    @SerializedName("updated_at") val updatedAt: String = "",
    val type: String = "post",
    @SerializedName("update_kind") val updateKind: String = ""
)

data class RankingItemDto(
    val id: Int = 0,
    val title: String = "",
    @SerializedName("author_id") val authorId: Int? = null,
    val author: String? = null,
    @SerializedName("date_gmt") val dateGmt: String = "",
    val views: Int = 0,
    val likes: Int = 0
)

data class RankingDto(
    val period: String = "month",
    @SerializedName("range_label") val rangeLabel: String = "",
    val items: List<RankingItemDto> = emptyList()
)

data class AnnouncementDto(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val pinned: Int = 0
)

data class BootstrapDto(
    val recent: List<RecentItemDto> = emptyList(),
    @SerializedName("post_ranking") val postRanking: RankingDto? = null,
    @SerializedName("author_ranking") val authorRanking: RankingDto? = null,
    val activities: List<AnnouncementDto> = emptyList(),
    val announcements: List<AnnouncementDto> = emptyList(),
    val settings: Map<String, Any?> = emptyMap(),
    @SerializedName("static_ready") val staticReady: Boolean? = null
)

data class AuthUserDto(
    val id: Int = 0,
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

/**
 * Domain post used by Compose UI. Built from PostDto / StaticArticle / cache.
 * Replaces former WordPress WpPost shape.
 */
data class AlhsPost(
    val id: Int = 0,
    val title: String = "",
    val excerpt: String = "",
    val contentHtml: String = "",
    val date: String = "",
    val link: String = "",
    val authorId: Int = 0,
    val authorName: String = "",
    val categoryIds: List<Int> = emptyList(),
    val type: String = "post",
    val sticky: Boolean = false,
    val viewCount: Int = 0
) {
    val isInteractive: Boolean
        get() = categoryIds.contains(SiteUrls.INTERACTIVE_CATEGORY_ID) ||
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
    link = SiteUrls.postUrl(id),
    authorId = author?.id ?: 0,
    authorName = author?.displayName?.takeIf { it.isNotBlank() } ?: "作者 #${author?.id ?: 0}",
    categoryIds = categories.map { it.id },
    type = type,
    viewCount = viewCount
)

fun StaticArticleDto.toDomain(): AlhsPost = AlhsPost(
    id = id,
    title = title,
    excerpt = excerpt,
    contentHtml = bodyHtml,
    date = dateLocal.ifBlank { dateGmt },
    link = SiteUrls.postUrl(id),
    authorId = authorId ?: 0,
    authorName = author?.takeIf { it.isNotBlank() } ?: "作者 #${authorId ?: 0}",
    type = type
)

fun RecentItemDto.toDomain(): AlhsPost = AlhsPost(
    id = id,
    title = title,
    excerpt = excerpt,
    date = publishedAt.ifBlank { updatedAt },
    link = SiteUrls.postUrl(id),
    authorId = authorId ?: 0,
    authorName = author?.takeIf { it.isNotBlank() } ?: "作者 #${authorId ?: 0}",
    type = type
)

fun RankingItemDto.toDomain(): AlhsPost = AlhsPost(
    id = id,
    title = title,
    date = dateGmt,
    link = SiteUrls.postUrl(id),
    authorId = authorId ?: 0,
    authorName = author?.takeIf { it.isNotBlank() } ?: "作者 #${authorId ?: 0}",
    sticky = true,
    viewCount = views
)
