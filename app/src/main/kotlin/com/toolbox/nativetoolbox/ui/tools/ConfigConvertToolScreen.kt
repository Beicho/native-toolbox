package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON / YAML / .env / properties 之间的互转。
 * YAML 只支持嵌套映射、列表和标量这些日常配置结构，不做锚点别名等高级特性。
 */
private fun jsonToYaml(value: Any?, indent: Int = 0): String {
    val pad = "  ".repeat(indent)
    return when (value) {
        is JSONObject -> {
            if (value.length() == 0) return "{}"
            value.keys().asSequence().joinToString("\n") { key ->
                val child = value.get(key)
                if (child is JSONObject || child is JSONArray) {
                    pad + key + ":\n" + jsonToYaml(child, indent + 1)
                } else {
                    pad + key + ": " + scalarToYaml(child)
                }
            }
        }
        is JSONArray -> {
            if (value.length() == 0) return "[]"
            (0 until value.length()).joinToString("\n") { i ->
                val child = value.get(i)
                if (child is JSONObject || child is JSONArray) {
                    pad + "-\n" + jsonToYaml(child, indent + 1)
                } else {
                    pad + "- " + scalarToYaml(child)
                }
            }
        }
        else -> pad + scalarToYaml(value)
    }
}

private fun scalarToYaml(value: Any?): String = when {
    value == null || value == JSONObject.NULL -> "null"
    value is String && (value.isEmpty() || value.contains(": ") || value.contains('#') ||
        value.trim() != value) -> "\"" + value.replace("\"", "\\\"") + "\""
    else -> value.toString()
}

private fun flatten(prefix: String, value: Any?, out: MutableList<Pair<String, String>>) {
    when (value) {
        is JSONObject -> value.keys().forEach { key ->
            flatten(if (prefix.isEmpty()) key else prefix + "." + key, value.get(key), out)
        }
        is JSONArray -> (0 until value.length()).forEach { i ->
            flatten(prefix + "[" + i + "]", value.get(i), out)
        }
        else -> out.add(prefix to (if (value == JSONObject.NULL) "" else value.toString()))
    }
}

/** 极简 YAML 解析：按缩进还原成 JSON，够覆盖常见配置文件 */
private fun yamlToJson(text: String): Any {
    val lines = text.lines()
        .map { it.substringBefore(" #").trimEnd() }
        .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }

    fun parseBlock(startIndex: Int, indent: Int): Pair<Any, Int> {
        var i = startIndex
        var result: Any? = null
        while (i < lines.size) {
            val line = lines[i]
            val currentIndent = line.takeWhile { it == ' ' }.length
            if (currentIndent < indent) break
            val content = line.trim()
            if (content.startsWith("- ") || content == "-") {
                val array = (result as? JSONArray) ?: JSONArray().also { result = it }
                val inline = content.removePrefix("-").trim()
                if (inline.isEmpty()) {
                    val (child, next) = parseBlock(i + 1, currentIndent + 1)
                    array.put(child)
                    i = next
                } else {
                    array.put(parseScalar(inline))
                    i++
                }
            } else {
                val obj = (result as? JSONObject) ?: JSONObject().also { result = it }
                val colon = content.indexOf(':')
                if (colon < 0) {
                    i++
                    continue
                }
                val key = content.take(colon).trim()
                val rest = content.substring(colon + 1).trim()
                if (rest.isEmpty()) {
                    val (child, next) = parseBlock(i + 1, currentIndent + 1)
                    obj.put(key, child)
                    i = next
                } else {
                    obj.put(key, parseScalar(rest))
                    i++
                }
            }
        }
        return (result ?: JSONObject()) to i
    }
    return parseBlock(0, 0).first
}

private fun parseScalar(text: String): Any {
    val t = text.trim()
    if (t.length >= 2 && ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'")))) {
        return t.substring(1, t.length - 1)
    }
    return when {
        t == "null" || t == "~" -> JSONObject.NULL
        t == "true" -> true
        t == "false" -> false
        t.toLongOrNull() != null -> t.toLong()
        t.toDoubleOrNull() != null -> t.toDouble()
        else -> t
    }
}

private fun toEnv(pairs: List<Pair<String, String>>): String = pairs.joinToString("\n") { (k, v) ->
    val key = k.replace(Regex("[.\\[\\]]+"), "_").trim('_').uppercase()
    val needQuote = v.contains(' ') || v.contains('#') || v.isEmpty()
    key + "=" + if (needQuote) "\"" + v + "\"" else v
}

private fun toProperties(pairs: List<Pair<String, String>>): String =
    pairs.joinToString("\n") { (k, v) -> k + "=" + v }

@Composable
fun ConfigConvertToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var input by rememberSaveable { mutableStateOf("") }
    var sourceIndex by rememberSaveable { mutableStateOf(0) }
    var targetIndex by rememberSaveable { mutableStateOf(1) }

    val parsed: Any? = if (input.isBlank()) null else runCatching {
        when (sourceIndex) {
            0 -> {
                val t = input.trim()
                if (t.startsWith("[")) JSONArray(t) else JSONObject(t)
            }
            else -> yamlToJson(input)
        }
    }.getOrNull()

    val flat = if (parsed == null) emptyList() else ArrayList<Pair<String, String>>().also { flatten("", parsed, it) }

    val output = when {
        parsed == null -> ""
        targetIndex == 0 -> when (parsed) {
            is JSONObject -> parsed.toString(2)
            is JSONArray -> parsed.toString(2)
            else -> parsed.toString()
        }
        targetIndex == 1 -> jsonToYaml(parsed)
        targetIndex == 2 -> toEnv(flat)
        else -> toProperties(flat)
    }

    ToolScaffold {
        item { SectionHeader("格式") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("从 JSON", "从 YAML"),
                        selectedIndex = sourceIndex,
                        onSelected = { sourceIndex = it }
                    )
                    SegmentedPicker(
                        options = listOf("到 JSON", "到 YAML", "到 .env", "到 properties"),
                        selectedIndex = targetIndex,
                        onSelected = { targetIndex = it }
                    )
                }
            }
        }
        item { SectionHeader("输入") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = if (sourceIndex == 0) "粘贴 JSON" else "粘贴 YAML",
                        minHeight = 140.dp,
                        mono = true
                    )
                    if (input.isNotBlank() && parsed == null) {
                        Text(
                            if (sourceIndex == 0) "JSON 解析不了，检查括号和引号"
                            else "YAML 解析不了，注意缩进要用空格不能用制表符",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    }
                }
            }
        }
        if (parsed != null) {
            item { SectionHeader("结构") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("叶子字段", flat.size.toString(), Modifier.weight(1f))
                            StatCell(
                                "顶层类型",
                                if (parsed is JSONArray) "数组" else "对象",
                                Modifier.weight(1f)
                            )
                            StatCell(
                                "最深层级",
                                (flat.maxOfOrNull { it.first.count { c -> c == '.' } + 1 } ?: 0).toString(),
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            item { SectionHeader("结果") }
            item { GroupedCard { CardPadding { OutputCard(text = output) } } }
            if (targetIndex >= 2) {
                item { SectionHeader("字段对照") }
                item {
                    GroupedCard {
                        flat.take(40).forEachIndexed { index, (k, v) ->
                            KeyValueRow(k, v)
                            if (index != flat.take(40).lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}
