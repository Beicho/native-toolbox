package com.toolbox.nativetoolbox.ui.tools

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
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import java.math.BigInteger

private val bases = listOf(2, 8, 10, 16)
private val baseNames = listOf("二进制", "八进制", "十进制", "十六进制")

@Composable
fun RadixToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var fromIndex by rememberSaveable { mutableStateOf(2) }

    val value: BigInteger? = runCatching {
        val text = input.trim().removePrefix("0x").removePrefix("0X").removePrefix("0b").removePrefix("0B")
        if (text.isEmpty()) null else BigInteger(text, bases[fromIndex])
    }.getOrNull()

    ToolScaffold(title = "进制转换", onBack = onBack) {
        item { SectionHeader("输入") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("BIN", "OCT", "DEC", "HEX"),
                        selectedIndex = fromIndex,
                        onSelected = { fromIndex = it }
                    )
                    IosTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "输入${baseNames[fromIndex]}数值(支持大数)",
                        mono = true
                    )
                }
            }
        }
        item { SectionHeader("转换结果(点击复制)") }
        item {
            GroupedCard {
                KeyValueRow("二进制", value?.toString(2) ?: "")
                RowDivider()
                KeyValueRow("八进制", value?.toString(8) ?: "")
                RowDivider()
                KeyValueRow("十进制", value?.toString(10) ?: "")
                RowDivider()
                KeyValueRow("十六进制", value?.toString(16)?.uppercase() ?: "")
            }
        }
    }
}
