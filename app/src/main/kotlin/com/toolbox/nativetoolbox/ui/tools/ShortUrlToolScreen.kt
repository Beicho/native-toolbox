package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
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
import java.net.URLEncoder

/** is.gd 免费短链(无需注册),simple 格式直接返回短链文本 */
private fun shorten(longUrl: String): Result<String> = runCatching {
    val api = "https://is.gd/create.php?format=simple&url=" + URLEncoder.encode(longUrl, "UTF-8")
    val conn = URL(api).openConnection() as HttpURLConnection
    conn.connectTimeout = 8000
    conn.readTimeout = 15000
    conn.setRequestProperty("User-Agent", "AstroKit")
    val code = conn.responseCode
    val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
        ?.bufferedReader()?.use { it.readText() }?.trim() ?: ""
    conn.disconnect()
    if (code !in 200..299 || !body.startsWith("http")) {
        throw Exception(body.ifBlank { "服务暂时不可用" }.removePrefix("Error: "))
    }
    body
}

@Composable
fun ShortUrlToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    fun run() {
        var url = input.trim()
        if (url.isEmpty()) { status = "先贴一个链接"; return }
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        busy = true; status = ""; result = ""
        scope.launch {
            val r = withContext(Dispatchers.IO) { shorten(url) }
            r.onSuccess {
                result = it
                history = (listOf(it to url) + history).take(8)
            }.onFailure { status = "生成失败:${it.message}" }
            busy = false
        }
    }

    ToolScaffold {
        item { if (result.isNotEmpty()) OutputCard(result, Modifier, label = "短链接") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(input, { input = it }, Modifier.fillMaxWidth(), placeholder = "https://很长很长的链接…", minHeight = 88.dp)
                    Spacer(Modifier.height(10.dp))
                    SolidButton(onClick = { run() }, Modifier.fillMaxWidth(), enabled = !busy) {
                        Text(if (busy) "生成中…" else "缩短")
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("需要联网,由公共短链服务 is.gd 生成,长期有效", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                }
            }
        }
        item { if (history.isNotEmpty()) SectionHeader("本次生成过的") }
        item {
            if (history.isNotEmpty()) {
                GroupedCard {
                    history.forEachIndexed { i, (short, long) ->
                        KeyValueRow(short, long.take(36) + if (long.length > 36) "…" else "")
                        if (i != history.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
