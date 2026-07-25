package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToolScaffold

/** 用 Unicode 数学字母数字符号做的字体变体：每种风格给出大小写与数字的起始码位 */
private class FancyStyle(
    val name: String,
    val upper: Int?,
    val lower: Int?,
    val digit: Int? = null,
    val fallbackMap: Map<Char, Char> = emptyMap()
)

private val fancyStyles = listOf(
    FancyStyle("粗体", 0x1D400, 0x1D41A, 0x1D7CE),
    FancyStyle("斜体", 0x1D434, 0x1D44E),
    FancyStyle("粗斜体", 0x1D468, 0x1D482),
    FancyStyle("手写体", 0x1D49C, 0x1D4B6),
    FancyStyle("粗手写", 0x1D4D0, 0x1D4EA),
    FancyStyle("哥特体", 0x1D504, 0x1D51E),
    FancyStyle("双线体", 0x1D538, 0x1D552, 0x1D7D8),
    FancyStyle("等宽体", 0x1D670, 0x1D68A, 0x1D7F6),
    FancyStyle("圆圈字", 0x24B6, 0x24D0),
    FancyStyle("方框字", 0x1F130, null)
)

private fun applyStyle(text: String, style: FancyStyle): String = buildString {
    text.forEach { c ->
        val mapped = when {
            c in 'A'..'Z' && style.upper != null -> style.upper + (c - 'A')
            c in 'a'..'z' && style.lower != null -> style.lower + (c - 'a')
            c in 'a'..'z' && style.lower == null && style.upper != null -> style.upper + (c - 'a')
            c in '0'..'9' && style.digit != null -> style.digit + (c - '0')
            else -> null
        }
        if (mapped != null) appendCodePoint(mapped) else append(c)
    }
}

private fun StringBuilder.appendCodePoint(cp: Int): StringBuilder {
    if (cp <= 0xFFFF) append(cp.toChar()) else {
        val v = cp - 0x10000
        append((0xD800 + (v shr 10)).toChar())
        append((0xDC00 + (v and 0x3FF)).toChar())
    }
    return this
}

@Composable
fun FancyTextToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }

    ToolScaffold {
        item { SectionHeader("输入") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "输入英文或数字，中文不受支持"
                    )
                }
            }
        }
        item { SectionHeader("点任意一行复制") }
        item {
            GroupedCard {
                Column {
                    fancyStyles.forEachIndexed { index, style ->
                        KeyValueRow(style.name, if (input.isBlank()) "" else applyStyle(input, style))
                        if (index != fancyStyles.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
