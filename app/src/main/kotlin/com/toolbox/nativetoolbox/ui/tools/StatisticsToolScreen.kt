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
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.sqrt

private fun num(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val rounded = Math.round(value * 10000.0) / 10000.0
    return if (rounded == Math.floor(rounded)) Math.round(rounded).toString() else rounded.toString()
}

private fun percentile(sorted: List<Double>, p: Double): Double {
    if (sorted.isEmpty()) return Double.NaN
    if (sorted.size == 1) return sorted[0]
    val idx = p * (sorted.size - 1)
    val lo = Math.floor(idx).toInt()
    val hi = Math.ceil(idx).toInt()
    if (lo == hi) return sorted[lo]
    return sorted[lo] + (sorted[hi] - sorted[lo]) * (idx - lo)
}

@Composable
fun StatisticsToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var input by rememberSaveable { mutableStateOf("") }

    val values = input.split(Regex("[,，\\s;；\n]+"))
        .mapNotNull { it.trim().toDoubleOrNull() }
    val sorted = values.sorted()
    val n = values.size
    val sum = values.sum()
    val mean = if (n > 0) sum / n else Double.NaN
    val variancePopulation = if (n > 0) values.sumOf { (it - mean) * (it - mean) } / n else Double.NaN
    val varianceSample = if (n > 1) values.sumOf { (it - mean) * (it - mean) } / (n - 1) else Double.NaN
    val median = if (n > 0) percentile(sorted, 0.5) else Double.NaN
    val mode = values.groupingBy { it }.eachCount()
        .filter { it.value > 1 }
        .maxByOrNull { it.value }?.key

    ToolScaffold {
        item { SectionHeader("数据") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "粘贴一组数字，用空格、逗号或换行分隔",
                        mono = true
                    )
                    Text(
                        if (input.isBlank()) "支持空格、逗号、分号、换行混用。"
                        else "识别到 $n 个有效数字。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader("概览") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("个数", n.toString(), Modifier.weight(1f))
                        StatCell("平均值", if (n > 0) num(mean) else "—", Modifier.weight(1f))
                        StatCell("中位数", if (n > 0) num(median) else "—", Modifier.weight(1f))
                    }
                }
            }
        }
        item { SectionHeader("详细") }
        item {
            GroupedCard {
                KeyValueRow("求和", if (n > 0) num(sum) else "")
                RowDivider()
                KeyValueRow("最小值", if (n > 0) num(sorted.first()) else "")
                RowDivider()
                KeyValueRow("最大值", if (n > 0) num(sorted.last()) else "")
                RowDivider()
                KeyValueRow("极差", if (n > 0) num(sorted.last() - sorted.first()) else "")
                RowDivider()
                KeyValueRow("众数", mode?.let { num(it) } ?: if (n > 0) "无重复值" else "")
                RowDivider()
                KeyValueRow("样本标准差", if (n > 1) num(sqrt(varianceSample)) else "")
                RowDivider()
                KeyValueRow("总体标准差", if (n > 0) num(sqrt(variancePopulation)) else "")
                RowDivider()
                KeyValueRow("样本方差", if (n > 1) num(varianceSample) else "")
                RowDivider()
                KeyValueRow("下四分位 Q1", if (n > 0) num(percentile(sorted, 0.25)) else "")
                RowDivider()
                KeyValueRow("上四分位 Q3", if (n > 0) num(percentile(sorted, 0.75)) else "")
            }
        }
    }
}
