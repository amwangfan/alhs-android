package com.ailihaosi.data

/**
 * Preview-stack hosts for v0.3+. WP (alhs.xyz/wp-json) is retired for this client.
 * Do not point download links at welcome.alhs.xyz from this app.
 */
object SiteUrls {
    const val PREVIEW = "https://alpha-preview.alhs.xyz/"
    const val STATIC = "https://static-preview.alhs.xyz/"
    const val API_V1 = "https://alpha-preview.alhs.xyz/api/v1/"
    const val HOME = PREVIEW
    /** Public notice / status page (unchanged). */
    const val LIVE = "https://alhs.live/"
    const val LOGIN = "https://alpha-preview.alhs.xyz/login"
    const val REGISTER = "https://alpha-preview.alhs.xyz/register"
    const val ACCOUNT = "https://alpha-preview.alhs.xyz/account"
    const val DASHBOARD = "https://alpha-preview.alhs.xyz/dashboard"
    const val ACTIVITIES = "https://alpha-preview.alhs.xyz/activities"
    const val RANKINGS = "https://alpha-preview.alhs.xyz/rankings"
    const val GITHUB_RELEASES = "https://github.com/amwangfan/alhs-android/releases/latest"

    /** Section pages still published as posts on the preview stack (same IDs as web IA). */
    const val HISTORY_PAGE_ID = 1690
    const val FEATURED_PAGE_ID = 3865

    /** Taxonomy ids mirrored from preview catalogs. */
    const val NOTICE_CATEGORY_ID = 3
    const val INTERACTIVE_CATEGORY_ID = 1070

    fun postUrl(id: Int): String = "https://alpha-preview.alhs.xyz/posts/$id"
}
