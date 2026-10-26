package com.ailihaosi.ui.nav

import android.net.Uri

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Detail : Routes("detail/{contentId}") {
        fun create(contentId: Int) = "detail/$contentId"
    }
    data object Page : Routes("page/{contentId}") {
        fun create(contentId: Int) = "page/$contentId"
    }
    data object Author : Routes("author/{authorId}/{authorName}") {
        fun create(authorId: Int, authorName: String): String =
            "author/$authorId/${Uri.encode(authorName)}"
    }
    data object Interactive : Routes("interactive")
    data object Favorites : Routes("favorites")
    data object Category : Routes("category/{categoryId}/{title}") {
        fun create(categoryId: Int, title: String) =
            "category/$categoryId/${Uri.encode(title)}"
    }
}
