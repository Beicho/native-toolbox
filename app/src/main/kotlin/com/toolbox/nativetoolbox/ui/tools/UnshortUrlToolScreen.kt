package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private class Hop(val url: String, val code: Int)

/** 逐跳跟随重定向，最多 10 跳，把整条链路显示出来 */
private suspend fun traceRedirects(start: String): Result<List<Hop>> = withContext(Dispatchers.IO) {
    runCatching {
        val hops = ArrayList<Hop>()
        var current = start
        repeat(10) {
            val connection = URL(current).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
            )
            val code = connection.responseCode
            hops.add(Hop(current, code))
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            if (code !in 300..399 || location.isNullOrBlank()) return@runCatching hops
            current = if (location.startsWith("http")) location else URL(URL(current), location).toString()
        }
        hops
    }
}

private fun paramsOf(url: String): List<Pair<String, String>> = runCatching {
    val query = URL(url).query ?: return@runCatching emptyList()
    query.split('&').mapNotNull {
        val idx = it.indexOf('=')
        if (idx <= 0) null else it.take(idx) to java.net.URLDecoder.decode(it.substring(idx + 1), "UTF-8")
    }
}.getOrDefault(emptyList())

private val trackingKeys = listOf("utm_", "spm", "from", "share", "fbclid", "gclid", "ref", "scene", "clickid")

@Composable
fun UnshortUrlToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var input by rememberSaveable { mutableStateOf("") }
    var hops by remember { mutableStateOf<List<Hop>>(emptyList()) }
    var error by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }

    fun run() {
        var target = input.trim()
        if (target.isBlank()) {
            error = "先粘贴一个短链"
            return
        }
        if (!target.startsWith("http")) target = "https://" + target
        loading = true
        error = ""
        hops = emptyList()
        scope.launch {
            traceRedirects(target)
                .onSuccess { hops = it }
                .onFailure { e ->
                    error = when {
                        e.message?.contains("Unable to resolve host") == true -> "域名解析不了，检查链接"
                        e.message?.contains("timeout", true) == true -> "请求超时"
                        else -> e.message ?: "还原失败"
                    }
                }
            loading = false
        }
    }

    val finalUrl = hops.lastOrNull()?.url ?: ""
    val params = if (finalUrl.isBlank()) emptyList() else paramsOf(finalUrl)
    val trackers = params.filter { p -> trackingKeys.any { p.first.startsWith(it, ignoreCase = true) } }
    val cleanUrl = if (finalUrl.isBlank()) "" else runCatching {
        val url = URL(finalUrl)
        val keep = params.filterNot { p -> trackingKeys.any { p.first.startsWith(it, ignoreCase = true) } }
        val base = url.protocol + "://" + url.host + (if (url.port > 0) ":" + url.port else "") + url.path
        if (keep.isEmpty()) base
        else base + "?" + keep.joinToString("&") { it.first + "=" + java.net.URLEncoder.encode(it.second, "UTF-8") }
    }.getOrDefault(finalUrl)

    ToolScaffold {
        item { SectionHeader("短链还原") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "粘贴短链，例如 t.cn/xxxx",
                        mono = true
                    )
                    SolidButton(onClick = { run() }, enabled = !loading) {
                        Text(if (loading) "跟踪中…" else "还原真实地址")
                    }
                    Text(
                        "只发 HEAD 请求跟踪跳转，不会真正打开页面。需要联网。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                    if (error.isNotBlank()) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = palette.red)
                    }
                }
            }
        }
        if (hops.isNotEmpty()) {
            item { SectionHeader("最终地址") }
            item { GroupedCard { CardPadding { OutputCard(text = finalUrl, label = "真实地址") } } }
            if (trackers.isNotEmpty()) {
                item { SectionHeader("去掉追踪参数后") }
                item {
                    GroupedCard {
                        CardPadding {
                            OutputCard(text = cleanUrl, label = "干净链接")
                            Text(
                                "移除了 " + trackers.size + " 个用于追踪来源的参数，分享这个更干净。",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.green
                            )
                        }
                    }
                }
            }
            item { SectionHeader("跳转链路（" + hops.size + " 跳）") }
            item {
                GroupedCard {
                    hops.forEachIndexed { index, hop ->
                        KeyValueRow((index + 1).toString() + "　" + hop.code, hop.url)
                        if (index != hops.lastIndex) RowDivider()
                    }
                }
            }
            if (params.isNotEmpty()) {
                item { SectionHeader("地址参数") }
                item {
                    GroupedCard {
                        params.forEachIndexed { index, (k, v) ->
                            val isTracker = trackingKeys.any { k.startsWith(it, ignoreCase = true) }
                            KeyValueRow(k + if (isTracker) "（追踪）" else "", v)
                            if (index != params.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}
