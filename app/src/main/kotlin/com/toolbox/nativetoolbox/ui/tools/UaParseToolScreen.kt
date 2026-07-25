package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private data class UaInfo(
    val browser: String,
    val browserVersion: String,
    val engine: String,
    val os: String,
    val osVersion: String,
    val device: String,
    val isMobile: Boolean,
    val isBot: Boolean
)

private fun firstMatch(ua: String, pattern: String): String? =
    Regex(pattern, RegexOption.IGNORE_CASE).find(ua)?.groupValues?.getOrNull(1)

private fun parseUa(ua: String): UaInfo {
    val lower = ua.lowercase()

    val isBot = listOf("bot", "spider", "crawler", "slurp", "bingpreview", "curl/", "wget/", "python-requests")
        .any { lower.contains(it) }

    // 顺序很重要：Edge / Opera / 各家国产壳都会同时带 Chrome 和 Safari 标识
    val browser: Pair<String, String> = when {
        lower.contains("edg/") || lower.contains("edga/") || lower.contains("edgios/") ->
            "Edge" to (firstMatch(ua, "Edg(?:A|iOS)?/([\\d.]+)") ?: "")
        lower.contains("opr/") || lower.contains("opera") ->
            "Opera" to (firstMatch(ua, "(?:OPR|Opera)/([\\d.]+)") ?: "")
        lower.contains("micromessenger") ->
            "微信内置浏览器" to (firstMatch(ua, "MicroMessenger/([\\d.]+)") ?: "")
        lower.contains("quark") -> "夸克" to (firstMatch(ua, "Quark/([\\d.]+)") ?: "")
        lower.contains("ucbrowser") -> "UC 浏览器" to (firstMatch(ua, "UCBrowser/([\\d.]+)") ?: "")
        lower.contains("huaweibrowser") -> "华为浏览器" to (firstMatch(ua, "HuaweiBrowser/([\\d.]+)") ?: "")
        lower.contains("miuibrowser") -> "小米浏览器" to (firstMatch(ua, "MiuiBrowser/([\\d.]+)") ?: "")
        lower.contains("firefox") -> "Firefox" to (firstMatch(ua, "Firefox/([\\d.]+)") ?: "")
        lower.contains("chrome") -> "Chrome" to (firstMatch(ua, "Chrome/([\\d.]+)") ?: "")
        lower.contains("safari") -> "Safari" to (firstMatch(ua, "Version/([\\d.]+)") ?: "")
        lower.contains("curl/") -> "curl" to (firstMatch(ua, "curl/([\\d.]+)") ?: "")
        else -> "未识别" to ""
    }

    val engine = when {
        lower.contains("applewebkit") && lower.contains("chrome") -> "Blink"
        lower.contains("applewebkit") -> "WebKit"
        lower.contains("gecko/") -> "Gecko"
        lower.contains("trident") -> "Trident"
        else -> "未识别"
    }

    val os: Pair<String, String> = when {
        lower.contains("android") -> "Android" to (firstMatch(ua, "Android ([\\d.]+)") ?: "")
        lower.contains("iphone") || lower.contains("ipad") ->
            "iOS" to ((firstMatch(ua, "OS (\\d+[_\\d]*) like") ?: "").replace('_', '.'))
        lower.contains("harmonyos") -> "HarmonyOS" to (firstMatch(ua, "HarmonyOS ([\\d.]+)") ?: "")
        lower.contains("windows nt") -> {
            val nt = firstMatch(ua, "Windows NT ([\\d.]+)") ?: ""
            "Windows" to when (nt) {
                "10.0" -> "10 或 11"
                "6.3" -> "8.1"
                "6.2" -> "8"
                "6.1" -> "7"
                else -> nt
            }
        }
        lower.contains("mac os x") -> "macOS" to ((firstMatch(ua, "Mac OS X (\\d+[_.\\d]*)") ?: "").replace('_', '.'))
        lower.contains("cros") -> "ChromeOS" to ""
        lower.contains("linux") -> "Linux" to ""
        else -> "未识别" to ""
    }

    val device = when {
        lower.contains("ipad") -> "iPad"
        lower.contains("iphone") -> "iPhone"
        lower.contains("android") -> firstMatch(ua, "Android [\\d.]+; ([^;)]+)")?.trim() ?: "Android 设备"
        else -> "桌面设备"
    }

    val isMobile = lower.contains("mobile") || lower.contains("android") ||
        lower.contains("iphone") || lower.contains("ipad")

    return UaInfo(browser.first, browser.second, engine, os.first, os.second, device, isMobile, isBot)
}

private val samples = listOf(
    "Chrome / Windows" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
    "Safari / iPhone" to "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1",
    "微信 / Android" to "Mozilla/5.0 (Linux; Android 14; PJD110 Build/UKQ1.230924.001) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/125.0.6422.165 Mobile Safari/537.36 MicroMessenger/8.0.49.2600"
)

@Composable
fun UaParseToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var input by rememberSaveable { mutableStateOf("") }
    val info = if (input.isBlank()) null else parseUa(input.trim())

    ToolScaffold {
        item { SectionHeader("User-Agent") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "粘贴 UA 字符串",
                        mono = true
                    )
                    samples.forEach { (name, value) ->
                        SolidButton(onClick = { input = value }, filled = false, height = 38.dp) {
                            Text("示例：" + name, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        item { SectionHeader("解析结果") }
        item {
            GroupedCard {
                if (info == null) {
                    CardPadding {
                        Text(
                            "粘贴后自动解析，全程离线完成。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                } else {
                    KeyValueRow(
                        "浏览器",
                        listOf(info.browser, info.browserVersion).filter { it.isNotBlank() }.joinToString(" ")
                    )
                    RowDivider()
                    KeyValueRow("渲染引擎", info.engine)
                    RowDivider()
                    KeyValueRow(
                        "操作系统",
                        listOf(info.os, info.osVersion).filter { it.isNotBlank() }.joinToString(" ")
                    )
                    RowDivider()
                    KeyValueRow("设备", info.device)
                    RowDivider()
                    KeyValueRow("类型", if (info.isMobile) "移动端" else "桌面端", copyable = false)
                    RowDivider()
                    KeyValueRow("疑似爬虫", if (info.isBot) "是" else "否", copyable = false)
                }
            }
        }
    }
}
