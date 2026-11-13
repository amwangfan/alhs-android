package com.ailihaosi.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ailihaosi.data.model.AlhsPost
import com.ailihaosi.data.repo.PostRepository
import com.ailihaosi.ui.components.PostListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenPost: (AlhsPost) -> Unit,
    onOpenAuthor: (authorId: Long, authorName: String) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { PostRepository(appContext = context.applicationContext) }
    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<AlhsPost>>(emptyList()) }
    var page by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    fun runSearch(text: String) {
        submitted = text.trim()
    }

    LaunchedEffect(submitted) {
        if (submitted.isBlank()) {
            posts = emptyList()
            error = null
            page = 0
            return@LaunchedEffect
        }
        loading = true
        error = null
        repo.fetchPosts(page = 1, search = submitted, preferCache = false)
            .onSuccess {
                posts = it.posts
                page = 1
                totalPages = it.totalPages
                loading = false
                if (it.posts.isEmpty()) error = "没有找到相关文章"
            }
            .onFailure {
                error = it.message
                loading = false
            }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore, page, totalPages, loadingMore, loading, submitted) {
        if (submitted.isNotBlank() && shouldLoadMore && !loading && !loadingMore && page in 1 until totalPages) {
            loadingMore = true
            val next = page + 1
            repo.fetchPosts(page = next, search = submitted, preferCache = false)
                .onSuccess {
                    posts = posts + it.posts
                    page = next
                    totalPages = it.totalPages
                    loadingMore = false
                }
                .onFailure { loadingMore = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { if (it.length <= 100) query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索标题或内容") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { runSearch(query) }, enabled = query.isNotBlank()) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch(query) })
            )
            when {
                loading && posts.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                error != null && posts.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        error ?: "",
                        color = if (submitted.isNotBlank() && posts.isEmpty() && error == "没有找到相关文章") {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else MaterialTheme.colorScheme.error
                    )
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostListItem(
                            post = post,
                            onClick = { onOpenPost(post) },
                            onAuthorClick = { onOpenAuthor(post.authorId, post.authorName) }
                        )
                    }
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            if (loadingMore) CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
