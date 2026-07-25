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
import kotlin.math.pow

private fun money(v: Double): String =
    if (v.isNaN() || v.isInfinite()) "—" else String.format("%,.2f", v)

private val compoundLabels = listOf("按年", "按月", "按日")
private val compoundTimes = listOf(1, 12, 365)

@Composable
fun InterestToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var mode by rememberSaveable { mutableStateOf(0) }
    var principalText by rememberSaveable { mutableStateOf("") }
    var rateText by rememberSaveable { mutableStateOf("") }
    var yearsText by rememberSaveable { mutableStateOf("") }
    var compoundIndex by rememberSaveable { mutableStateOf(0) }
    var monthlyText by rememberSaveable { mutableStateOf("") }

    val principal = principalText.trim().replace(",", "").toDoubleOrNull() ?: 0.0
    val annualRate = rateText.trim().toDoubleOrNull() ?: 0.0
    val years = yearsText.trim().toDoubleOrNull() ?: 0.0
    val monthly = monthlyText.trim().replace(",", "").toDoubleOrNull() ?: 0.0
    val n = compoundTimes[compoundIndex]
    val r = annualRate / 100.0

    val simpleInterest = principal * r * years
    val compoundTotal = if (principal > 0 && years > 0 && r >= 0)
        principal * (1 + r / n).pow(n * years) else Double.NaN
    val compoundInterest = if (compoundTotal.isNaN()) Double.NaN else compoundTotal - principal

    // 定投：每月末投入，按月复利
    val monthlyRate = r / 12.0
    val months = years * 12
    val investTotal = if (monthly > 0 && months > 0) {
        if (monthlyRate == 0.0) monthly * months
        else monthly * (((1 + monthlyRate).pow(months) - 1) / monthlyRate)
    } else Double.NaN
    val investPrincipal = monthly * months
    val investGain = if (investTotal.isNaN()) Double.NaN else investTotal - investPrincipal

    // 本金翻倍所需年数（72 法则的精确版）
    val doubleYears = if (r > 0) Math.log(2.0) / Math.log(1 + r / n) / n else Double.NaN

    ToolScaffold {
        item { SectionHeader("算什么") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("存款利息", "每月定投"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                }
            }
        }
        item { SectionHeader("参数") }
        item {
            GroupedCard {
                CardPadding {
                    if (mode == 0) {
                        IosTextField(
                            value = principalText,
                            onValueChange = { principalText = it },
                            placeholder = "本金（元）",
                            mono = true
                        )
                    } else {
                        IosTextField(
                            value = monthlyText,
                            onValueChange = { monthlyText = it },
                            placeholder = "每月投入（元）",
                            mono = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = rateText,
                            onValueChange = { rateText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "年利率 %",
                            mono = true
                        )
                        IosTextField(
                            value = yearsText,
                            onValueChange = { yearsText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "年数",
                            mono = true
                        )
                    }
                    if (mode == 0) {
                        SegmentedPicker(
                            options = compoundLabels,
                            selectedIndex = compoundIndex,
                            onSelected = { compoundIndex = it }
                        )
                        Text(
                            "复利频率越高，最终收益越多，但差别通常不大。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
        if (mode == 0) {
            item { SectionHeader("到期金额") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("复利本息", money(compoundTotal), Modifier.weight(1f))
                            StatCell("复利收益", money(compoundInterest), Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SectionHeader("对比与参考") }
            item {
                GroupedCard {
                    KeyValueRow("单利收益", if (principal > 0 && years > 0) money(simpleInterest) else "", copyable = false)
                    RowDivider()
                    KeyValueRow(
                        "复利比单利多",
                        if (compoundInterest.isNaN()) "" else money(compoundInterest - simpleInterest),
                        copyable = false
                    )
                    RowDivider()
                    KeyValueRow("单利本息", if (principal > 0) money(principal + simpleInterest) else "", copyable = false)
                    RowDivider()
                    KeyValueRow(
                        "本金翻倍需要",
                        if (doubleYears.isNaN()) "" else String.format("%.1f 年", doubleYears),
                        copyable = false
                    )
                }
            }
        } else {
            item { SectionHeader("定投结果") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("最终总额", money(investTotal), Modifier.weight(1f))
                            StatCell("其中收益", money(investGain), Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SectionHeader("明细") }
            item {
                GroupedCard {
                    KeyValueRow("累计投入", if (investPrincipal > 0) money(investPrincipal) else "", copyable = false)
                    RowDivider()
                    KeyValueRow("投入期数", if (months > 0) months.toInt().toString() + " 期" else "", copyable = false)
                    RowDivider()
                    KeyValueRow(
                        "收益占比",
                        if (investTotal.isNaN() || investTotal <= 0) ""
                        else String.format("%.1f%%", investGain / investTotal * 100),
                        copyable = false
                    )
                }
            }
        }
    }
}
