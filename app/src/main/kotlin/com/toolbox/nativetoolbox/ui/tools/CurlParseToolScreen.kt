package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private data class Parsed(
    val method: String,
    val url: String,
    val headers: List<Pair<String, String>>,
    val body: String?
)

/** 按 shell 规则切词：处理单双引号与行尾续行符 */
private fun tokenize(input: String): List<String> {
    val text = input.replace("\\\n", " ").replace("\\\r\n", " ")
    val tokens = ArrayList<String>()
    val sb = StringBuilder()
    var quote: Char? = null
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            quote != null && c == quote -> quote = null
            quote != null -> sb.append(c)
            c == '\'' || c == '"' -> quote = c
            c.isWhitespace() -> {
                if (sb.isNotEmpty()) { tokens.add(sb.toString()); sb.clear() }
            }
            else -> sb.append(c)
        }
        i++
    }
    if (sb.isNotEmpty()) tokens.add(sb.toString())
    return tokens
}

private fun parseCurl(input: String): Parsed? {
    val tokens = tokenize(input).toMutableList()
    if (tokens.isEmpty()) return null
    if (tokens.first() == "curl") tokens.removeAt(0)

    var method: String? = null
    var url: String? = null
    val headers = ArrayList<Pair<String, String>>()
    var body: String? = null

    var i = 0
    while (i < tokens.size) {
        when (val t = tokens[i]) {
            "-X", "--request" -> { method = tokens.getOrNull(i + 1)?.uppercase(); i += 2 }
            "-H", "--header" -> {
                tokens.getOrNull(i + 1)?.let { raw ->
                    val idx = raw.indexOf(':')
                    if (idx > 0) headers.add(raw.take(idx).trim() to raw.substring(idx + 1).trim())
                }
                i += 2
            }
            "-d", "--data", "--data-raw", "--data-binary" -> { body = tokens.getOrNull(i + 1); i += 2 }
            "-u", "--user" -> {
                tokens.getOrNull(i + 1)?.let { headers.add("Authorization" to "Basic <base64 of $it>") }
                i += 2
            }
            "-b", "--cookie" -> {
                tokens.getOrNull(i + 1)?.let { headers.add("Cookie" to it) }
                i += 2
            }
            "-A", "--user-agent" -> {
                tokens.getOrNull(i + 1)?.let { headers.add("User-Agent" to it) }
                i += 2
            }
            "-e", "--referer" -> {
                tokens.getOrNull(i + 1)?.let { headers.add("Referer" to it) }
                i += 2
            }
            "--url" -> { url = tokens.getOrNull(i + 1); i += 2 }
            else -> {
                if (!t.startsWith("-") && url == null) url = t
                i++
            }
        }
    }
    if (url == null) return null
    val resolvedMethod = method ?: if (body != null) "POST" else "GET"
    return Parsed(resolvedMethod, url, headers, body)
}

private fun toKotlin(p: Parsed): String = buildString {
    appendLine("val conn = URL(\"${p.url}\").openConnection() as HttpURLConnection")
    appendLine("conn.requestMethod = \"${p.method}\"")
    p.headers.forEach { (k, v) -> appendLine("conn.setRequestProperty(\"$k\", \"$v\")") }
    if (p.body != null) {
        appendLine("conn.doOutput = true")
        appendLine("conn.outputStream.use { it.write(\"\"\"${p.body}\"\"\".toByteArray()) }")
    }
    appendLine("val code = conn.responseCode")
    append("val text = conn.inputStream.bufferedReader().use { it.readText() }")
}

private fun toFetch(p: Parsed): String = buildString {
    appendLine("await fetch(\"${p.url}\", {")
    appendLine("  method: \"${p.method}\",")
    if (p.headers.isNotEmpty()) {
        appendLine("  headers: {")
        p.headers.forEach { (k, v) -> appendLine("    \"$k\": \"$v\",") }
        appendLine("  },")
    }
    if (p.body != null) appendLine("  body: ${'"'}${'"'}${'"'}${p.body}${'"'}${'"'}${'"'},".replace("\"\"\"", "`"))
    append("})")
}

@Composable
fun CurlParseToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var input by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf(0) }

    val parsed = if (input.isBlank()) null else parseCurl(input)

    ToolScaffold {
        item { SectionHeader("粘贴 cURL 命令") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "从浏览器开发者工具里复制的 curl 命令",
                        mono = true
                    )
                    Text(
                        "支持 -X -H -d -u -b -A -e --url，能处理换行续行符。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        if (parsed == null) {
            item { SectionHeader("解析结果") }
            item {
                GroupedCard {
                    CardPadding {
                        Text(
                            if (input.isBlank()) "等待粘贴" else "没解析出请求地址，检查命令是否完整",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (input.isBlank()) palette.tertiaryLabel else palette.red
                        )
                    }
                }
            }
        } else {
            item { SectionHeader("请求概要") }
            item {
                GroupedCard {
                    KeyValueRow("方法", parsed.method)
                    RowDivider()
                    KeyValueRow("地址", parsed.url)
                    RowDivider()
                    KeyValueRow("请求头数", parsed.headers.size.toString(), copyable = false)
                    RowDivider()
                    KeyValueRow("有正文", if (parsed.body != null) "是" else "否", copyable = false)
                }
            }
            if (parsed.headers.isNotEmpty()) {
                item { SectionHeader("请求头") }
                item {
                    GroupedCard {
                        parsed.headers.forEachIndexed { index, (k, v) ->
                            KeyValueRow(k, v)
                            if (index != parsed.headers.lastIndex) RowDivider()
                        }
                    }
                }
            }
            if (parsed.body != null) {
                item { SectionHeader("请求正文") }
                item { GroupedCard { CardPadding { OutputCard(text = parsed.body, label = "body") } } }
            }
            item { SectionHeader("转成代码") }
            item {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(
                            options = listOf("Kotlin", "JS fetch"),
                            selectedIndex = target,
                            onSelected = { target = it }
                        )
                        OutputCard(
                            text = if (target == 0) toKotlin(parsed) else toFetch(parsed),
                            label = if (target == 0) "Kotlin" else "JavaScript"
                        )
                    }
                }
            }
        }
    }
}
