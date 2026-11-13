package com.ailihaosi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ailihaosi.data.model.AlhsPost
import com.ailihaosi.ui.components.PostListItem
import com.ailihaosi.ui.components.StickyBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPost: (AlhsPost) -> Unit,
    onOpenAuthor: (authorId: Long, authorName: String) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    vm: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.endReached, state.loadingMore, state.orderby) {
        if (shouldLoadMore && !state.endReached && !state.loadingMore && !state.loading) {
            vm.loadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("艾利浩斯学院图书馆", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "主站:alhs.xyz  发布页:alhs.live",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
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
            state.loading && state.posts.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            state.error != null && state.posts.isEmpty() -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { vm.refresh() }, modifier = Modifier.padding(top = 12.dp)) {
                        Text("重试")
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.orderby == "date",
                                onClick = { vm.setOrderby("date") },
                                label = { Text("最新") }
                            )
                            FilterChip(
                                selected = state.orderby == "modified",
                                onClick = { vm.setOrderby("modified") },
                                label = { Text("最近修改") }
                            )
                        }
                    }
                    if (state.sticky.isNotEmpty()) {
                        item {
                            StickyBanner(
                                posts = state.sticky,
                                onClick = onOpenPost
                            )
                        }
                    }
                    items(state.posts, key = { it.id }) { post ->
                        PostListItem(
                            post = post,
                            onClick = { onOpenPost(post) },
                            onAuthorClick = {
                                onOpenAuthor(post.authorId, post.authorName)
                            }
                        )
                    }
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                state.loadingMore -> CircularProgressIndicator()
                                state.endReached -> Text(
                                    "没有更多了",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
