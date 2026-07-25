package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private class BandColor(val name: String, val color: Color, val digit: Int?, val multiplier: Double?, val tolerance: Double?)

private val bands = listOf(
    BandColor("黑", Color(0xFF1C1C1E), 0, 1.0, null),
    BandColor("棕", Color(0xFF8B5A2B), 1, 10.0, 1.0),
    BandColor("红", Color(0xFFD70015), 2, 100.0, 2.0),
    BandColor("橙", Color(0xFFFF9500), 3, 1_000.0, null),
    BandColor("黄", Color(0xFFFFCC00), 4, 10_000.0, null),
    BandColor("绿", Color(0xFF34C759), 5, 100_000.0, 0.5),
    BandColor("蓝", Color(0xFF007AFF), 6, 1_000_000.0, 0.25),
    BandColor("紫", Color(0xFFAF52DE), 7, 10_000_000.0, 0.1),
    BandColor("灰", Color(0xFF8E8E93), 8, null, 0.05),
    BandColor("白", Color(0xFFF2F2F7), 9, null, null),
    BandColor("金", Color(0xFFD4AF37), null, 0.1, 5.0),
    BandColor("银", Color(0xFFC0C0C0), null, 0.01, 10.0)
)

private fun formatOhm(value: Double): String = when {
    value >= 1_000_000 -> "${trimZero(value / 1_000_000)} MΩ"
    value >= 1_000 -> "${trimZero(value / 1_000)} kΩ"
    else -> "${trimZero(value)} Ω"
}

private fun trimZero(v: Double): String {
    val r = Math.round(v * 100.0) / 100.0
    return if (r == Math.floor(r)) Math.round(r).toString() else r.toString()
}

@Composable
private fun BandPicker(label: String, selected: Int, allowed: List<Int>, onSelect: (Int) -> Unit) {
    val palette = LocalIosPalette.current
    Text(label, style = MaterialTheme.typography.labelLarge, color = palette.secondaryLabel)
    allowed.chunked(6).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { index ->
                val band = bands[index]
                SolidButton(
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                    filled = index == selected,
                    height = 40.dp
                ) {
                    Box(
                        Modifier
                            .height(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(band.color)
                            .padding(horizontal = 6.dp)
                    )
                    Text("　${band.name}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ResistorToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var bandCount by rememberSaveable { mutableStateOf(0) } // 0=四环 1=五环
    var b1 by rememberSaveable { mutableStateOf(1) }
    var b2 by rememberSaveable { mutableStateOf(0) }
    var b3 by rememberSaveable { mutableStateOf(0) }
    var mult by rememberSaveable { mutableStateOf(2) }
    var tol by rememberSaveable { mutableStateOf(10) }

    val digitIndices = bands.indices.filter { bands[it].digit != null }
    val multIndices = bands.indices.filter { bands[it].multiplier != null }
    val tolIndices = bands.indices.filter { bands[it].tolerance != null }

    val digits = if (bandCount == 0) {
        "${bands[b1].digit}${bands[b2].digit}"
    } else {
        "${bands[b1].digit}${bands[b2].digit}${bands[b3].digit}"
    }
    val base = digits.toDouble()
    val ohm = base * (bands[mult].multiplier ?: 1.0)
    val tolerance = bands[tol].tolerance
    val low = tolerance?.let { ohm * (1 - it / 100) }
    val high = tolerance?.let { ohm * (1 + it / 100) }

    ToolScaffold {
        item { SectionHeader("阻值") }
        item {
            GroupedCard {
                KeyValueRow("标准读数", formatOhm(ohm))
                RowDivider()
                KeyValueRow("误差", tolerance?.let { "±$it%" } ?: "未标注")
                RowDivider()
                KeyValueRow("实际范围", if (low != null && high != null) "${formatOhm(low)} ~ ${formatOhm(high)}" else "—")
            }
        }
        item { SectionHeader("色环数") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("四环", "五环"),
                        selectedIndex = bandCount,
                        onSelected = { bandCount = it }
                    )
                    Text(
                        "四环 = 两位有效数字，五环 = 三位（精密电阻）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("依次选择色环") }
        item {
            GroupedCard {
                CardPadding {
                    BandPicker("第一环（十位）", b1, digitIndices) { b1 = it }
                    BandPicker("第二环（个位）", b2, digitIndices) { b2 = it }
                    if (bandCount == 1) BandPicker("第三环（小数位）", b3, digitIndices) { b3 = it }
                    BandPicker("倍率环", mult, multIndices) { mult = it }
                    BandPicker("误差环", tol, tolIndices) { tol = it }
                }
            }
        }
    }
}
