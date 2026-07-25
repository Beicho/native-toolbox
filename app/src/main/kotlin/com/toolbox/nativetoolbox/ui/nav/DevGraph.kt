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
    composable("tool/unicode_escape") { UnicodeEscapeToolScreen(back) }
    composable("tool/mockdata") { MockDataToolScreen(back) }
    composable("tool/curl_parse") { CurlParseToolScreen(back) }
    composable("tool/http_ref") { HttpRefToolScreen(back) }
    composable("tool/ua_parse") { UaParseToolScreen(back) }
    composable("tool/sql_format") { SqlFormatToolScreen(back) }
    composable("tool/config_convert") { ConfigConvertToolScreen(back) }
    composable("tool/markdown_preview") { MarkdownPreviewToolScreen(back) }
    composable("tool/html_preview") { PlaceholderToolScreen("HTML 预览", back) }
    composable("tool/css_gen") { CssGenToolScreen(back) }
    composable("tool/svg_tool") { PlaceholderToolScreen("SVG 工具", back) }
    composable("tool/chmod") { ChmodToolScreen(back) }
    composable("tool/cmd_ref") { CmdRefToolScreen(back) }
    composable("tool/ascii") { AsciiToolScreen(back) }
    composable("tool/json2code") { Json2CodeToolScreen(back) }
    composable("tool/android_ref") { PlaceholderToolScreen("Android 速查", back) }
    composable("tool/icon_gen") { PlaceholderToolScreen("图标生成", back) }
    composable("tool/resistor") { ResistorToolScreen(back) }
}
