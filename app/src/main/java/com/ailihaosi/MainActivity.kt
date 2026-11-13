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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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
import com.ailihaosi.ui.home.ActivitiesScreen
import com.ailihaosi.ui.home.CategoryScreen
import com.ailihaosi.ui.home.HomeScreen
import com.ailihaosi.ui.home.InteractiveScreen
import com.ailihaosi.ui.home.RankingsScreen
import com.ailihaosi.ui.home.SearchScreen
import com.ailihaosi.ui.home.SectionsScreen
import com.ailihaosi.ui.library.FavoritesScreen
import com.ailihaosi.ui.nav.AppDrawerContent
import com.ailihaosi.ui.nav.Routes
import com.ailihaosi.ui.theme.AlhsTheme
import com.ailihaosi.update.openDownload
import com.ailihaosi.update.ReleaseInfo
import com.ailihaosi.update.UpdateChecker
import com.ailihaosi.update.UpdateDialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = PostRepository(appContext = applicationContext)
        setContent {
            AlhsTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                fun openPost(id: Long) = navController.navigate(Routes.Detail.create(id))
                fun openPage(id: Long) = navController.navigate(Routes.Page.create(id))
                fun openCategory(id: Long, title: String) =
                    navController.navigate(Routes.Category.create(id, title))

                fun openNamedCategory(name: String) {
                    Toast.makeText(context, "正在打开$name…", Toast.LENGTH_SHORT).show()
                    lifecycleScope.launch {
                        repo.resolveCategory(name)
                            .onSuccess { openCategory(it.id, it.name.ifBlank { name }) }
                            .onFailure {
                                Toast.makeText(context, it.message ?: "未找到栏目", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                fun openNamedPage(title: String) {
                    Toast.makeText(context, "正在打开$title…", Toast.LENGTH_SHORT).show()
                    lifecycleScope.launch {
                        repo.resolvePage(title)
                            .onSuccess { openPage(it.id) }
                            .onFailure {
                                Toast.makeText(context, it.message ?: "未找到页面", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                var pendingUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }
                var autoChecked by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (!autoChecked) {
                        autoChecked = true
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
                            onSearch = { navController.navigate(Routes.Search.route) },
                            onRankings = { navController.navigate(Routes.Rankings.route) },
                            onActivities = { navController.navigate(Routes.Activities.route) },
                            onOriginals = { openNamedCategory(SiteUrls.SECTION_ORIGINALS) },
                            onFeatured = { openNamedPage(SiteUrls.SECTION_FEATURED) },
                            onHistory = { openNamedPage(SiteUrls.SECTION_HISTORY) },
                            onSections = { navController.navigate(Routes.Sections.route) },
                            onInteractive = { navController.navigate(Routes.Interactive.route) },
                            onNotices = { openNamedCategory(SiteUrls.SECTION_NOTICE) },
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
                            onFavorites = { navController.navigate(Routes.Favorites.route) },
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
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenSearch = { navController.navigate(Routes.Search.route) }
                            )
                        }
                        composable(
                            route = Routes.Detail.route,
                            arguments = listOf(navArgument("contentId") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("contentId") ?: return@composable
                            if (id <= 0L) return@composable
                            DetailScreen(
                                contentId = id,
                                isPage = false,
                                onBack = { navController.popBackStack() },
                                onOpenAuthor = { authorId, name ->
                                    navController.navigate(Routes.Author.create(authorId, name))
                                },
                                onOpenPost = { openPost(it) }
                            )
                        }
                        composable(
                            route = Routes.Page.route,
                            arguments = listOf(navArgument("contentId") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("contentId") ?: return@composable
                            if (id <= 0L) return@composable
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
                                navArgument("authorId") { type = NavType.LongType },
                                navArgument("authorName") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val id = entry.arguments?.getLong("authorId") ?: return@composable
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
                                navArgument("categoryId") { type = NavType.LongType },
                                navArgument("title") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val cid = entry.arguments?.getLong("categoryId") ?: return@composable
                            val title = entry.arguments?.getString("title").orEmpty()
                            CategoryScreen(
                                categoryId = cid,
                                title = title,
                                onBack = { navController.popBackStack() },
                                onOpenPost = { openPost(it.id) }
                            )
                        }
                        composable(Routes.Search.route) {
                            SearchScreen(
                                onBack = { navController.popBackStack() },
                                onOpenPost = { openPost(it.id) },
                                onOpenAuthor = { id, name ->
                                    navController.navigate(Routes.Author.create(id, name))
                                }
                            )
                        }
                        composable(Routes.Rankings.route) {
                            RankingsScreen(
                                onBack = { navController.popBackStack() },
                                onOpenPost = { openPost(it.id) },
                                onOpenAuthor = { id, name ->
                                    navController.navigate(Routes.Author.create(id, name))
                                }
                            )
                        }
                        composable(Routes.Activities.route) {
                            ActivitiesScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.Sections.route) {
                            SectionsScreen(
                                onBack = { navController.popBackStack() },
                                onOpenCategory = { id, name -> openCategory(id, name) }
                            )
                        }
                    }
                }
            }
        }
    }
}
