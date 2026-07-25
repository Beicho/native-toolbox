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
import kotlin.math.ceil

private fun fmt(value: Double, digits: Int = 2): String =
    if (value.isNaN() || value.isInfinite() || value <= 0) "—" else String.format("%.${digits}f", value)

/** 常见地砖规格（毫米） */
private val tileSizes = listOf(600, 800, 300)

@Composable
fun DecorationToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var mode by rememberSaveable { mutableStateOf(0) }
    var lengthText by rememberSaveable { mutableStateOf("") }
    var widthText by rememberSaveable { mutableStateOf("") }
    var heightText by rememberSaveable { mutableStateOf("2.8") }
    var tileIndex by rememberSaveable { mutableStateOf(0) }
    var wasteText by rememberSaveable { mutableStateOf("8") }
    var priceText by rememberSaveable { mutableStateOf("") }
    var coatsText by rememberSaveable { mutableStateOf("2") }

    val length = lengthText.trim().toDoubleOrNull() ?: 0.0
    val width = widthText.trim().toDoubleOrNull() ?: 0.0
    val height = heightText.trim().toDoubleOrNull() ?: 0.0
    val waste = (wasteText.trim().toDoubleOrNull() ?: 8.0).coerceIn(0.0, 50.0)
    val price = priceText.trim().toDoubleOrNull() ?: 0.0
    val coats = (coatsText.trim().toIntOrNull() ?: 2).coerceIn(1, 5)

    val floorArea = length * width
    val perimeter = 2 * (length + width)
    // 墙面积扣除门窗按经验值 5 平方米
    val wallArea = (perimeter * height - 5.0).coerceAtLeast(0.0)
    val ceilingArea = floorArea

    val tileMeters = tileSizes[tileIndex] / 1000.0
    val tileArea = tileMeters * tileMeters
    val tileCount = if (floorArea > 0 && tileArea > 0) ceil(floorArea * (1 + waste / 100) / tileArea) else Double.NaN
    val tileCost = if (!tileCount.isNaN() && price > 0) tileCount * price else Double.NaN

    // 乳胶漆按每升涂 10 平方米一遍计
    val paintArea = wallArea + ceilingArea
    val paintLiters = if (paintArea > 0) paintArea * coats / 10.0 else Double.NaN
    val paintCost = if (!paintLiters.isNaN() && price > 0) paintLiters * price else Double.NaN

    ToolScaffold {
        item { SectionHeader("算什么") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("地砖用量", "乳胶漆用量", "面积清单"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                }
            }
        }
        item { SectionHeader("房间尺寸（米）") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = lengthText,
                            onValueChange = { lengthText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "长",
                            mono = true
                        )
                        IosTextField(
                            value = widthText,
                            onValueChange = { widthText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "宽",
                            mono = true
                        )
                        IosTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "层高",
                            mono = true
                        )
                    }
                }
            }
        }
        if (mode == 0) {
            item { SectionHeader("地砖参数") }
            item {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(
                            options = tileSizes.map { it.toString() + "×" + it },
                            selectedIndex = tileIndex,
                            onSelected = { tileIndex = it }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IosTextField(
                                value = wasteText,
                                onValueChange = { wasteText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = "损耗 %",
                                mono = true
                            )
                            IosTextField(
                                value = priceText,
                                onValueChange = { priceText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = "单价（元/片）",
                                mono = true
                            )
                        }
                        Text(
                            "常规损耗 5% 到 10%，斜铺或异形房间要留 15%。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
            item { SectionHeader("结果") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("需要", if (tileCount.isNaN()) "—" else tileCount.toInt().toString() + " 片", Modifier.weight(1f))
                            StatCell("预算", fmt(tileCost), Modifier.weight(1f))
                        }
                    }
                    KeyValueRow("地面面积", fmt(floorArea) + " 平方米", copyable = false)
                    RowDivider()
                    KeyValueRow("单片面积", fmt(tileArea, 3) + " 平方米", copyable = false)
                    RowDivider()
                    KeyValueRow("含损耗面积", fmt(floorArea * (1 + waste / 100)) + " 平方米", copyable = false)
                }
            }
        } else if (mode == 1) {
            item { SectionHeader("涂刷参数") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IosTextField(
                                value = coatsText,
                                onValueChange = { coatsText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = "涂几遍",
                                mono = true
                            )
                            IosTextField(
                                value = priceText,
                                onValueChange = { priceText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = "单价（元/升）",
                                mono = true
                            )
                        }
                        Text(
                            "按每升涂 10 平方米一遍估算，已扣除约 5 平方米门窗。底漆通常 1 遍、面漆 2 遍。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
            item { SectionHeader("结果") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("需要", fmt(paintLiters, 1) + " 升", Modifier.weight(1f))
                            StatCell("预算", fmt(paintCost), Modifier.weight(1f))
                        }
                    }
                    KeyValueRow("墙面面积", fmt(wallArea) + " 平方米", copyable = false)
                    RowDivider()
                    KeyValueRow("顶面面积", fmt(ceilingArea) + " 平方米", copyable = false)
                    RowDivider()
                    KeyValueRow("涂刷总面积", fmt(paintArea * coats) + " 平方米", copyable = false)
                }
            }
        } else {
            item { SectionHeader("面积清单") }
            item {
                GroupedCard {
                    KeyValueRow("地面", fmt(floorArea) + " 平方米", copyable = false)
                    RowDivider()
                    KeyValueRow("顶面", fmt(ceilingArea) + " 平方米", copyable = false)
                    RowDivider()
                    KeyValueRow("墙面（扣门窗）", fmt(wallArea) + " 平方米", copyable = false)
                    RowDivider()
                    KeyValueRow("周长", fmt(perimeter) + " 米", copyable = false)
                    RowDivider()
                    KeyValueRow("踢脚线长度", fmt(perimeter - 0.9) + " 米", copyable = false)
                    RowDivider()
                    KeyValueRow("空间体积", fmt(floorArea * height) + " 立方米", copyable = false)
                }
            }
            item {
                GroupedCard {
                    CardPadding {
                        Text(
                            "踢脚线按周长减去一个 0.9 米门洞。实际下单前请让商家复尺。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
    }
}
