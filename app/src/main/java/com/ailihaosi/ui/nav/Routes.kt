package com.ailihaosi.ui.nav

import android.net.Uri

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Detail : Routes("detail/{contentId}") {
        fun create(contentId: Long) = "detail/$contentId"
    }
    data object Page : Routes("page/{contentId}") {
        fun create(contentId: Long) = "page/$contentId"
    }
    data object Author : Routes("author/{authorId}/{authorName}") {
        fun create(authorId: Long, authorName: String): String =
            "author/$authorId/${Uri.encode(authorName.ifBlank { "作者" })}"
    }
    data object Interactive : Routes("interactive")
    data object Favorites : Routes("favorites")
    data object Category : Routes("category/{categoryId}/{title}") {
        fun create(categoryId: Long, title: String) =
            "category/$categoryId/${Uri.encode(title)}"
    }
    data object Search : Routes("search")
    data object Rankings : Routes("rankings")
    data object Activities : Routes("activities")
    data object Sections : Routes("sections")
}
