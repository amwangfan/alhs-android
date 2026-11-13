package com.ailihaosi.data

object SiteUrls {
    const val ORIGIN = "https://alhs.xyz"
    const val HOME = "https://alhs.xyz/"
    const val API_V1 = "https://alhs.xyz/api/v1/"
    const val LIVE = "https://alhs.live/"
    const val LOGIN = "https://alhs.xyz/login"
    const val REGISTER = "https://alhs.xyz/register"
    const val ACCOUNT = "https://alhs.xyz/account"
    const val DASHBOARD = "https://alhs.xyz/dashboard"
    const val ACTIVITIES = "https://alhs.xyz/activities"
    const val RANKINGS = "https://alhs.xyz/rankings"
    const val GITHUB_RELEASES = "https://github.com/amwangfan/alhs-android/releases/latest"

    const val SECTION_ORIGINALS = "原创小说"
    const val SECTION_FEATURED = "图书馆精选"
    const val SECTION_HISTORY = "校史馆"
    const val SECTION_NOTICE = "公告"
    const val SECTION_INTERACTIVE = "互动小说"

    fun postUrl(id: Long): String = "$ORIGIN/posts/$id"
    fun authorUrl(id: Long): String = "$ORIGIN/author?id=$id"
}
