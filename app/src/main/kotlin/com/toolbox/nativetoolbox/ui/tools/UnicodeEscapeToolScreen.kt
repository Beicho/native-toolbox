package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold

private val htmlEntities = mapOf(
    '&' to "&amp;", '<' to "&lt;", '>' to "&gt;", '"' to "&quot;", '\'' to "&#39;"
)
private val htmlReverse = mapOf(
    "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"",
    "&#39;" to "'", "&apos;" to "'", "&nbsp;" to " "
)

private fun toUnicode(text: String): String = buildString {
    text.forEach { c ->
        if (c.code < 128) append(c) else append("\\u").append(String.format("%04x", c.code))
    }
}

private fun fromUnicode(text: String): String =
    Regex("\\\\[uU]\\+?([0-9a-fA-F]{4,6})").replace(text) { m ->
        val cp = m.groupValues[1].toInt(16)
        String(Character.toChars(cp))
    }

private fun escapeJava(text: String): String = buildString {
    text.forEach { c ->
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}

private fun unescapeJava(text: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\\' && i + 1 < text.length) {
            when (val next = text[i + 1]) {
                'n' -> { sb.append('\n'); i += 2 }
                'r' -> { sb.append('\r'); i += 2 }
                't' -> { sb.append('\t'); i += 2 }
                '"' -> { sb.append('"'); i += 2 }
                '\\' -> { sb.append('\\'); i += 2 }
                'u' -> {
                    val hex = text.substring(i + 2).take(4)
                    if (hex.length == 4 && hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                        sb.append(hex.toInt(16).toChar()); i += 6
                    } else { sb.append(c); i++ }
                }
                else -> { sb.append(next); i += 2 }
            }
        } else { sb.append(c); i++ }
    }
    return sb.toString()
}

private fun escapeHtml(text: String): String = buildString {
    text.forEach { c -> append(htmlEntities[c] ?: c.toString()) }
}

private fun unescapeHtml(text: String): String {
    var out = text
    htmlReverse.forEach { (k, v) -> out = out.replace(k, v) }
    out = Regex("&#(\\d+);").replace(out) { m -> m.groupValues[1].toInt().toChar().toString() }
    out = Regex("&#[xX]([0-9a-fA-F]+);").replace(out) { m -> m.groupValues[1].toInt(16).toChar().toString() }
    return out
}

@Composable
fun UnicodeEscapeToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(0) }
    var direction by rememberSaveable { mutableStateOf(0) }

    val result = when {
        input.isEmpty() -> ""
        kind == 0 -> if (direction == 0) toUnicode(input) else fromUnicode(input)
        kind == 1 -> if (direction == 0) escapeJava(input) else unescapeJava(input)
        else -> if (direction == 0) escapeHtml(input) else unescapeHtml(input)
    }

    ToolScaffold {
        item { SectionHeader("转义类型") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("Unicode", "字符串", "HTML"),
                        selectedIndex = kind,
                        onSelected = { kind = it }
                    )
                    SegmentedPicker(
                        options = listOf("转义", "还原"),
                        selectedIndex = direction,
                        onSelected = { direction = it }
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
                        placeholder = when (kind) {
                            0 -> if (direction == 0) "输入中文，转成 \\uXXXX" else "输入 \\u4f60\\u597d"
                            1 -> if (direction == 0) "输入含引号换行的文本" else "输入带 \\n \\t 的字符串"
                            else -> if (direction == 0) "输入含 < > & 的文本" else "输入含 &lt; &amp; 的文本"
                        },
                        mono = true
                    )
                }
            }
        }
        item { SectionHeader("结果") }
        item { GroupedCard { CardPadding { OutputCard(text = result) } } }
    }
}
