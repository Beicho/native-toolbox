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
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.abs

private fun fmt(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val rounded = Math.round(value * 100.0) / 100.0
    return if (abs(rounded - Math.round(rounded)) < 1e-9) Math.round(rounded).toString()
    else String.format("%.2f", rounded)
}

@Composable
fun PercentToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var mode by rememberSaveable { mutableStateOf(0) }
    var a by rememberSaveable { mutableStateOf("") }
    var b by rememberSaveable { mutableStateOf("") }

    val x = a.trim().toDoubleOrNull()
    val y = b.trim().toDoubleOrNull()

    val labels = when (mode) {
        0 -> "原价" to "折扣（如 8.5 表示八五折）"
        1 -> "原价" to "现价"
        else -> "数值" to "百分比 %"
    }

    ToolScaffold {
        item { SectionHeader("算什么") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("打折后价格", "折扣力度", "百分比取值"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                }
            }
        }
        item { SectionHeader("输入") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(value = a, onValueChange = { a = it }, placeholder = labels.first, mono = true)
                    IosTextField(value = b, onValueChange = { b = it }, placeholder = labels.second, mono = true)
                }
            }
        }
        item { SectionHeader("结果") }
        item {
            GroupedCard {
                when (mode) {
                    0 -> {
                        val price = if (x != null && y != null) x * y / 10.0 else null
                        KeyValueRow("折后价", price?.let { fmt(it) } ?: "")
                        RowDivider()
                        KeyValueRow("省下", if (x != null && price != null) fmt(x - price) else "")
                        RowDivider()
                        KeyValueRow("相当于", if (y != null) "${fmt(y * 10)}%" else "")
                    }
                    1 -> {
                        val ratio = if (x != null && y != null && x != 0.0) y / x else null
                        KeyValueRow("折扣", ratio?.let { "${fmt(it * 10)} 折" } ?: "")
                        RowDivider()
                        KeyValueRow("降幅", ratio?.let { "${fmt((1 - it) * 100)}%" } ?: "")
                        RowDivider()
                        KeyValueRow("省下", if (x != null && y != null) fmt(x - y) else "")
                    }
                    else -> {
                        val part = if (x != null && y != null) x * y / 100.0 else null
                        KeyValueRow("结果", part?.let { fmt(it) } ?: "")
                        RowDivider()
                        KeyValueRow("加上后", if (x != null && part != null) fmt(x + part) else "")
                        RowDivider()
                        KeyValueRow("减掉后", if (x != null && part != null) fmt(x - part) else "")
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "两个输入框都填上才会出结果。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
