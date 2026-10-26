package com.ailihaosi.ui.detail

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ailihaosi.ui.browser.openInCustomTab
import com.ailihaosi.ui.components.formatWpDate
import com.ailihaosi.ui.components.stripHtml
import com.ailihaosi.ui.components.wrapArticleHtml

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    contentId: Int,
    isPage: Boolean = false,
    onBack: () -> Unit,
    onOpenAuthor: (authorId: Int, authorName: String) -> Unit,
    vm: DetailViewModel = viewModel(
        factory = DetailViewModel.factory(
            LocalContext.current.applicationContext as android.app.Application,
            contentId,
            isPage
        )
    )
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val post = state.post

    fun share() {
        val p = post ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, stripHtml(p.title))
            putExtra(Intent.EXTRA_TEXT, "${stripHtml(p.title)}\n${p.link}")
        }
        context.startActivity(Intent.createChooser(send, "分享"))
    }

    fun copyLink() {
        val link = post?.link ?: return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("link", link))
        Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = post?.let { stripHtml(it.title) } ?: "阅读",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = buildString {
                                append("预览站:alpha-preview")
                                if (state.fromCache) append(" · 离线")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { share() }, enabled = post != null) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                    IconButton(onClick = { vm.load(preferNetwork = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { copyLink() }, enabled = post != null) {
                        Icon(Icons.Default.Star, contentDescription = "复制链接")
                    }
                    IconButton(onClick = { vm.toggleFavorite() }, enabled = post != null) {
                        Icon(
                            if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏"
                        )
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
            state.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null && post == null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                Button(onClick = { vm.load() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("重试")
                }
                TextButton(onClick = { vm.load(preferNetwork = false) }) {
                    Text("尝试读取离线缓存")
                }
            }

            post != null -> {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Text(
                        text = buildString {
                            append(post.authorName)
                            append(" · ")
                            append(formatWpDate(post.date))
                            if (state.isCached) append(" · 已缓存")
                            if (state.isFavorite) append(" · 已收藏")
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                if (!isPage) onOpenAuthor(post.authorId, post.authorName)
                            }
                    )
                    if (post.isInteractive && !isPage) {
                        Text(
                            text = "检测到互动小说，建议用浏览器打开以获得完整体验。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        Button(
                            onClick = { openInCustomTab(context, post.link) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        ) { Text("在浏览器中打开") }
                    }
                    TextButton(
                        onClick = { vm.cacheNow() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(if (state.isCached) "更新离线缓存" else "缓存到本地")
                    }
                    ArticleWebView(
                        html = wrapArticleHtml(
                            post.contentHtml,
                            stripHtml(post.title)
                        ),
                        baseUrl = post.link.ifBlank { "https://alpha-preview.alhs.xyz/" },
                        onLinkClick = { url -> openInCustomTab(context, url) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleWebView(
    html: String,
    baseUrl: String,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(0xFF121212.toInt())
                settings.apply {
                    javaScriptEnabled = false
                    domStorageEnabled = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        onLinkClick(url)
                        return true
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        }
    )
}
