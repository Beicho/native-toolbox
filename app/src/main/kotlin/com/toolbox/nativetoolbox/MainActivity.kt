package com.toolbox.nativetoolbox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.toolbox.nativetoolbox.data.prefs.SettingsStore
import com.toolbox.nativetoolbox.data.prefs.UsageStore
import com.toolbox.nativetoolbox.ui.encoding.EncodingScreen
import com.toolbox.nativetoolbox.ui.home.HomeScreen
import com.toolbox.nativetoolbox.ui.home.toolCategories
import com.toolbox.nativetoolbox.ui.liquid.LiquidBottomTabs
import com.toolbox.nativetoolbox.ui.liquid.LiquidTab
import com.toolbox.nativetoolbox.ui.liquid.LiquidTopBar
import com.toolbox.nativetoolbox.ui.liquid.LocalRootBackdrop
import com.toolbox.nativetoolbox.ui.settings.SettingsScreen
import com.toolbox.nativetoolbox.ui.share.ShareScreen
import com.toolbox.nativetoolbox.ui.theme.AstroKitTheme
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.theme.ThemeMode
import com.toolbox.nativetoolbox.ui.tools.Base64ToolScreen
import com.toolbox.nativetoolbox.ui.tools.BannerToolScreen
import com.toolbox.nativetoolbox.ui.tools.ColorToolScreen
import com.toolbox.nativetoolbox.ui.tools.CronToolScreen
import com.toolbox.nativetoolbox.ui.tools.DateCalcToolScreen
import com.toolbox.nativetoolbox.ui.tools.DeciderToolScreen
import com.toolbox.nativetoolbox.ui.tools.DeviceInfoToolScreen
import com.toolbox.nativetoolbox.ui.tools.DiffToolScreen
import com.toolbox.nativetoolbox.ui.tools.ExifToolScreen
import com.toolbox.nativetoolbox.ui.tools.FileHashToolScreen
import com.toolbox.nativetoolbox.ui.tools.GridCutToolScreen
import com.toolbox.nativetoolbox.ui.tools.HashToolScreen
import com.toolbox.nativetoolbox.ui.tools.ImageCompressToolScreen
import com.toolbox.nativetoolbox.ui.tools.ImageConvertToolScreen
import com.toolbox.nativetoolbox.ui.tools.JsonToolScreen
import com.toolbox.nativetoolbox.ui.tools.JwtToolScreen
import com.toolbox.nativetoolbox.ui.tools.LevelToolScreen
import com.toolbox.nativetoolbox.ui.tools.PickColorToolScreen
import com.toolbox.nativetoolbox.ui.tools.QrToolScreen
import com.toolbox.nativetoolbox.ui.tools.RadixToolScreen
import com.toolbox.nativetoolbox.ui.tools.RandomToolScreen
import com.toolbox.nativetoolbox.ui.tools.RegexToolScreen
import com.toolbox.nativetoolbox.ui.tools.ScreenTestToolScreen
import com.toolbox.nativetoolbox.ui.tools.StitchToolScreen
import com.toolbox.nativetoolbox.ui.tools.TextProcessToolScreen
import com.toolbox.nativetoolbox.ui.tools.TextStatsToolScreen
import com.toolbox.nativetoolbox.ui.tools.TimestampToolScreen
import com.toolbox.nativetoolbox.ui.tools.UnitToolScreen
import com.toolbox.nativetoolbox.ui.tools.UrlToolScreen
import com.toolbox.nativetoolbox.ui.tools.UuidToolScreen
import com.toolbox.nativetoolbox.ui.tools.WatermarkToolScreen
import com.toolbox.nativetoolbox.ui.tools.WifiQrToolScreen
import com.toolbox.nativetoolbox.util.ShareBus
import com.toolbox.nativetoolbox.util.UpdateChecker
import com.toolbox.nativetoolbox.util.UpdateInfo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val context = LocalContext.current
            val settings = remember { SettingsStore(context) }
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.System)
            AstroKitTheme(themeMode = themeMode) {
                AppRoot(
                    settings = settings,
                    pendingRoute = pendingRoute,
                    onPendingConsumed = { pendingRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val route = intent.getStringExtra("route")
        if (route != null) {
            pendingRoute = route
            return
        }
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                ShareBus.post(text)
                pendingRoute = "share"
            }
        }
    }
}

private val topLevelRoutes = listOf("home", "settings")

@Composable
private fun AppRoot(
    settings: SettingsStore,
    pendingRoute: String?,
    onPendingConsumed: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backdrop = rememberLayerBackdrop()
    val usageStore = remember { UsageStore(context) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabIndex = topLevelRoutes.indexOf(currentRoute)
    val toolTitles = remember { toolCategories().flatMap { it.tools }.associate { it.route to it.title } }

    fun openTool(route: String) {
        navController.navigate(route)
        scope.launch { usageStore.recordUse(route) }
    }

    // 快捷方式 / 分享进入
    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        navController.navigate(route) { launchSingleTop = true }
        onPendingConsumed()
    }

    // 启动静默检查更新(24h 一次)
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        if (System.currentTimeMillis() - usageStore.lastCheckAt() > 24 * 3600_000L) {
            UpdateChecker.check().getOrNull()?.let { info ->
                usageStore.saveCheckResult(info?.tag ?: "v${BuildConfig.VERSION_NAME}")
                if (info != null) update = info
            }
        }
    }

    update?.let { info ->
        AlertDialog(
            onDismissRequest = { update = null },
            containerColor = palette.cardBackground,
            title = { Text("发现新版本 ${info.tag}", color = palette.label) },
            text = {
                Text(
                    info.notes.ifBlank { "有新版本可用" },
                    color = palette.secondaryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = info.apkUrl.ifBlank { info.htmlUrl }
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    update = null
                }) { Text("去下载", color = palette.accent) }
            },
            dismissButton = {
                TextButton(onClick = { update = null }) { Text("以后再说", color = palette.secondaryLabel) }
            }
        )
    }

    CompositionLocalProvider(LocalRootBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize()) {
            // 内容层:整体注册为玻璃的 backdrop 源
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(usageStore = usageStore, onOpenTool = { openTool(it) })
                    }
                    composable("settings") { SettingsScreen(settings) }
                    composable("share") {
                        ShareScreen(onOpenTool = { route ->
                            navController.navigate(route) {
                                popUpTo("share") { inclusive = true }
                            }
                        })
                    }

                    val back: () -> Unit = { navController.popBackStack() }
                    composable("tool/json") { JsonToolScreen(back) }
                    composable("tool/timestamp") { TimestampToolScreen(back) }
                    composable("tool/radix") { RadixToolScreen(back) }
                    composable("tool/base64") { Base64ToolScreen(back) }
                    composable("tool/url") { UrlToolScreen(back) }
                    composable("tool/uuid") { UuidToolScreen(back) }
                    composable("tool/regex") { RegexToolScreen(back) }
                    composable("tool/color") { ColorToolScreen(back) }
                    composable("tool/jwt") { JwtToolScreen(back) }
                    composable("tool/diff") { DiffToolScreen(back) }
                    composable("tool/cron") { CronToolScreen(back) }
                    composable("tool/filehash") { FileHashToolScreen(back) }
                    composable("tool/hash") { HashToolScreen(back) }
                    composable("tool/textprocess") { TextProcessToolScreen(back) }
                    composable("tool/textstats") { TextStatsToolScreen(back) }
                    composable("tool/random") { RandomToolScreen(back) }
                    composable("tool/encoding") { EncodingScreen(back) }
                    composable("tool/qrcode") { QrToolScreen(back) }
                    composable("tool/wifiqr") { WifiQrToolScreen(back) }
                    composable("tool/imagecompress") { ImageCompressToolScreen(back) }
                    composable("tool/imageconvert") { ImageConvertToolScreen(back) }
                    composable("tool/pickcolor") { PickColorToolScreen(back) }
                    composable("tool/watermark") { WatermarkToolScreen(back) }
                    composable("tool/gridcut") { GridCutToolScreen(back) }
                    composable("tool/stitch") { StitchToolScreen(back) }
                    composable("tool/exif") { ExifToolScreen(back) }
                    composable("tool/unit") { UnitToolScreen(back) }
                    composable("tool/datecalc") { DateCalcToolScreen(back) }
                    composable("tool/deviceinfo") { DeviceInfoToolScreen(back) }
                    composable("tool/level") { LevelToolScreen(back) }
                    composable("tool/screentest") { ScreenTestToolScreen(back) }
                    composable("tool/banner") { BannerToolScreen(back) }
                    composable("tool/decider") { DeciderToolScreen(back) }
                }
            }

            // 悬浮玻璃顶栏:必须在 layerBackdrop 录制层之外渲染,
            // 否则玻璃会画到包含自己的层 → RenderThread 无限递归闪退
            if (currentRoute != null && currentRoute.startsWith("tool/")) {
                LiquidTopBar(
                    title = toolTitles[currentRoute] ?: "",
                    onBack = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // 悬浮玻璃 Dock:只在顶级页显示
            if (tabIndex >= 0) {
                LiquidBottomTabs(
                    selectedIndex = tabIndex,
                    onSelected = { index ->
                        val route = topLevelRoutes[index]
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    tabs = listOf(
                        LiquidTab(Icons.Rounded.GridView, "工具"),
                        LiquidTab(Icons.Rounded.Settings, "设置")
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 48.dp, vertical = 8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}
