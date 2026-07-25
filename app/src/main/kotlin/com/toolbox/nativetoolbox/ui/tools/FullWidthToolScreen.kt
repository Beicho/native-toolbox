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

/** 半角 → 全角：ASCII 可见字符整体偏移 0xFEE0，空格特殊处理 */
private fun toFullWidth(text: String): String = buildString {
    text.forEach { c ->
        when {
            c == ' ' -> append('　')
            c.code in 0x21..0x7E -> append((c.code + 0xFEE0).toChar())
            else -> append(c)
        }
    }
}

private fun toHalfWidth(text: String): String = buildString {
    text.forEach { c ->
        when {
            c == '　' -> append(' ')
            c.code in 0xFF01..0xFF5E -> append((c.code - 0xFEE0).toChar())
            else -> append(c)
        }
    }
}

@Composable
fun FullWidthToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(0) }
    val result = if (input.isEmpty()) "" else if (mode == 0) toFullWidth(input) else toHalfWidth(input)

    ToolScaffold {
        item { SectionHeader("输入") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("转全角", "转半角"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "粘贴需要转换的文字"
                    )
                }
            }
        }
        item { SectionHeader("结果") }
        item {
            GroupedCard { CardPadding { OutputCard(text = result) } }
        }
    }
}
