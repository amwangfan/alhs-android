package com.ailihaosi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ailihaosi.data.SiteUrls
import com.ailihaosi.data.repo.PostRepository
import com.ailihaosi.ui.author.AuthorScreen
import com.ailihaosi.ui.detail.DetailScreen
import com.ailihaosi.ui.home.CategoryScreen
import com.ailihaosi.ui.home.HomeScreen
import com.ailihaosi.ui.home.InteractiveScreen
import com.ailihaosi.ui.library.FavoritesScreen
import com.ailihaosi.ui.nav.AppDrawerContent
import com.ailihaosi.ui.nav.Routes
import com.ailihaosi.ui.theme.AlhsTheme
import com.ailihaosi.update.ReleaseInfo
import com.ailihaosi.update.UpdateChecker
import com.ailihaosi.update.UpdateDialog
import com.ailihaosi.update.openDownload
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repo by lazy { PostRepository(appContext = applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlhsTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                fun openPost(id: Int) = navController.navigate(Routes.Detail.create(id))
                fun openPage(id: Int) = navController.navigate(Routes.Page.create(id))

                // ---- Update check (GitHub Releases only) ----
                var pendingUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }
                var autoChecked by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (!autoChecked) {
                        autoChecked = true
                        // Throttled (24h) + silent on failure.
                        val r = UpdateChecker.autoCheck(applicationContext)
                        if (r is UpdateChecker.Result.Newer) pendingUpdate = r.release
                    }
                }
                pendingUpdate?.let { rel ->
                    UpdateDialog(
                        release = rel,
                        onDownload = {
                            pendingUpdate = null
                            openDownload(context, rel)
                        },
                        onLater = { pendingUpdate = null },
                        onIgnore = {
                            UpdateChecker.ignoreTag(applicationContext, rel.tagName)
                            pendingUpdate = null
                        }
                    )
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawerContent(
                            onHome = {
                                navController.navigate(Routes.Home.route) {
                                    popUpTo(Routes.Home.route) { inclusive = true }
                                }
                            },
                            onRandom = {
                                Toast.makeText(context, "正在抽取随机文章…", Toast.LENGTH_SHORT).show()
                                lifecycleScope.launch {
                                    repo.fetchRandomPost()
                                        .onSuccess { openPost(it.id) }
                                        .onFailure {
                                            Toast.makeText(
                                                context,
                                                it.message ?: "随机失败",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            },
                            // 校史馆 → preview section page id 1690 (same IA as web)
                            onHistory = { openPage(SiteUrls.HISTORY_PAGE_ID) },
                            // 图书馆精选 → page 3865 (web sectionPages mapping)
                            onFeatured = { openPage(SiteUrls.FEATURED_PAGE_ID) },
                            onFavorites = { navController.navigate(Routes.Favorites.route) },
                            // 公告 → category 3 posts (announcements API often empty on preview)
                            onNotices = {
                                navController.navigate(
                                    Routes.Category.create(SiteUrls.NOTICE_CATEGORY_ID, "公告")
                                )
                            },
                            onInteractive = { navController.navigate(Routes.Interactive.route) },
                            onCheckUpdate = {
                                Toast.makeText(context, "正在检查更新…", Toast.LENGTH_SHORT).show()
                                lifecycleScope.launch {
                                    when (val r = UpdateChecker.manualCheck()) {
                                        is UpdateChecker.Result.Newer -> pendingUpdate = r.release
                                        is UpdateChecker.Result.UpToDate -> Toast.makeText(
                                            context,
                                            "已是最新版本 (${r.current})",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        is UpdateChecker.Result.Failed -> Toast.makeText(
                                            context,
                                            "检查更新失败：${r.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        UpdateChecker.Result.Skipped -> Unit
                                    }
                                }
                            },
                            onClose = { scope.launch { drawerState.close() } }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.Home.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Routes.Home.route) {
                            HomeScreen(
                                onOpenPost = { openPost(it.id) },
                                onOpenAuthor = { id, name ->
                                    navController.navigate(Routes.Author.create(id, name))
                                },
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                        composable(
                            route = Routes.Detail.route,
                            arguments = listOf(navArgument("contentId") { type = NavType.IntType })
                        ) { entry ->
                            val id = entry.arguments?.getInt("contentId") ?: return@composable
                            DetailScreen(
                                contentId = id,
                                isPage = false,
                                onBack = { navController.popBackStack() },
                                onOpenAuthor = { authorId, name ->
                                    navController.navigate(Routes.Author.create(authorId, name))
                                }
                            )
                        }
                        composable(
                            route = Routes.Page.route,
                            arguments = listOf(navArgument("contentId") { type = NavType.IntType })
                        ) { entry ->
                            val id = entry.arguments?.getInt("contentId") ?: return@composable
                            DetailScreen(
                                contentId = id,
                                isPage = true,
                                onBack = { navController.popBackStack() },
                                onOpenAuthor = { _, _ -> }
                            )
                        }
                        composable(
                            route = Routes.Author.route,
                            arguments = listOf(
                                navArgument("authorId") { type = NavType.IntType },
                                navArgument("authorName") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val id = entry.arguments?.getInt("authorId") ?: return@composable
                            val name = entry.arguments?.getString("authorName").orEmpty()
                            AuthorScreen(
                                authorId = id,
                                authorName = name,
                                onBack = { navController.popBackStack() },
                                onOpenPost = { openPost(it.id) }
                            )
                        }
                        composable(Routes.Interactive.route) {
                            InteractiveScreen(
                                onBack = { navController.popBackStack() },
                                onOpenPost = { openPost(it.id) }
                            )
                        }
                        composable(Routes.Favorites.route) {
                            FavoritesScreen(
                                onBack = { navController.popBackStack() },
                                onOpenCached = { cached ->
                                    if (cached.isPage) openPage(cached.id) else openPost(cached.id)
                                }
                            )
                        }
                        composable(
                            route = Routes.Category.route,
                            arguments = listOf(
                                navArgument("categoryId") { type = NavType.IntType },
                                navArgument("title") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val cid = entry.arguments?.getInt("categoryId") ?: return@composable
                            val title = entry.arguments?.getString("title").orEmpty()
                            CategoryScreen(
                                categoryId = cid,
                                title = title,
                                onBack = { navController.popBackStack() },
                                onOpenPost = { openPost(it.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
