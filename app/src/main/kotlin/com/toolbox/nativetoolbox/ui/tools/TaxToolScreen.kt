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
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/** 综合所得年度税率表（速算扣除数），2019 起沿用 */
private val brackets = listOf(
    Triple(36000.0, 0.03, 0.0),
    Triple(144000.0, 0.10, 2520.0),
    Triple(300000.0, 0.20, 16920.0),
    Triple(420000.0, 0.25, 31920.0),
    Triple(660000.0, 0.30, 52920.0),
    Triple(960000.0, 0.35, 85920.0),
    Triple(Double.MAX_VALUE, 0.45, 181920.0)
)

private fun yearlyTax(taxable: Double): Pair<Double, Double> {
    if (taxable <= 0) return 0.0 to 0.0
    val hit = brackets.first { taxable <= it.first }
    return (taxable * hit.second - hit.third).coerceAtLeast(0.0) to hit.second
}

private fun money(value: Double): String = String.format("%,.2f", value)

@Composable
fun TaxToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var monthly by rememberSaveable { mutableStateOf("") }
    var social by rememberSaveable { mutableStateOf("") }
    var special by rememberSaveable { mutableStateOf("") }

    val salary = monthly.trim().replace(",", "").toDoubleOrNull() ?: 0.0
    val socialFee = social.trim().replace(",", "").toDoubleOrNull() ?: 0.0
    val specialFee = special.trim().replace(",", "").toDoubleOrNull() ?: 0.0

    val yearIncome = salary * 12
    val taxable = (yearIncome - 60000 - socialFee * 12 - specialFee * 12).coerceAtLeast(0.0)
    val (tax, rate) = yearlyTax(taxable)
    val monthlyTax = tax / 12
    val takeHome = salary - socialFee - specialFee - monthlyTax

    ToolScaffold {
        item { SectionHeader("收入") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(value = monthly, onValueChange = { monthly = it }, placeholder = "月薪（税前）", mono = true)
                    IosTextField(value = social, onValueChange = { social = it }, placeholder = "每月五险一金个人部分", mono = true)
                    IosTextField(value = special, onValueChange = { special = it }, placeholder = "每月专项附加扣除（房租/房贷/子女等）", mono = true)
                    Text(
                        "按全年综合所得计算，起征点每月 5000 元。这是估算，实际以单位申报为准。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("每月") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("到手", if (salary > 0) money(takeHome) else "—", Modifier.weight(1f))
                        StatCell("个税", if (salary > 0) money(monthlyTax) else "—", Modifier.weight(1f))
                    }
                }
            }
        }
        item { SectionHeader("全年") }
        item {
            GroupedCard {
                KeyValueRow("税前总收入", if (salary > 0) money(yearIncome) else "")
                RowDivider()
                KeyValueRow("应纳税所得额", if (salary > 0) money(taxable) else "")
                RowDivider()
                KeyValueRow("适用税率", if (salary > 0) "${(rate * 100).toInt()}%" else "")
                RowDivider()
                KeyValueRow("全年个税", if (salary > 0) money(tax) else "")
                RowDivider()
                KeyValueRow("全年到手", if (salary > 0) money(takeHome * 12) else "")
            }
        }
    }
}
