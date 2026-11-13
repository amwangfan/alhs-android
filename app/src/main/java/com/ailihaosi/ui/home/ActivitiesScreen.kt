package com.ailihaosi.ui.home

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ailihaosi.data.model.AnnouncementDto
import com.ailihaosi.data.repo.PostRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { PostRepository(appContext = context.applicationContext) }
    var activities by remember { mutableStateOf<List<AnnouncementDto>>(emptyList()) }
    var announcements by remember { mutableStateOf<List<AnnouncementDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        val boot = repo.fetchBootstrap()
        if (boot.isSuccess) {
            val data = boot.getOrThrow()
            activities = data.activities
            announcements = data.announcements
            if (activities.isEmpty()) {
                repo.fetchActivities().onSuccess { activities = it }
            }
            if (announcements.isEmpty()) {
                repo.fetchAnnouncements().onSuccess { announcements = it }
            }
            loading = false
        } else {
            val a = repo.fetchActivities()
            val n = repo.fetchAnnouncements()
            activities = a.getOrDefault(emptyList())
            announcements = n.getOrDefault(emptyList())
            if (a.isFailure && n.isFailure) {
                error = a.exceptionOrNull()?.message ?: "暂时无法加载"
            }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("活动与公告") },
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
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null && activities.isEmpty() && announcements.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) { Text(error ?: "", color = MaterialTheme.colorScheme.error) }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        "活动",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                if (activities.isEmpty()) {
                    item {
                        Text(
                            "暂无正在展示的内容",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    items(activities, key = { "act-${it.id}-${it.title}" }) { item ->
                        AnnouncementBlock(item)
                    }
                }
                item {
                    Text(
                        "公告",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                if (announcements.isEmpty()) {
                    item {
                        Text(
                            "暂无正在展示的内容",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    items(announcements, key = { "ann-${it.id}-${it.title}" }) { item ->
                        AnnouncementBlock(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementBlock(item: AnnouncementDto) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = (if (item.pinned != 0) "★ " else "") + item.title,
            style = MaterialTheme.typography.titleSmall
        )
        if (item.content.isNotBlank()) {
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (item.createdAt.isNotBlank()) {
            Text(
                text = item.createdAt,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
