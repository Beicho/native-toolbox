package com.toolbox.nativetoolbox

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.toolbox.nativetoolbox.ui.encoding.EncodingScreen
import com.toolbox.nativetoolbox.ui.home.HomeScreen
import com.toolbox.nativetoolbox.ui.home.toolCategories
import com.toolbox.nativetoolbox.ui.liquid.LiquidBottomTabs
import com.toolbox.nativetoolbox.ui.liquid.LiquidTab
import com.toolbox.nativetoolbox.ui.liquid.LocalRootBackdrop
import com.toolbox.nativetoolbox.ui.settings.SettingsScreen
import com.toolbox.nativetoolbox.ui.theme.AstroKitTheme
import com.toolbox.nativetoolbox.ui.theme.ThemeMode
import com.toolbox.nativetoolbox.ui.tools.Base64ToolScreen
import com.toolbox.nativetoolbox.ui.tools.ColorToolScreen
import com.toolbox.nativetoolbox.ui.tools.HashToolScreen
import com.toolbox.nativetoolbox.ui.tools.ImageCompressToolScreen
import com.toolbox.nativetoolbox.ui.tools.ImageConvertToolScreen
import com.toolbox.nativetoolbox.ui.tools.JsonToolScreen
import com.toolbox.nativetoolbox.ui.tools.QrToolScreen
import com.toolbox.nativetoolbox.ui.tools.RadixToolScreen
import com.toolbox.nativetoolbox.ui.tools.RandomToolScreen
import com.toolbox.nativetoolbox.ui.tools.RegexToolScreen
import com.toolbox.nativetoolbox.ui.tools.TextProcessToolScreen
import com.toolbox.nativetoolbox.ui.tools.TextStatsToolScreen
import com.toolbox.nativetoolbox.ui.tools.TimestampToolScreen
import com.toolbox.nativetoolbox.ui.tools.UrlToolScreen
import com.toolbox.nativetoolbox.ui.tools.UuidToolScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settings = remember { SettingsStore(context) }
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.System)
            AstroKitTheme(themeMode = themeMode) {
                AppRoot(settings)
            }
        }
    }
}

private val topLevelRoutes = listOf("home", "settings")

@Composable
private fun AppRoot(settings: SettingsStore) {
    val navController = rememberNavController()
    val backdrop = rememberLayerBackdrop()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabIndex = topLevelRoutes.indexOf(currentRoute)

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
                        HomeScreen(onOpenTool = { route -> navController.navigate(route) })
                    }
                    composable("settings") { SettingsScreen(settings) }

                    val back: () -> Unit = { navController.popBackStack() }
                    composable("tool/json") { JsonToolScreen(back) }
                    composable("tool/timestamp") { TimestampToolScreen(back) }
                    composable("tool/radix") { RadixToolScreen(back) }
                    composable("tool/base64") { Base64ToolScreen(back) }
                    composable("tool/url") { UrlToolScreen(back) }
                    composable("tool/uuid") { UuidToolScreen(back) }
                    composable("tool/regex") { RegexToolScreen(back) }
                    composable("tool/color") { ColorToolScreen(back) }
                    composable("tool/hash") { HashToolScreen(back) }
                    composable("tool/textprocess") { TextProcessToolScreen(back) }
                    composable("tool/textstats") { TextStatsToolScreen(back) }
                    composable("tool/random") { RandomToolScreen(back) }
                    composable("tool/qrcode") { QrToolScreen(back) }
                    composable("tool/imagecompress") { ImageCompressToolScreen(back) }
                    composable("tool/imageconvert") { ImageConvertToolScreen(back) }
                    composable("tool/encoding") { EncodingScreen(back) }
                }
            }

            // 悬浮玻璃 Dock:只在三个顶级页显示
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
