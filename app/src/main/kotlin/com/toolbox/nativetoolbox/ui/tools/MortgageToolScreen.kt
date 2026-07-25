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

private fun money(value: Double): String =
    if (value.isNaN() || value.isInfinite()) "—" else String.format("%,.2f", value)

private data class Plan(
    val monthlyFirst: Double,
    val monthlyLast: Double,
    val totalInterest: Double,
    val totalPay: Double,
    val equal: Boolean
)

/** 等额本息 / 等额本金 */
private fun calc(principal: Double, annualRate: Double, years: Int, equalInstallment: Boolean): Plan? {
    if (principal <= 0 || annualRate < 0 || years <= 0) return null
    val months = years * 12
    val r = annualRate / 100.0 / 12.0
    return if (equalInstallment) {
        val monthly = if (r == 0.0) principal / months
        else principal * r * (1 + r).pow(months) / ((1 + r).pow(months) - 1)
        val total = monthly * months
        Plan(monthly, monthly, total - principal, total, true)
    } else {
        val basePrincipal = principal / months
        val first = basePrincipal + principal * r
        val last = basePrincipal + basePrincipal * r
        val totalInterest = if (r == 0.0) 0.0 else (months + 1) * principal * r / 2.0
        Plan(first, last, totalInterest, principal + totalInterest, false)
    }
}

@Composable
fun MortgageToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("3.1") }
    var years by rememberSaveable { mutableStateOf("30") }
    var mode by rememberSaveable { mutableStateOf(0) }

    val principal = amount.trim().replace(",", "").toDoubleOrNull()?.times(10000) ?: 0.0
    val plan = calc(principal, rate.trim().toDoubleOrNull() ?: -1.0, years.trim().toIntOrNull() ?: 0, mode == 0)

    ToolScaffold {
        item { SectionHeader("贷款信息") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(value = amount, onValueChange = { amount = it }, placeholder = "贷款金额（万元）", mono = true)
                    IosTextField(value = rate, onValueChange = { rate = it }, placeholder = "年利率 %", mono = true)
                    IosTextField(value = years, onValueChange = { years = it }, placeholder = "贷款年限", mono = true)
                    SegmentedPicker(
                        options = listOf("等额本息", "等额本金"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    Text(
                        if (mode == 0) "每月还一样多，前期利息占比高，最常见。"
                        else "每月递减，首月压力大，总利息更少。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader("月供") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell(if (plan?.equal == false) "首月" else "每月", plan?.let { money(it.monthlyFirst) } ?: "—", Modifier.weight(1f))
                        StatCell(if (plan?.equal == false) "末月" else "期数", plan?.let {
                            if (it.equal) "${(years.trim().toIntOrNull() ?: 0) * 12} 期" else money(it.monthlyLast)
                        } ?: "—", Modifier.weight(1f))
                    }
                }
            }
        }
        item { SectionHeader("总计") }
        item {
            GroupedCard {
                KeyValueRow("贷款本金", if (principal > 0) money(principal) else "")
                RowDivider()
                KeyValueRow("支付利息", plan?.let { money(it.totalInterest) } ?: "")
                RowDivider()
                KeyValueRow("本息合计", plan?.let { money(it.totalPay) } ?: "")
                RowDivider()
                KeyValueRow("利息占本金", plan?.let { String.format("%.1f%%", it.totalInterest / principal * 100) } ?: "")
            }
        }
        if (plan == null) {
            item {
                GroupedCard {
                    CardPadding {
                        Text(
                            "把金额、利率、年限都填上就会自动算。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
    }
}
