package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 开发者进阶分类路由(10 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.devProToolsGraph(back: () -> Unit) {
    composable("tool/jwt") { JwtToolScreen(back) }
    composable("tool/diff") { DiffToolScreen(back) }
    composable("tool/cron") { CronToolScreen(back) }
    composable("tool/hash") { HashToolScreen(back) }
    composable("tool/hex_viewer") { PlaceholderToolScreen("Hex 查看器", back) }
    composable("tool/apk_analyze") { PlaceholderToolScreen("APK 分析", back) }
    composable("tool/log_analyze") { PlaceholderToolScreen("日志分析", back) }
    composable("tool/code_screenshot") { PlaceholderToolScreen("代码截图", back) }
    composable("tool/http_test") { PlaceholderToolScreen("HTTP 请求测试", back) }
    composable("tool/websocket_test") { PlaceholderToolScreen("WebSocket 测试", back) }
}
