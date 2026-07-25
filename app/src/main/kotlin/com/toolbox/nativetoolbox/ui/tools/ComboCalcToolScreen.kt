package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/** 一个凑单方案 */
private data class ComboPlan(
    val picks: List<Double>,      // 选中的商品价格
    val addSum: Double,           // 凑单总额
    val discount: Double,         // 触发的优惠金额
)

/**
 * 求「已有 base 元,从 candidates 里选若干件,使 base+选中 ≥ threshold 且选中总额最小」。
 * 分为单位做可达性 DP,再回溯选中集合。everyFull=true 时按「每满 T 减 D」找性价比最高的档位。
 */
private fun solve(
    base: Double,
    threshold: Double,
    discount: Double,
    everyFull: Boolean,
    candidates: List<Double>,
): List<ComboPlan> {
    val baseC = (base * 100).toInt()
    val thrC = (threshold * 100).toInt()
    if (thrC <= 0) return emptyList()
    val items = candidates.map { (it * 100).toInt() }.filter { it > 0 }
    val maxItem = items.maxOrNull() ?: 0

    // 目标档位:普通满减只看 1 档;每满减看 1~5 档(再多没有实际意义)
    val tiers = if (everyFull) (1..5).toList() else listOf(1)
    val plans = mutableListOf<ComboPlan>()

    for (k in tiers) {
        val target = thrC * k - baseC
        if (target <= 0) {
            // 已有金额本身就够这一档,不用凑
            plans.add(ComboPlan(emptyList(), 0.0, discount * k))
            continue
        }
        if (items.isEmpty()) continue
        val cap = target + maxItem // 最优解不会超过 target+最大单品
        if (cap > 2_000_000) continue // 2 万元上限,防止极端输入卡死
        // 标准 01 背包可达性(分层,保证每件只用一次),额外记回溯
        val reach = Array(items.size + 1) { BooleanArray(cap + 1) }
        reach[0][0] = true
        for (i in 1..items.size) {
            val p = items[i - 1]
            val prev = reach[i - 1]
            val cur = reach[i]
            for (s in 0..cap) {
                cur[s] = prev[s] || (s >= p && prev[s - p])
            }
        }
        var best = -1
        for (s in target..cap) if (reach[items.size][s]) { best = s; break }
        if (best == -1) continue
        // 回溯:reach[i-1][s] 仍可达说明第 i 件可以不选,否则必然选了它
        val picks = mutableListOf<Double>()
        var s = best
        for (i in items.size downTo 1) {
            if (!reach[i - 1][s]) {
                picks.add(items[i - 1] / 100.0)
                s -= items[i - 1]
            }
        }
        plans.add(ComboPlan(picks.sorted(), best / 100.0, discount * k))
    }
    return plans
}

private fun fmt(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else "%.2f".format(v)

@Composable
fun ComboCalcToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var mode by rememberSaveable { mutableStateOf(0) } // 0 满减 1 每满减
    var threshold by rememberSaveable { mutableStateOf("300") }
    var discount by rememberSaveable { mutableStateOf("50") }
    var base by rememberSaveable { mutableStateOf("") }
    var goods by rememberSaveable { mutableStateOf("") }
    var plans by remember { mutableStateOf<List<ComboPlan>?>(null) }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        val t = threshold.toDoubleOrNull()
        val d = discount.toDoubleOrNull()
        val b = base.toDoubleOrNull() ?: 0.0
        if (t == null || t <= 0) { error = "先填满减门槛,比如 300"; plans = null; return }
        if (d == null || d < 0) { error = "优惠金额没填对"; plans = null; return }
        val cand = goods.split('\n', ',', '，', ' ', '、')
            .mapNotNull { it.trim().toDoubleOrNull() }
            .filter { it > 0 }
        if (cand.size > 60) { error = "备选商品最多 60 件,太多了算不过来"; plans = null; return }
        if (b <= 0 && cand.isEmpty()) { error = "购物车金额和备选商品至少填一样"; plans = null; return }
        plans = solve(b, t, d, mode == 1, cand)
        if (plans!!.isEmpty()) error = "这些商品怎么凑都够不到门槛,要么换商品,要么放弃这单优惠"
    }

    ToolScaffold {
        item {
            val p = plans
            if (p != null && p.isNotEmpty()) {
                // 挑「多花最少」的方案置顶
                val baseV = base.toDoubleOrNull() ?: 0.0
                val bestPlan = p.minByOrNull { it.addSum - it.discount }!!
                GroupedCard {
                    CardPadding {
                        val net = bestPlan.addSum - bestPlan.discount
                        Text(
                            if (bestPlan.picks.isEmpty()) "不用凑,直接下单"
                            else if (net <= 0) "白赚:加购后反而更省"
                            else "推荐凑单方案",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.label
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("再凑", "¥" + fmt(bestPlan.addSum), Modifier.weight(1f))
                            StatCell("立减", "¥" + fmt(bestPlan.discount), Modifier.weight(1f))
                            StatCell("实付", "¥" + fmt(baseV + bestPlan.addSum - bestPlan.discount), Modifier.weight(1f))
                        }
                        if (bestPlan.picks.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "加购:" + bestPlan.picks.joinToString(" + ") { "¥" + fmt(it) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryLabel
                            )
                            Spacer(Modifier.height(4.dp))
                            val worth = bestPlan.picks.sum()
                            Text(
                                "多花 ¥" + fmt((bestPlan.addSum - bestPlan.discount).coerceAtLeast(0.0)) +
                                    " 拿到价值 ¥" + fmt(worth) + " 的东西",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.green
                            )
                        }
                    }
                }
            }
        }
        item {
            if (plans != null && plans!!.size > 1) {
                SectionHeader("其他档位")
            }
        }
        item {
            val p = plans
            if (p != null && p.size > 1) {
                val baseV = base.toDoubleOrNull() ?: 0.0
                GroupedCard {
                    p.forEachIndexed { i, plan ->
                        KeyValueRow(
                            "凑 ¥" + fmt(plan.addSum) + " 减 ¥" + fmt(plan.discount),
                            "实付 ¥" + fmt(baseV + plan.addSum - plan.discount),
                            copyable = false
                        )
                        if (i != p.lastIndex) RowDivider()
                    }
                }
            }
        }
        item { if (error.isNotEmpty()) GroupedCard { CardPadding { Text(error, style = MaterialTheme.typography.bodyMedium, color = palette.red) } } }
        item { SectionHeader("优惠规则") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("满 X 减 Y", "每满 X 减 Y"), mode, { mode = it }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("满(元)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                            Spacer(Modifier.height(4.dp))
                            IosTextField(threshold, { threshold = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.fillMaxWidth(), placeholder = "300")
                        }
                        Column(Modifier.weight(1f)) {
                            Text("减(元)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                            Spacer(Modifier.height(4.dp))
                            IosTextField(discount, { discount = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.fillMaxWidth(), placeholder = "50")
                        }
                    }
                }
            }
        }
        item { SectionHeader("购物车") }
        item {
            GroupedCard {
                CardPadding {
                    Text("已有金额(元)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(4.dp))
                    IosTextField(base, { base = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.fillMaxWidth(), placeholder = "比如 268.5")
                    Spacer(Modifier.height(12.dp))
                    Text("备选商品价格(空格、逗号或换行分开)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(4.dp))
                    IosTextArea(goods, { goods = it }, Modifier.fillMaxWidth(), placeholder = "19.9 35 12.8\n49.9", minHeight = 88.dp)
                    Spacer(Modifier.height(12.dp))
                    SolidButton(onClick = { calc() }, Modifier.fillMaxWidth()) { Text("算最优凑单") }
                }
            }
        }
    }
}
