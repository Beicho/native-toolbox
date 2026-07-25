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

private val presets = listOf(
    "5 公里" to 5.0,
    "10 公里" to 10.0,
    "半马" to 21.0975,
    "全马" to 42.195
)

private fun paceText(secondsPerKm: Double): String {
    if (secondsPerKm.isNaN() || secondsPerKm.isInfinite() || secondsPerKm <= 0) return "—"
    val total = Math.round(secondsPerKm).toInt()
    return "${total / 60}'${(total % 60).toString().padStart(2, '0')}\""
}

private fun durationText(totalSeconds: Double): String {
    if (totalSeconds.isNaN() || totalSeconds.isInfinite() || totalSeconds <= 0) return "—"
    val t = Math.round(totalSeconds).toInt()
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}

@Composable
fun PaceCalcToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var mode by rememberSaveable { mutableStateOf(0) }
    var distance by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var paceMin by rememberSaveable { mutableStateOf("") }
    var paceSec by rememberSaveable { mutableStateOf("") }

    val km = distance.trim().toDoubleOrNull() ?: 0.0
    val totalSeconds = (hours.trim().toDoubleOrNull() ?: 0.0) * 3600 + (minutes.trim().toDoubleOrNull() ?: 0.0) * 60
    val paceSeconds = (paceMin.trim().toDoubleOrNull() ?: 0.0) * 60 + (paceSec.trim().toDoubleOrNull() ?: 0.0)

    // mode 0：已知距离+用时 → 求配速；mode 1：已知距离+配速 → 求用时
    val resolvedPace = if (mode == 0 && km > 0 && totalSeconds > 0) totalSeconds / km else paceSeconds
    val resolvedTime = if (mode == 0) totalSeconds else km * paceSeconds
    val speedKmh = if (resolvedPace > 0) 3600.0 / resolvedPace else Double.NaN

    ToolScaffold {
        item { SectionHeader("算什么") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("由用时算配速", "由配速算用时"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                }
            }
        }
        item { SectionHeader("距离（公里）") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(value = distance, onValueChange = { distance = it }, placeholder = "例如 10", mono = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { (name, value) ->
                            com.toolbox.nativetoolbox.ui.components.SolidButton(
                                onClick = { distance = value.toString() },
                                modifier = Modifier.weight(1f),
                                filled = false,
                                height = 38.dp
                            ) { Text(name, style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }
            }
        }
        if (mode == 0) {
            item { SectionHeader("用时") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IosTextField(value = hours, onValueChange = { hours = it }, modifier = Modifier.weight(1f), placeholder = "小时", mono = true)
                            IosTextField(value = minutes, onValueChange = { minutes = it }, modifier = Modifier.weight(1f), placeholder = "分钟", mono = true)
                        }
                    }
                }
            }
        } else {
            item { SectionHeader("目标配速（每公里）") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IosTextField(value = paceMin, onValueChange = { paceMin = it }, modifier = Modifier.weight(1f), placeholder = "分", mono = true)
                            IosTextField(value = paceSec, onValueChange = { paceSec = it }, modifier = Modifier.weight(1f), placeholder = "秒", mono = true)
                        }
                    }
                }
            }
        }
        item { SectionHeader("结果") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("配速", paceText(resolvedPace), Modifier.weight(1f))
                        StatCell("总用时", durationText(resolvedTime), Modifier.weight(1f))
                        StatCell("时速", if (speedKmh.isNaN()) "—" else String.format("%.1f", speedKmh), Modifier.weight(1f))
                    }
                }
            }
        }
        item { SectionHeader("分段用时") }
        item {
            GroupedCard {
                val marks = listOf(1.0, 5.0, 10.0, 21.0975, 42.195)
                marks.forEachIndexed { index, mark ->
                    val label = when (mark) {
                        21.0975 -> "半马"
                        42.195 -> "全马"
                        else -> "${mark.toInt()} 公里"
                    }
                    KeyValueRow(label, if (resolvedPace > 0) durationText(mark * resolvedPace) else "")
                    if (index != marks.lastIndex) RowDivider()
                }
            }
        }
        if (resolvedPace <= 0) {
            item {
                GroupedCard {
                    CardPadding {
                        Text(
                            "填上距离和另一个条件就会自动算。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
    }
}
