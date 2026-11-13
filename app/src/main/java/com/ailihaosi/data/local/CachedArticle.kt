package com.ailihaosi.data.local

data class CachedArticle(
    val id: Long,
    val title: String,
    val authorName: String = "",
    val authorId: Long = 0L,
    val date: String = "",
    val link: String = "",
    val contentHtml: String = "",
    val excerpt: String = "",
    val isPage: Boolean = false,
    val cachedAtEpochMs: Long = System.currentTimeMillis(),
    val favorited: Boolean = false
)
