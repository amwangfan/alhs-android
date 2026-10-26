package com.ailihaosi.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ailihaosi.data.SiteUrls
import com.ailihaosi.data.local.ArticleCacheStore
import com.ailihaosi.data.local.CachedArticle
import com.ailihaosi.data.model.AlhsPost
import com.ailihaosi.data.repo.PostRepository
import com.ailihaosi.ui.components.stripHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DetailUiState(
    val post: AlhsPost? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val fromCache: Boolean = false,
    val isCached: Boolean = false,
    val isFavorite: Boolean = false,
    val isPage: Boolean = false
)

class DetailViewModel(
    app: Application,
    private val contentId: Int,
    private val isPage: Boolean,
    private val repo: PostRepository = PostRepository(appContext = app)
) : AndroidViewModel(app) {

    private val cache = ArticleCacheStore.get(app)

    private val _state = MutableStateFlow(DetailUiState(isPage = isPage))
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        load(preferNetwork = true)
    }

    fun load(preferNetwork: Boolean = true) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    isFavorite = cache.isFavorite(contentId),
                    isCached = cache.isCached(contentId)
                )
            }
            // Always try local first for instant paint / offline.
            val local = withContext(Dispatchers.IO) { cache.get(contentId) }
            if (local != null && !preferNetwork) {
                _state.update {
                    it.copy(
                        post = local.toAlhsPost(),
                        loading = false,
                        fromCache = true,
                        isCached = true,
                        isFavorite = local.favorited || cache.isFavorite(contentId)
                    )
                }
                return@launch
            }
            if (local != null && preferNetwork) {
                // Show stale while revalidating.
                _state.update {
                    it.copy(
                        post = local.toAlhsPost(),
                        loading = false,
                        fromCache = true,
                        isCached = true,
                        isFavorite = cache.isFavorite(contentId)
                    )
                }
            }
            val result = repo.fetchPostWithBody(contentId)
            result
                .onSuccess { post ->
                    val typed = post.copy(type = if (isPage) "page" else post.type)
                    withContext(Dispatchers.IO) {
                        cache.save(typed.toCached(isPage))
                    }
                    _state.update {
                        it.copy(
                            post = typed,
                            loading = false,
                            fromCache = false,
                            isCached = true,
                            isFavorite = cache.isFavorite(contentId),
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    if (local != null) {
                        _state.update {
                            it.copy(
                                post = local.toAlhsPost(),
                                loading = false,
                                fromCache = true,
                                isCached = true,
                                error = null,
                                isFavorite = cache.isFavorite(contentId)
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(loading = false, error = e.message ?: "加载失败")
                        }
                    }
                }
        }
    }

    fun toggleFavorite() {
        val post = _state.value.post ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val next = !cache.isFavorite(contentId)
            cache.setFavorite(contentId, next, seed = post.toCached(isPage))
            if (next && !cache.isCached(contentId)) {
                cache.save(post.toCached(isPage), markFavorite = true)
            }
            _state.update {
                it.copy(isFavorite = next, isCached = cache.isCached(contentId))
            }
        }
    }

    fun cacheNow() {
        val post = _state.value.post ?: return
        viewModelScope.launch(Dispatchers.IO) {
            cache.save(post.toCached(isPage))
            _state.update { it.copy(isCached = true) }
        }
    }

    companion object {
        fun factory(app: Application, id: Int, isPage: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DetailViewModel(app, id, isPage) as T
            }
    }
}

fun AlhsPost.toCached(isPage: Boolean = false) = CachedArticle(
    id = id,
    title = stripHtml(title),
    authorName = authorName,
    authorId = authorId,
    date = date,
    link = link.ifBlank { SiteUrls.postUrl(id) },
    contentHtml = contentHtml,
    excerpt = stripHtml(excerpt),
    isPage = isPage || this.isPage
)

fun CachedArticle.toAlhsPost() = AlhsPost(
    id = id,
    title = title,
    excerpt = excerpt,
    contentHtml = contentHtml,
    date = date,
    link = link.ifBlank { SiteUrls.postUrl(id) },
    authorId = authorId,
    authorName = authorName,
    type = if (isPage) "page" else "post"
)
