package com.ailihaosi.data.local

data class CachedArticle(
    val id: Int,
    val title: String,
    val authorName: String = "",
    val authorId: Int = 0,
    val date: String = "",
    val link: String = "",
    val contentHtml: String = "",
    val excerpt: String = "",
    val isPage: Boolean = false,
    val cachedAtEpochMs: Long = System.currentTimeMillis(),
    val favorited: Boolean = false
)
