package com.ailihaosi.ui.home

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

data class HomeUiState(
    val posts: List<AlhsPost> = emptyList(),
    val sticky: List<AlhsPost> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false,
    val fromCache: Boolean = false
)

class HomeViewModel(
    app: Application,
    private val repo: PostRepository = PostRepository(appContext = app)
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // Instant paint from disk snapshot when available, then revalidate.
        repo.cachedHomePosts()?.let { cached ->
            _state.update {
                it.copy(
                    posts = cached.posts,
                    page = 1,
                    totalPages = cached.totalPages,
                    fromCache = true,
                    endReached = 1 >= cached.totalPages
                )
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = it.posts.isEmpty(), error = null, page = 0, endReached = false) }
            val stickyJob = launch {
                repo.fetchRankedPosts(limit = 5).onSuccess { list ->
                    _state.update { it.copy(sticky = list) }
                }
            }
            val result = repo.fetchPosts(page = 1, perPage = 20, preferCache = false)
            stickyJob.join()
            result
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            posts = page.posts,
                            page = 1,
                            totalPages = page.totalPages,
                            loading = false,
                            fromCache = page.fromCache,
                            endReached = 1 >= page.totalPages || page.posts.isEmpty()
                        )
                    }
                }
                .onFailure { e ->
                    if (_state.value.posts.isEmpty()) {
                        _state.update {
                            it.copy(loading = false, error = e.message ?: "加载失败")
                        }
                    } else {
                        _state.update { it.copy(loading = false, error = null) }
                    }
                }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || s.endReached) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, error = null) }
            val next = s.page + 1
            repo.fetchPosts(page = next, perPage = 20, preferCache = false)
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
        fun factory(app: Application) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(app) as T
            }
    }
}
