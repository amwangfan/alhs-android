package com.ailihaosi.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ailihaosi.data.model.AlhsPost
import com.ailihaosi.data.model.PublicUserDto
import com.ailihaosi.data.model.RankingItemDto
import com.ailihaosi.data.model.toAuthor
import com.ailihaosi.data.model.toPost
import com.ailihaosi.data.repo.PostRepository
import com.ailihaosi.ui.components.stripHtml

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingsScreen(
    onBack: () -> Unit,
    onOpenPost: (AlhsPost) -> Unit,
    onOpenAuthor: (authorId: Long, authorName: String) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { PostRepository(appContext = context.applicationContext) }
    var period by remember { mutableStateOf("month") }
    var rangeLabel by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<RankingItemDto>>(emptyList()) }
    var authors by remember { mutableStateOf<List<RankingItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(period) {
        loading = true
        error = null
        val postRes = repo.fetchRankings("posts", period, limit = 30)
        val authorRes = repo.fetchRankings("authors", period, limit = 30)
        val p = postRes.getOrNull()
        val a = authorRes.getOrNull()
        if (p == null && a == null) {
            error = postRes.exceptionOrNull()?.message ?: authorRes.exceptionOrNull()?.message ?: "榜单暂时不可用"
            loading = false
            return@LaunchedEffect
        }
        posts = p?.items.orEmpty()
        authors = a?.items.orEmpty()
        rangeLabel = p?.rangeLabel ?: a?.rangeLabel.orEmpty()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (rangeLabel.isBlank()) "排行" else "排行 · $rangeLabel") },
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
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("month" to "近 90 天", "week" to "近 7 天", "all" to "全部").forEach { (value, label) ->
                    FilterChip(
                        selected = period == value,
                        onClick = { period = value },
                        label = { Text(label) }
                    )
                }
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null && posts.isEmpty() && authors.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { Text(error ?: "", color = MaterialTheme.colorScheme.error) }
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        Text(
                            "文章榜",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (posts.isEmpty()) {
                        item {
                            Text(
                                "暂无本期数据",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        itemsIndexed(posts, key = { _, it -> "p-${it.id}" }) { index, item ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPost(item.toPost()) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "${index + 1}. ${stripHtml(item.title)}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "${item.author.orEmpty()} · 阅读 ${item.views} / 点赞 ${item.likes}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        onOpenAuthor(item.authorId ?: 0L, item.author.orEmpty())
                                    }
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            "作者榜",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }
                    if (authors.isEmpty()) {
                        item {
                            Text(
                                "暂无本期数据",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        itemsIndexed(authors, key = { _, it -> "a-${it.id}" }) { index, item ->
                            val user: PublicUserDto = item.toAuthor()
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenAuthor(user.id, user.displayName) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "${index + 1}. ${user.displayName}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "${user.publications} 篇 · 阅读 ${item.views} / 点赞 ${item.likes}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
