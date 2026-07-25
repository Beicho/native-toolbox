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
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/**
 * 竖排：把整段文字按列排布。传统古籍是从右往左读，所以第一列排在最右。
 * 每列高度取最长句子，短句用全角空格补齐，保证列对齐。
 */
private fun toVertical(text: String, rightToLeft: Boolean, fullWidthPad: Boolean): String {
    val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) return ""
    val columns = if (rightToLeft) lines.reversed() else lines
    val height = columns.maxOf { it.length }
    val pad = if (fullWidthPad) '　' else ' '
    return (0 until height).joinToString("\n") { row ->
        columns.joinToString("") { col -> if (row < col.length) col[row].toString() else pad.toString() }
    }
}

@Composable
fun VerticalTextToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var input by rememberSaveable { mutableStateOf("") }
    var direction by rememberSaveable { mutableStateOf(0) }

    val result = if (input.isBlank()) "" else toVertical(input, direction == 0, true)

    ToolScaffold {
        item { SectionHeader("原文") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "一行一句，例如：\n床前明月光\n疑是地上霜"
                    )
                    SegmentedPicker(
                        options = listOf("从右往左（古籍）", "从左往右"),
                        selectedIndex = direction,
                        onSelected = { direction = it }
                    )
                    Text(
                        "每一行会变成竖着的一列。用等宽字体查看效果最好。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader("竖排结果") }
        item {
            GroupedCard {
                CardPadding {
                    if (result.isBlank()) {
                        Text("先输入几句话", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    } else {
                        OutputCard(text = result, label = "竖排文字")
                    }
                }
            }
        }
    }
}
