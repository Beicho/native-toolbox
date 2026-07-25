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
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private val controlNames = mapOf(
    0 to "NUL 空字符", 7 to "BEL 响铃", 8 to "BS 退格", 9 to "TAB 制表",
    10 to "LF 换行", 13 to "CR 回车", 27 to "ESC 转义", 32 to "SPACE 空格",
    127 to "DEL 删除"
)

private fun describe(code: Int): String = when {
    controlNames.containsKey(code) -> controlNames.getValue(code)
    code < 32 -> "控制字符"
    code in 48..57 -> "数字 ${code - 48}"
    code in 65..90 -> "大写字母"
    code in 97..122 -> "小写字母"
    else -> "可打印符号"
}

@Composable
fun AsciiToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var query by rememberSaveable { mutableStateOf("") }
    var range by rememberSaveable { mutableStateOf(1) }

    val trimmed = query.trim()
    val lookupCode: Int? = when {
        trimmed.isEmpty() -> null
        trimmed.length == 1 && trimmed[0].code < 128 -> trimmed[0].code
        trimmed.startsWith("0x") || trimmed.startsWith("0X") -> trimmed.drop(2).toIntOrNull(16)
        trimmed.all { it.isDigit() } -> trimmed.toIntOrNull()
        else -> null
    }?.takeIf { it in 0..127 }

    val visible = when (range) {
        0 -> 0..31
        1 -> 32..95
        else -> 96..127
    }

    ToolScaffold {
        item { SectionHeader("查一个字符") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "输入字符（如 A）、十进制（65）或十六进制（0x41）",
                        mono = true
                    )
                    if (lookupCode != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("字符", if (lookupCode in 33..126) lookupCode.toChar().toString() else "—", Modifier.weight(1f))
                            StatCell("十进制", lookupCode.toString(), Modifier.weight(1f))
                            StatCell("十六", String.format("%02X", lookupCode), Modifier.weight(1f))
                            StatCell("八进制", lookupCode.toString(8), Modifier.weight(1f))
                        }
                        Text(
                            "${describe(lookupCode)}　二进制 ${lookupCode.toString(2).padStart(8, '0')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    } else {
                        Text(
                            if (trimmed.isEmpty()) "也可以直接翻下面的表" else "只支持 0-127 范围内的 ASCII",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (trimmed.isEmpty()) palette.tertiaryLabel else palette.red
                        )
                    }
                }
            }
        }
        item { SectionHeader("码表") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("控制符 0-31", "可打印 32-95", "小写区 96-127"),
                        selectedIndex = range,
                        onSelected = { range = it }
                    )
                }
            }
        }
        item {
            GroupedCard {
                visible.forEachIndexed { index, code ->
                    val display = if (code in 33..126) code.toChar().toString() else describe(code).substringBefore(' ')
                    KeyValueRow("$code　0x${String.format("%02X", code)}", display)
                    if (index != visible.count() - 1) RowDivider()
                }
            }
        }
    }
}
