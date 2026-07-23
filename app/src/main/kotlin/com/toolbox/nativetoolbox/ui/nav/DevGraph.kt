package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 开发者分类路由(26 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.devToolsGraph(back: () -> Unit) {
    composable("tool/json") { JsonToolScreen(back) }
    composable("tool/timestamp") { TimestampToolScreen(back) }
    composable("tool/radix") { RadixToolScreen(back) }
    composable("tool/base64") { Base64ToolScreen(back) }
    composable("tool/url") { UrlToolScreen(back) }
    composable("tool/uuid") { UuidToolScreen(back) }
    composable("tool/regex") { RegexToolScreen(back) }
    composable("tool/color") { ColorToolScreen(back) }
    composable("tool/unicode_escape") { PlaceholderToolScreen("字符转义", back) }
    composable("tool/mockdata") { PlaceholderToolScreen("Mock 数据", back) }
    composable("tool/curl_parse") { PlaceholderToolScreen("cURL 解析", back) }
    composable("tool/http_ref") { PlaceholderToolScreen("HTTP 速查", back) }
    composable("tool/ua_parse") { PlaceholderToolScreen("UA 解析", back) }
    composable("tool/sql_format") { PlaceholderToolScreen("SQL 格式化", back) }
    composable("tool/config_convert") { PlaceholderToolScreen("配置互转", back) }
    composable("tool/markdown_preview") { PlaceholderToolScreen("Markdown 预览", back) }
    composable("tool/html_preview") { PlaceholderToolScreen("HTML 预览", back) }
    composable("tool/css_gen") { PlaceholderToolScreen("CSS 样式生成", back) }
    composable("tool/svg_tool") { PlaceholderToolScreen("SVG 工具", back) }
    composable("tool/chmod") { PlaceholderToolScreen("chmod 计算", back) }
    composable("tool/cmd_ref") { PlaceholderToolScreen("命令速查", back) }
    composable("tool/ascii") { PlaceholderToolScreen("ASCII 码表", back) }
    composable("tool/json2code") { PlaceholderToolScreen("JSON 转代码", back) }
    composable("tool/android_ref") { PlaceholderToolScreen("Android 速查", back) }
    composable("tool/icon_gen") { PlaceholderToolScreen("图标生成", back) }
    composable("tool/resistor") { PlaceholderToolScreen("电阻色环", back) }
}
