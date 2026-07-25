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

/** 中国成人 BMI 分级（国家卫健委标准，与 WHO 略有差异） */
private fun bmiLevel(bmi: Double): String = when {
    bmi < 18.5 -> "偏瘦"
    bmi < 24.0 -> "正常"
    bmi < 28.0 -> "偏胖"
    else -> "肥胖"
}

private val activityLabels = listOf("久坐", "轻度", "中度", "高强度")
private val activityFactors = listOf(1.2, 1.375, 1.55, 1.725)

private fun num(value: Double, digits: Int = 1): String =
    if (value.isNaN() || value.isInfinite() || value <= 0) "—" else String.format("%.${digits}f", value)

@Composable
fun HealthCalcToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var male by rememberSaveable { mutableStateOf(0) }
    var activity by rememberSaveable { mutableStateOf(1) }

    val cm = height.trim().toDoubleOrNull() ?: 0.0
    val kg = weight.trim().toDoubleOrNull() ?: 0.0
    val years = age.trim().toDoubleOrNull() ?: 0.0
    val meters = cm / 100.0

    val bmi = if (meters > 0 && kg > 0) kg / (meters * meters) else Double.NaN
    // Mifflin-St Jeor：目前最常用的基础代谢公式
    val bmr = if (cm > 0 && kg > 0 && years > 0) {
        10 * kg + 6.25 * cm - 5 * years + if (male == 0) 5 else -161
    } else Double.NaN
    val tdee = if (!bmr.isNaN()) bmr * activityFactors[activity] else Double.NaN
    val idealMin = if (meters > 0) 18.5 * meters * meters else Double.NaN
    val idealMax = if (meters > 0) 23.9 * meters * meters else Double.NaN
    val waterMl = if (kg > 0) kg * 35 else Double.NaN

    ToolScaffold {
        item { SectionHeader("身体数据") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(value = height, onValueChange = { height = it }, modifier = Modifier.weight(1f), placeholder = "身高 cm", mono = true)
                        IosTextField(value = weight, onValueChange = { weight = it }, modifier = Modifier.weight(1f), placeholder = "体重 kg", mono = true)
                    }
                    IosTextField(value = age, onValueChange = { age = it }, placeholder = "年龄", mono = true)
                    SegmentedPicker(options = listOf("男", "女"), selectedIndex = male, onSelected = { male = it })
                    SegmentedPicker(options = activityLabels, selectedIndex = activity, onSelected = { activity = it })
                    Text(
                        "久坐=几乎不运动，轻度=每周 1-3 次，中度=3-5 次，高强度=6 次以上。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("BMI") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("BMI", num(bmi), Modifier.weight(1f))
                        StatCell("评价", if (bmi.isNaN()) "—" else bmiLevel(bmi), Modifier.weight(1f))
                    }
                }
            }
        }
        item { SectionHeader("参考数据") }
        item {
            GroupedCard {
                KeyValueRow("理想体重区间", if (idealMin.isNaN()) "" else "${num(idealMin)} ~ ${num(idealMax)} kg")
                RowDivider()
                KeyValueRow("基础代谢 BMR", if (bmr.isNaN()) "" else "${num(bmr, 0)} 千卡/天")
                RowDivider()
                KeyValueRow("每日消耗 TDEE", if (tdee.isNaN()) "" else "${num(tdee, 0)} 千卡/天")
                RowDivider()
                KeyValueRow("减脂建议摄入", if (tdee.isNaN()) "" else "${num(tdee - 500, 0)} 千卡/天")
                RowDivider()
                KeyValueRow("增肌建议摄入", if (tdee.isNaN()) "" else "${num(tdee + 300, 0)} 千卡/天")
                RowDivider()
                KeyValueRow("每日饮水建议", if (waterMl.isNaN()) "" else "${num(waterMl, 0)} 毫升")
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "以上都是通用公式估算，不能替代体检和医生建议。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
