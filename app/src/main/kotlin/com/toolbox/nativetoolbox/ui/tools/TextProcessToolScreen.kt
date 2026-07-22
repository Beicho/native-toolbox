package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

@Composable
fun TextProcessToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }
    var sortOp by rememberSaveable { mutableStateOf(0) }
    var cleanOp by rememberSaveable { mutableStateOf(0) }
    var caseOp by rememberSaveable { mutableStateOf(0) }

    fun apply() {
        var lines = input.lines()
        lines = when (cleanOp) {
            1 -> lines.filter { it.isNotBlank() }
            2 -> lines.map { it.trim() }
            3 -> lines.map { it.trim() }.filter { it.isNotBlank() }
            else -> lines
        }
        lines = when (sortOp) {
            1 -> lines.distinct()
            2 -> lines.sorted()
            3 -> lines.reversed()
            else -> lines
        }
        var text = lines.joinToString("\n")
        text = when (caseOp) {
            1 -> text.uppercase()
            2 -> text.lowercase()
            else -> text
        }
        output = text
    }

    ToolScaffold(title = "文本处理", onBack = onBack) {
        item { SectionHeader("输入") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "每行一条内容…",
                        minHeight = 150.dp
                    )
                }
            }
        }
        item { SectionHeader("操作(按 清理 → 排序 → 大小写 顺序执行)") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("不清理", "去空行", "Trim", "两者"),
                        selectedIndex = cleanOp,
                        onSelected = { cleanOp = it }
                    )
                    SegmentedPicker(
                        options = listOf("原序", "去重", "排序", "反转"),
                        selectedIndex = sortOp,
                        onSelected = { sortOp = it }
                    )
                    SegmentedPicker(
                        options = listOf("原样", "全大写", "全小写"),
                        selectedIndex = caseOp,
                        onSelected = { caseOp = it }
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        SolidButton(onClick = { apply() }, filled = true) {
                            Text("执行", color = Color.White)
                        }
                    }
                }
            }
        }
        item { SectionHeader("输出") }
        item {
            GroupedCard {
                CardPadding {
                    OutputCard(text = output, label = "处理结果")
                }
            }
        }
    }
}
