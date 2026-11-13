package com.ailihaosi.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ailihaosi.BuildConfig
import com.ailihaosi.data.SiteUrls
import com.ailihaosi.ui.browser.openInCustomTab

@Composable
fun AppDrawerContent(
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onRankings: () -> Unit,
    onActivities: () -> Unit,
    onOriginals: () -> Unit,
    onFeatured: () -> Unit,
    onHistory: () -> Unit,
    onSections: () -> Unit,
    onInteractive: () -> Unit,
    onNotices: () -> Unit,
    onRandom: () -> Unit,
    onFavorites: () -> Unit,
    onCheckUpdate: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    @Composable
    fun DrawerItem(label: String, icon: ImageVector, onClick: () -> Unit) {
        NavigationDrawerItem(
            label = { Text(label) },
            selected = false,
            onClick = { onClose(); onClick() },
            icon = { Icon(icon, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "艾利浩斯学院图书馆",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
            )
            Text(
                text = "主站 alhs.xyz",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            DrawerItem("首页", Icons.Default.Home, onHome)
            DrawerItem("搜索", Icons.Default.Search, onSearch)
            DrawerItem("排行", Icons.Default.Star, onRankings)
            DrawerItem("活动与公告", Icons.Default.Info, onActivities)
            DrawerItem("原创小说", Icons.Default.Star, onOriginals)
            DrawerItem("图书馆精选", Icons.Default.Star, onFeatured)
            DrawerItem("校史馆", Icons.Default.Info, onHistory)
            DrawerItem("栏目", Icons.Default.Place, onSections)
            DrawerItem("互动小说", Icons.Default.Star, onInteractive)
            DrawerItem("公告", Icons.Default.Info, onNotices)
            DrawerItem("随机文章", Icons.Default.Refresh, onRandom)
            DrawerItem("收藏 / 离线缓存", Icons.Default.Favorite, onFavorites)

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            DrawerItem("打开主站", Icons.Default.Place) {
                openInCustomTab(context, SiteUrls.HOME)
            }
            DrawerItem("地址与发布页", Icons.Default.Place) {
                openInCustomTab(context, SiteUrls.LIVE)
            }
            DrawerItem("登录", Icons.Default.Person) {
                openInCustomTab(context, SiteUrls.LOGIN)
            }
            DrawerItem("注册", Icons.Default.Person) {
                openInCustomTab(context, SiteUrls.REGISTER)
            }
            DrawerItem("个人中心", Icons.Default.Info) {
                openInCustomTab(context, SiteUrls.DASHBOARD)
            }
            DrawerItem("检查更新 (当前 ${BuildConfig.VERSION_NAME})", Icons.Default.Refresh, onCheckUpdate)
        }
    }
}
