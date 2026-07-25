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
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private class Item(val label: String, val price: Double, val quantity: Double) {
    val unitPrice: Double get() = if (quantity > 0) price / quantity else Double.NaN
}

private val unitNames = listOf("克 / 毫升", "千克 / 升", "个 / 片")
private val unitSuffix = listOf("元/100克", "元/千克", "元/个")

/** 每行格式：名称 价格 数量。名称可以带空格，最后两个数字才是价格和数量 */
private fun parseItems(raw: String): List<Item> = raw.lines().mapNotNull { line ->
    val parts = line.split(Regex("[\\s,，]+")).filter { it.isNotBlank() }
    if (parts.size < 2) return@mapNotNull null
    val quantity = parts.last().toDoubleOrNull() ?: return@mapNotNull null
    val price = parts.getOrNull(parts.size - 2)?.toDoubleOrNull() ?: return@mapNotNull null
    val label = parts.dropLast(2).joinToString(" ").ifBlank { "未命名" }
    if (price <= 0 || quantity <= 0) null else Item(label, price, quantity)
}

private fun fmt(value: Double, digits: Int = 3): String =
    if (value.isNaN() || value.isInfinite()) "—" else String.format("%.${digits}f", value)

@Composable
fun PriceCompareToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var raw by rememberSaveable { mutableStateOf("") }
    var unitIndex by rememberSaveable { mutableStateOf(0) }

    val items = parseItems(raw)
    val sorted = items.sortedBy { it.unitPrice }
    val cheapest = sorted.firstOrNull()
    val mostExpensive = sorted.lastOrNull()

    // 展示口径：克类换算成每 100 克更直观
    val scale = if (unitIndex == 0) 100.0 else 1.0

    ToolScaffold {
        item { SectionHeader("商品清单") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = raw,
                        onValueChange = { raw = it },
                        placeholder = "一行一个商品：名称 价格 数量\n例如：\nA品牌 39.9 500\nB品牌 25.8 300\n散装 12 100",
                        mono = true
                    )
                    SegmentedPicker(
                        options = unitNames,
                        selectedIndex = unitIndex,
                        onSelected = { unitIndex = it }
                    )
                    Text(
                        if (items.isEmpty()) "按格式填两行以上就能比价"
                        else "识别到 " + items.size + " 个商品",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        if (cheapest != null) {
            item { SectionHeader("最划算") }
            item {
                GroupedCard {
                    CardPadding {
                        Text(
                            cheapest.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.green
                        )
                        Text(
                            fmt(cheapest.unitPrice * scale) + " " + unitSuffix[unitIndex],
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryLabel
                        )
                        if (mostExpensive != null && mostExpensive !== cheapest && mostExpensive.unitPrice > 0) {
                            val save = (1 - cheapest.unitPrice / mostExpensive.unitPrice) * 100
                            Text(
                                "比最贵的「" + mostExpensive.label + "」便宜 " + String.format("%.1f%%", save),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.green
                            )
                        }
                    }
                }
            }
            item { SectionHeader("从便宜到贵") }
            item {
                GroupedCard {
                    sorted.forEachIndexed { index, item ->
                        val ratio = if (cheapest.unitPrice > 0) item.unitPrice / cheapest.unitPrice else 1.0
                        KeyValueRow(
                            (index + 1).toString() + ". " + item.label,
                            fmt(item.unitPrice * scale) + "　" +
                                if (index == 0) "最低" else String.format("贵 %.0f%%", (ratio - 1) * 100),
                            copyable = false
                        )
                        if (index != sorted.lastIndex) RowDivider()
                    }
                }
            }
            item { SectionHeader("原始数据") }
            item {
                GroupedCard {
                    sorted.forEachIndexed { index, item ->
                        KeyValueRow(
                            item.label,
                            String.format("%.2f 元 / %.0f", item.price, item.quantity),
                            copyable = false
                        )
                        if (index != sorted.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
