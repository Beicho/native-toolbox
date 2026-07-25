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
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.abs

private fun money(value: Double): String = String.format("%.2f", value)

/**
 * 多人 AA：每人先填自己垫付的金额，算出人均后给出「谁该给谁多少」的最少转账方案。
 * 贪心配对：最大债主对最大债权人，每次结清一方。
 */
private data class Transfer(val from: String, val to: String, val amount: Double)

private fun settle(entries: List<Pair<String, Double>>): List<Transfer> {
    if (entries.isEmpty()) return emptyList()
    val average = entries.sumOf { it.second } / entries.size
    val debtors = entries.map { it.first to (it.second - average) }
        .filter { it.second < -0.005 }
        .sortedBy { it.second }
        .map { it.first to -it.second }
        .toMutableList()
    val creditors = entries.map { it.first to (it.second - average) }
        .filter { it.second > 0.005 }
        .sortedByDescending { it.second }
        .toMutableList()

    val result = ArrayList<Transfer>()
    var guard = 0
    while (debtors.isNotEmpty() && creditors.isNotEmpty() && guard < 200) {
        guard++
        val (debtorName, debtorOwes) = debtors.first()
        val (creditorName, creditorGets) = creditors.first()
        val amount = minOf(debtorOwes, creditorGets)
        result.add(Transfer(debtorName, creditorName, amount))
        val debtLeft = debtorOwes - amount
        val creditLeft = creditorGets - amount
        debtors.removeAt(0)
        creditors.removeAt(0)
        if (debtLeft > 0.005) debtors.add(0, debtorName to debtLeft)
        if (creditLeft > 0.005) creditors.add(0, creditorName to creditLeft)
        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }
    }
    return result
}

@Composable
fun CostSplitToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var raw by rememberSaveable { mutableStateOf("") }

    val entries = raw.lines().mapNotNull { line ->
        val parts = line.split(Regex("[\\s,，:：]+")).filter { it.isNotBlank() }
        if (parts.size < 2) return@mapNotNull null
        val amount = parts.last().toDoubleOrNull() ?: return@mapNotNull null
        val name = parts.dropLast(1).joinToString(" ")
        if (name.isBlank()) null else name to amount
    }

    val total = entries.sumOf { it.second }
    val average = if (entries.isNotEmpty()) total / entries.size else 0.0
    val transfers = settle(entries)
    val plan = if (transfers.isEmpty()) "" else transfers.joinToString("\n") {
        "${it.from} → ${it.to}  ${money(it.amount)}"
    }

    ToolScaffold {
        item { SectionHeader("谁付了多少") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = raw,
                        onValueChange = { raw = it },
                        placeholder = "一行一个人，例如：\n小明 320\n小红 0\n阿强 150",
                        singleLine = false,
                        minLines = 5,
                        mono = true
                    )
                    Text(
                        "没垫钱的人也要写，金额填 0。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("汇总") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("人数", entries.size.toString(), Modifier.weight(1f))
                        StatCell("总花费", if (entries.isEmpty()) "—" else money(total), Modifier.weight(1f))
                        StatCell("人均", if (entries.isEmpty()) "—" else money(average), Modifier.weight(1f))
                    }
                }
            }
        }
        if (entries.isNotEmpty()) {
            item { SectionHeader("每人盈亏") }
            item {
                GroupedCard {
                    entries.forEachIndexed { index, (name, paid) ->
                        val delta = paid - average
                        val text = when {
                            abs(delta) < 0.005 -> "刚好"
                            delta > 0 -> "应收 ${money(delta)}"
                            else -> "应付 ${money(-delta)}"
                        }
                        KeyValueRow(name, text, copyable = false)
                        if (index != entries.lastIndex) RowDivider()
                    }
                }
            }
        }
        item { SectionHeader("转账方案") }
        item {
            GroupedCard {
                CardPadding {
                    if (plan.isBlank()) {
                        Text(
                            if (entries.isEmpty()) "先按格式填人和金额" else "大家出的钱一样多，不用转账",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    } else {
                        OutputCard(text = plan, label = "最少 ${transfers.size} 笔转账")
                    }
                }
            }
        }
    }
}
