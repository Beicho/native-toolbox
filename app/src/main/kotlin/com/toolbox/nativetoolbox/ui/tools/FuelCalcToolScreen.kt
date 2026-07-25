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

private fun fmt(value: Double, digits: Int = 2): String =
    if (value.isNaN() || value.isInfinite() || value <= 0) "—" else String.format("%.${digits}f", value)

@Composable
fun FuelCalcToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var mode by rememberSaveable { mutableStateOf(0) }
    var distanceText by rememberSaveable { mutableStateOf("") }
    var fuelText by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("7.8") }
    var consumptionText by rememberSaveable { mutableStateOf("") }
    var peopleText by rememberSaveable { mutableStateOf("1") }

    val distance = distanceText.trim().toDoubleOrNull() ?: 0.0
    val fuel = fuelText.trim().toDoubleOrNull() ?: 0.0
    val price = priceText.trim().toDoubleOrNull() ?: 0.0
    val people = peopleText.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1

    // mode 0：加了多少油跑了多远 → 算实际油耗
    // mode 1：已知百公里油耗 → 估算这趟花多少钱
    val consumption = if (mode == 0) {
        if (distance > 0 && fuel > 0) fuel / distance * 100 else Double.NaN
    } else {
        consumptionText.trim().toDoubleOrNull() ?: Double.NaN
    }

    val totalFuel = if (mode == 0) fuel else {
        if (!consumption.isNaN() && distance > 0) consumption * distance / 100 else Double.NaN
    }
    val totalCost = if (!totalFuel.isNaN() && price > 0) totalFuel * price else Double.NaN
    val costPerKm = if (!totalCost.isNaN() && distance > 0) totalCost / distance else Double.NaN
    val costPerPerson = if (!totalCost.isNaN()) totalCost / people else Double.NaN
    val range = if (!consumption.isNaN() && consumption > 0) 50.0 / consumption * 100 else Double.NaN

    ToolScaffold {
        item { SectionHeader("算什么") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("算实际油耗", "估算油费"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    Text(
                        if (mode == 0) "用加油量和行驶里程算出真实百公里油耗。"
                        else "已知车的百公里油耗，估算这趟路要多少钱。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader("参数") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it },
                        placeholder = "行驶里程（公里）",
                        mono = true
                    )
                    if (mode == 0) {
                        IosTextField(
                            value = fuelText,
                            onValueChange = { fuelText = it },
                            placeholder = "加油量（升）",
                            mono = true
                        )
                    } else {
                        IosTextField(
                            value = consumptionText,
                            onValueChange = { consumptionText = it },
                            placeholder = "百公里油耗（升）",
                            mono = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "油价（元/升）",
                            mono = true
                        )
                        IosTextField(
                            value = peopleText,
                            onValueChange = { peopleText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "分摊人数",
                            mono = true
                        )
                    }
                }
            }
        }
        item { SectionHeader("结果") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("百公里油耗", fmt(consumption), Modifier.weight(1f))
                        StatCell("总油费", fmt(totalCost), Modifier.weight(1f))
                    }
                }
            }
        }
        item { SectionHeader("明细") }
        item {
            GroupedCard {
                KeyValueRow("用油量", if (totalFuel.isNaN()) "" else fmt(totalFuel) + " 升", copyable = false)
                RowDivider()
                KeyValueRow("每公里成本", if (costPerKm.isNaN()) "" else fmt(costPerKm) + " 元", copyable = false)
                RowDivider()
                KeyValueRow("每人分摊", if (costPerPerson.isNaN()) "" else fmt(costPerPerson) + " 元", copyable = false)
                RowDivider()
                KeyValueRow("加满 50 升可跑", if (range.isNaN()) "" else fmt(range, 0) + " 公里", copyable = false)
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "实际油耗受路况、载重、空调和驾驶习惯影响，市区通常比高速高 30% 以上。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
