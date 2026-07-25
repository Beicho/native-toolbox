package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val methods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD")

private class HttpResult(
    val code: Int,
    val message: String,
    val millis: Long,
    val headers: List<Pair<String, String>>,
    val body: String,
    val sizeBytes: Int
)

private fun prettify(text: String): String {
    val trimmed = text.trim()
    return runCatching {
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> text
        }
    }.getOrDefault(text)
}

private suspend fun send(
    urlText: String,
    method: String,
    headerLines: String,
    body: String
): Result<HttpResult> = withContext(Dispatchers.IO) {
    runCatching {
        val started = System.currentTimeMillis()
        val connection = URL(urlText).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true

        headerLines.lines().forEach { line ->
            val idx = line.indexOf(':')
            if (idx > 0) {
                connection.setRequestProperty(line.take(idx).trim(), line.substring(idx + 1).trim())
            }
        }

        if (body.isNotBlank() && method != "GET" && method != "HEAD") {
            connection.doOutput = true
            if (connection.getRequestProperty("Content-Type") == null) {
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }

        val code = connection.responseCode
        val text = (if (code in 200..399) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        val headers = connection.headerFields
            .filterKeys { it != null }
            .map { (k, v) -> k to v.joinToString(", ") }
            .sortedBy { it.first }
        val elapsed = System.currentTimeMillis() - started
        val msg = connection.responseMessage ?: ""
        connection.disconnect()
        HttpResult(code, msg, elapsed, headers, text, text.toByteArray(Charsets.UTF_8).size)
    }
}

@Composable
fun HttpTestToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var url by rememberSaveable { mutableStateOf("") }
    var methodIndex by rememberSaveable { mutableStateOf(0) }
    var headers by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<HttpResult?>(null) }
    var error by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var showRaw by rememberSaveable { mutableStateOf(false) }

    fun run() {
        var target = url.trim()
        if (target.isBlank()) {
            error = "先填请求地址"
            return
        }
        if (!target.startsWith("http://") && !target.startsWith("https://")) target = "https://" + target
        loading = true
        error = ""
        result = null
        scope.launch {
            send(target, methods[methodIndex], headers, body)
                .onSuccess { result = it }
                .onFailure { e ->
                    error = when {
                        e.message?.contains("Unable to resolve host") == true -> "域名解析不了，检查网址或网络"
                        e.message?.contains("timeout", true) == true -> "请求超时"
                        e.message?.contains("CLEARTEXT") == true -> "系统禁止明文 HTTP，请改用 HTTPS"
                        else -> e.message ?: "请求失败"
                    }
                }
            loading = false
        }
    }

    val codeColor = result?.let {
        when {
            it.code in 200..299 -> palette.green
            it.code in 300..399 -> palette.teal
            it.code in 400..499 -> palette.orange
            else -> palette.red
        }
    } ?: palette.secondaryLabel

    ToolScaffold {
        item { SectionHeader("请求") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = methods,
                        selectedIndex = methodIndex,
                        onSelected = { methodIndex = it }
                    )
                    IosTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = "https://api.example.com/path",
                        mono = true
                    )
                    IosTextArea(
                        value = headers,
                        onValueChange = { headers = it },
                        placeholder = "请求头，一行一个：\nAuthorization: Bearer xxx\nAccept: application/json",
                        minHeight = 90.dp,
                        mono = true
                    )
                    if (methods[methodIndex] != "GET" && methods[methodIndex] != "HEAD") {
                        IosTextArea(
                            value = body,
                            onValueChange = { body = it },
                            placeholder = "请求正文，通常是 JSON",
                            minHeight = 110.dp,
                            mono = true
                        )
                    }
                    SolidButton(onClick = { run() }, enabled = !loading) {
                        Text(if (loading) "请求中…" else "发送请求")
                    }
                    Text(
                        "请求直接从这台手机发出，需要联网。不会经过任何中转服务器，你的凭据不会外泄给第三方。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                    if (error.isNotBlank()) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = palette.red)
                    }
                }
            }
        }
        result?.let { res ->
            item { SectionHeader("响应") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("状态", res.code.toString(), Modifier.weight(1f))
                            StatCell("耗时", res.millis.toString() + " ms", Modifier.weight(1f))
                            StatCell(
                                "大小",
                                if (res.sizeBytes < 1024) res.sizeBytes.toString() + " B"
                                else String.format("%.1f KB", res.sizeBytes / 1024.0),
                                Modifier.weight(1f)
                            )
                        }
                        Text(
                            res.code.toString() + " " + res.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = codeColor
                        )
                    }
                }
            }
            item { SectionHeader("响应头（" + res.headers.size + "）") }
            item {
                GroupedCard {
                    res.headers.forEachIndexed { index, (k, v) ->
                        KeyValueRow(k, v)
                        if (index != res.headers.lastIndex) RowDivider()
                    }
                }
            }
            item { SectionHeader("响应正文") }
            item {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(
                            options = listOf("格式化", "原始"),
                            selectedIndex = if (showRaw) 1 else 0,
                            onSelected = { showRaw = it == 1 }
                        )
                        if (res.body.isBlank()) {
                            Text(
                                "响应没有正文",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryLabel
                            )
                        } else {
                            OutputCard(
                                text = if (showRaw) res.body else prettify(res.body),
                                label = "body"
                            )
                        }
                    }
                }
            }
        }
    }
}
