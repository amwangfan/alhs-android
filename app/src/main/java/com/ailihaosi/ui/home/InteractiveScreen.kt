package com.ailihaosi.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ailihaosi.data.model.AlhsPost
import com.ailihaosi.data.repo.PostRepository
import com.ailihaosi.ui.browser.openInCustomTab
import com.ailihaosi.ui.components.PostListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveScreen(
    onBack: () -> Unit,
    onOpenPost: (AlhsPost) -> Unit
) {
    val context = LocalContext.current
    var posts by remember { mutableStateOf<List<AlhsPost>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }
    val repo = remember { PostRepository(appContext = context.applicationContext) }

    LaunchedEffect(reloadToken) {
        loading = true
        error = null
        repo.fetchInteractive()
            .onSuccess {
                posts = it.posts
                loading = false
            }
            .onFailure {
                error = it.message
                loading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("互动小说") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            error != null && posts.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
                Button(
                    onClick = { reloadToken++ },
                    modifier = Modifier.padding(top = 12.dp)
                ) { Text("重试") }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        text = "分类「互动小说」；条目默认用浏览器打开（含 JS 互动）。点标题旁也可进详情预览正文。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(posts, key = { it.id }) { post ->
                    PostListItem(
                        post = post,
                        onClick = {
                            val url = post.link.ifBlank {
                                com.ailihaosi.data.SiteUrls.postUrl(post.id)
                            }
                            openInCustomTab(context, url)
                        },
                        onAuthorClick = { onOpenPost(post) }
                    )
                }
            }
        }
    }
}
