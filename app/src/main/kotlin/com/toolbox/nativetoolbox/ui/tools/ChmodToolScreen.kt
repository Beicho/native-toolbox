package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.toolbox.nativetoolbox.ui.components.CheckRow
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private fun triadText(bits: Int): String =
    (if (bits and 4 != 0) "r" else "-") +
        (if (bits and 2 != 0) "w" else "-") +
        (if (bits and 1 != 0) "x" else "-")

private val commonModes = listOf(
    "644" to "普通文件：自己可改，别人只读",
    "755" to "脚本/目录：自己可改可执行，别人可读可执行",
    "600" to "私密文件：只有自己能读写（如 SSH 私钥）",
    "777" to "所有人可读写执行（有风险，尽量避免）",
    "700" to "私密目录：只有自己能进",
    "664" to "团队共享文件：同组可改"
)

@Composable
fun ChmodToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var owner by rememberSaveable { mutableStateOf(6) }
    var group by rememberSaveable { mutableStateOf(4) }
    var other by rememberSaveable { mutableStateOf(4) }
    var numericInput by rememberSaveable { mutableStateOf("") }

    val typed = numericInput.trim()
    val typedValid = typed.length == 3 && typed.all { it in '0'..'7' }

    val numeric = "$owner$group$other"
    val symbolic = triadText(owner) + triadText(group) + triadText(other)

    @Composable
    fun triad(title: String, value: Int, onChange: (Int) -> Unit) {
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = palette.secondaryLabel)
            CheckRow("读取 r（4）", value and 4 != 0) { onChange(value xor 4) }
            CheckRow("写入 w（2）", value and 2 != 0) { onChange(value xor 2) }
            CheckRow("执行 x（1）", value and 1 != 0) { onChange(value xor 1) }
        }
    }

    ToolScaffold {
        item { SectionHeader("结果") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("数字", numeric, Modifier.weight(1f))
                        StatCell("符号", symbolic, Modifier.weight(1f))
                    }
                    KeyValueRow("命令", "chmod $numeric 文件名")
                }
            }
        }
        item { SectionHeader("直接输入数字") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = numericInput,
                        onValueChange = { numericInput = it },
                        placeholder = "输入三位数字如 755",
                        mono = true
                    )
                    com.toolbox.nativetoolbox.ui.components.SolidButton(
                        onClick = {
                            owner = typed[0] - '0'
                            group = typed[1] - '0'
                            other = typed[2] - '0'
                            numericInput = ""
                        },
                        enabled = typedValid
                    ) { Text(if (typedValid) "套用 $typed" else "输入三位 0-7 的数字") }
                }
            }
        }
        item { SectionHeader("拥有者") }
        item { GroupedCard { CardPadding { triad("owner", owner) { owner = it } } } }
        item { SectionHeader("同组用户") }
        item { GroupedCard { CardPadding { triad("group", group) { group = it } } } }
        item { SectionHeader("其他人") }
        item { GroupedCard { CardPadding { triad("other", other) { other = it } } } }
        item { SectionHeader("常用权限") }
        item {
            GroupedCard {
                commonModes.forEachIndexed { index, (code, desc) ->
                    KeyValueRow(code, desc, copyable = false)
                    if (index != commonModes.lastIndex) RowDivider()
                }
            }
        }
    }
}
