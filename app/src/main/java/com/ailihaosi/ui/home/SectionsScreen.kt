package com.ailihaosi.ui.home

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
import com.ailihaosi.data.model.TermRef
import com.ailihaosi.data.repo.PostRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionsScreen(
    onBack: () -> Unit,
    onOpenCategory: (id: Long, name: String) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { PostRepository(appContext = context.applicationContext) }
    var items by remember { mutableStateOf<List<TermRef>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        repo.categories(force = reload > 0)
            .onSuccess {
                items = it
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
                title = { Text("栏目") },
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
            loading && items.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            error != null && items.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
                Button(onClick = { reload++ }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("重试")
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(items, key = { it.id }) { term ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCategory(term.id, term.name) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(term.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${term.postCount} 篇公开文章",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
