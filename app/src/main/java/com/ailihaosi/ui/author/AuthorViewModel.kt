package com.ailihaosi.ui.author

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ailihaosi.data.model.AlhsPost
import com.ailihaosi.data.repo.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthorUiState(
    val authorName: String = "",
    val posts: List<AlhsPost> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false
)

class AuthorViewModel(
    app: Application,
    private val authorId: Int,
    initialName: String,
    private val repo: PostRepository = PostRepository(appContext = app)
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(AuthorUiState(authorName = initialName))
    val state: StateFlow<AuthorUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, page = 0, endReached = false) }
            repo.fetchAuthorPosts(authorId, page = 1)
                .onSuccess { page ->
                    val name = page.posts.firstOrNull()?.authorName?.takeIf { it.isNotBlank() }
                    _state.update {
                        it.copy(
                            posts = page.posts,
                            page = 1,
                            totalPages = page.totalPages,
                            loading = false,
                            endReached = 1 >= page.totalPages,
                            authorName = name ?: it.authorName
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "加载失败")
                    }
                }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || s.endReached) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            val next = s.page + 1
            repo.fetchAuthorPosts(authorId, page = next)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            posts = it.posts + page.posts,
                            page = next,
                            totalPages = page.totalPages,
                            loadingMore = false,
                            endReached = next >= page.totalPages || page.posts.isEmpty()
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loadingMore = false, error = e.message ?: "加载更多失败")
                    }
                }
        }
    }

    companion object {
        fun factory(app: Application, authorId: Int, name: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AuthorViewModel(app, authorId, name) as T
            }
    }
}
