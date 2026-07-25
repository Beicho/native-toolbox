package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.security.SecureRandom

private val rng = SecureRandom()

private fun rollInts(min: Long, max: Long, count: Int, unique: Boolean, sorted: Boolean): List<Long>? {
    val span = max - min + 1
    if (span <= 0) return null
    if (unique && count > span) return null
    val out: MutableList<Long>
    if (unique && span <= 1_000_000) {
        // 小范围去重:洗牌取前 N
        val pool = (min..max).toMutableList()
        for (i in pool.indices.reversed()) {
            val j = rng.nextInt(i + 1)
            val t = pool[i]; pool[i] = pool[j]; pool[j] = t
        }
        out = pool.take(count).toMutableList()
    } else if (unique) {
        // 大范围去重:集合采样
        val set = LinkedHashSet<Long>()
        while (set.size < count) set.add(min + (rng.nextDouble() * span).toLong().coerceAtMost(span - 1))
        out = set.toMutableList()
    } else {
        out = MutableList(count) { min + (rng.nextDouble() * span).toLong().coerceAtMost(span - 1) }
    }
    if (sorted) out.sort()
    return out
}

private fun pickUnique(range: IntRange, n: Int): List<Int> {
    val pool = range.toMutableList()
    for (i in pool.indices.reversed()) {
        val j = rng.nextInt(i + 1)
        val t = pool[i]; pool[i] = pool[j]; pool[j] = t
    }
    return pool.take(n).sorted()
}

private val DICE_FACES = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

@Composable
fun RandomNumToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var mode by rememberSaveable { mutableStateOf(0) } // 0 数字 1 骰子 2 硬币 3 彩票
    var result by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(listOf<String>()) }

    // 数字模式
    var minText by rememberSaveable { mutableStateOf("1") }
    var maxText by rememberSaveable { mutableStateOf("100") }
    var countText by rememberSaveable { mutableStateOf("1") }
    var unique by rememberSaveable { mutableStateOf(false) }
    var sorted by rememberSaveable { mutableStateOf(false) }
    // 骰子/硬币个数
    var diceText by rememberSaveable { mutableStateOf("2") }
    var coinText by rememberSaveable { mutableStateOf("1") }
    // 彩票玩法
    var lottery by rememberSaveable { mutableStateOf(0) } // 0 双色球 1 大乐透

    fun push(r: String) {
        result = r
        history = (listOf(r) + history).take(10)
    }

    fun run() {
        error = ""; detail = ""
        when (mode) {
            0 -> {
                val mn = minText.toLongOrNull()
                val mx = maxText.toLongOrNull()
                val n = countText.toIntOrNull()
                if (mn == null || mx == null) { error = "范围要填数字"; return }
                if (mx < mn) { error = "最大值比最小值还小"; return }
                if (n == null || n !in 1..1000) { error = "个数 1~1000"; return }
                val r = rollInts(mn, mx, n, unique, sorted)
                if (r == null) { error = "去重模式下,个数不能超过范围内的数字总数"; return }
                push(r.joinToString(" "))
                if (n > 1) detail = "共 ${r.size} 个 · 合计 ${r.sum()}"
            }
            1 -> {
                val n = diceText.toIntOrNull()
                if (n == null || n !in 1..20) { error = "骰子 1~20 颗"; return }
                val rolls = List(n) { rng.nextInt(6) }
                push(rolls.joinToString(" ") { DICE_FACES[it] })
                detail = rolls.joinToString(" + ") { (it + 1).toString() } + " = ${rolls.sumOf { it + 1 }}"
            }
            2 -> {
                val n = coinText.toIntOrNull()
                if (n == null || n !in 1..50) { error = "硬币 1~50 枚"; return }
                val flips = List(n) { rng.nextBoolean() }
                push(flips.joinToString(" ") { if (it) "正" else "反" })
                if (n > 1) detail = "正面 ${flips.count { it }} · 反面 ${flips.count { !it }}"
            }
            3 -> {
                if (lottery == 0) {
                    val red = pickUnique(1..33, 6)
                    val blue = 1 + rng.nextInt(16)
                    push(red.joinToString(" ") { "%02d".format(it) } + "  |  " + "%02d".format(blue))
                    detail = "红球 6 个(01~33) + 蓝球 1 个(01~16),纯随机,买不买随你"
                } else {
                    val front = pickUnique(1..35, 5)
                    val backs = pickUnique(1..12, 2)
                    push(front.joinToString(" ") { "%02d".format(it) } + "  |  " + backs.joinToString(" ") { "%02d".format(it) })
                    detail = "前区 5 个(01~35) + 后区 2 个(01~12),纯随机,买不买随你"
                }
            }
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    if (result.isEmpty()) {
                        Text("点下面的按钮开摇", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                    } else {
                        Text(
                            result,
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = if (mode == 1) 40.sp else 26.sp),
                            fontWeight = FontWeight.Bold,
                            color = palette.label
                        )
                        if (detail.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        }
                    }
                    if (error.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                    }
                    Spacer(Modifier.height(12.dp))
                    SolidButton(onClick = { run() }, Modifier.fillMaxWidth()) {
                        Text(listOf("生成随机数", "掷骰子", "抛硬币", "机选一注")[mode])
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("数字", "骰子", "硬币", "彩票"), mode, { mode = it; result = ""; detail = ""; error = "" }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    when (mode) {
                        0 -> {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("从", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                                    Spacer(Modifier.height(4.dp))
                                    IosTextField(minText, { minText = it.filter { c -> c.isDigit() || c == '-' } }, Modifier.fillMaxWidth(), placeholder = "1")
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("到", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                                    Spacer(Modifier.height(4.dp))
                                    IosTextField(maxText, { maxText = it.filter { c -> c.isDigit() || c == '-' } }, Modifier.fillMaxWidth(), placeholder = "100")
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("个数", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                                    Spacer(Modifier.height(4.dp))
                                    IosTextField(countText, { countText = it.filter { c -> c.isDigit() } }, Modifier.fillMaxWidth(), placeholder = "1")
                                }
                            }
                        }
                        1 -> {
                            Text("骰子数量(1~20)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                            Spacer(Modifier.height(4.dp))
                            IosTextField(diceText, { diceText = it.filter { c -> c.isDigit() } }, Modifier.fillMaxWidth(), placeholder = "2")
                        }
                        2 -> {
                            Text("硬币数量(1~50)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                            Spacer(Modifier.height(4.dp))
                            IosTextField(coinText, { coinText = it.filter { c -> c.isDigit() } }, Modifier.fillMaxWidth(), placeholder = "1")
                        }
                        3 -> {
                            SegmentedPicker(listOf("双色球", "大乐透"), lottery, { lottery = it }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        item {
            if (mode == 0) {
                GroupedCard {
                    ToggleRow("结果去重", unique) { unique = it }
                    RowDivider()
                    ToggleRow("从小到大排序", sorted) { sorted = it }
                }
            }
        }
        item { if (history.size > 1) SectionHeader("最近几次") }
        item {
            if (history.size > 1) {
                GroupedCard {
                    history.drop(1).forEachIndexed { i, h ->
                        KeyValueRow("第 ${history.size - 1 - i} 次前", h)
                        if (i != history.size - 2) RowDivider()
                    }
                }
            }
        }
    }
}
