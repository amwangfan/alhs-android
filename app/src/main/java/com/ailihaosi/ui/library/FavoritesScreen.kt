package com.ailihaosi.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ailihaosi.data.local.ArticleCacheStore
import com.ailihaosi.data.local.CachedArticle
import com.ailihaosi.ui.components.formatWpDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onOpenCached: (CachedArticle) -> Unit
) {
    val context = LocalContext.current
    val store = remember { ArticleCacheStore.get(context) }
    var items by remember { mutableStateOf<List<CachedArticle>>(emptyList()) }
    var showCached by remember { mutableStateOf(false) }

    fun reload() {
        items = if (showCached) store.listCached() else store.listFavorites()
    }

    LaunchedEffect(showCached) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (showCached) "离线缓存" else "收藏")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showCached = !showCached
                    }) {
                        Text(
                            if (showCached) "收藏" else "缓存",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    IconButton(onClick = {
                        store.clearAll(clearFavorites = !showCached)
                        reload()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "清空")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (showCached) "暂无离线缓存\n打开文章会自动缓存正文"
                    else "暂无收藏\n在阅读页点心形即可收藏（需已缓存正文）",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        text = if (showCached) {
                            "共 ${items.size} 篇 · 约 ${store.cacheBytes() / 1024} KB · 点垃圾桶清空全部缓存"
                        } else {
                            "共 ${items.size} 篇 · 收藏复用本地缓存正文，无网可离线阅读"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(items, key = { it.id }) { article ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCached(article) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(article.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${article.authorName} · ${formatWpDate(article.date)}" +
                                if (article.isPage) " · 页面" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (article.excerpt.isNotBlank()) {
                            Text(
                                article.excerpt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
