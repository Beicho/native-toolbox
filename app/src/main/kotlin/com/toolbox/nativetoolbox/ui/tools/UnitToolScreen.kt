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
import java.math.BigDecimal
import java.math.MathContext

private data class UnitDef(val name: String, val factor: Double)

// 每类以第一个单位为基准
private val categories = listOf(
    "长度" to listOf(
        UnitDef("米 m", 1.0),
        UnitDef("千米 km", 1000.0),
        UnitDef("厘米 cm", 0.01),
        UnitDef("毫米 mm", 0.001),
        UnitDef("英寸 in", 0.0254),
        UnitDef("英尺 ft", 0.3048),
        UnitDef("英里 mi", 1609.344),
        UnitDef("海里 nmi", 1852.0)
    ),
    "重量" to listOf(
        UnitDef("千克 kg", 1.0),
        UnitDef("克 g", 0.001),
        UnitDef("吨 t", 1000.0),
        UnitDef("斤", 0.5),
        UnitDef("磅 lb", 0.45359237),
        UnitDef("盎司 oz", 0.028349523)
    ),
    "存储" to listOf(
        UnitDef("MB", 1.0),
        UnitDef("KB", 1.0 / 1024),
        UnitDef("GB", 1024.0),
        UnitDef("TB", 1024.0 * 1024),
        UnitDef("B", 1.0 / 1024 / 1024),
        UnitDef("Gbit", 128.0)
    ),
    "面积" to listOf(
        UnitDef("平方米 m²", 1.0),
        UnitDef("平方千米 km²", 1e6),
        UnitDef("公顷 ha", 1e4),
        UnitDef("亩", 2000.0 / 3),
        UnitDef("平方英尺 ft²", 0.09290304)
    )
)

private fun trimNum(v: Double): String {
    if (v == 0.0) return "0"
    val bd = BigDecimal(v, MathContext(10)).stripTrailingZeros()
    return bd.toPlainString()
}

@Composable
fun UnitToolScreen(onBack: () -> Unit) {
    var catIndex by rememberSaveable { mutableStateOf(0) }
    var unitIndex by rememberSaveable { mutableStateOf(0) }
    var input by rememberSaveable { mutableStateOf("1") }

    val (catName, units) = categories[catIndex]
    val safeUnit = unitIndex.coerceIn(0, units.lastIndex)
    val value = input.trim().toDoubleOrNull()
    val base = value?.times(units[safeUnit].factor)

    ToolScaffold {
        item { SectionHeader("类别") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = categories.map { it.first },
                        selectedIndex = catIndex,
                        onSelected = { catIndex = it; unitIndex = 0 }
                    )
                    IosTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "输入数值",
                        mono = true
                    )
                    SegmentedPicker(
                        options = units.take(4).map { it.name.substringBefore(" ") },
                        selectedIndex = safeUnit.coerceAtMost(3),
                        onSelected = { unitIndex = it }
                    )
                }
            }
        }
        item { SectionHeader("$catName 换算(点击复制)") }
        item {
            GroupedCard {
                units.forEachIndexed { index, unit ->
                    if (index > 0) RowDivider()
                    KeyValueRow(
                        unit.name,
                        if (base == null) "" else trimNum(base / unit.factor)
                    )
                }
            }
        }
    }
}
