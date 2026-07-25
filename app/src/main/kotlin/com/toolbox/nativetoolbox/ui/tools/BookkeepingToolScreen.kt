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
import com.toolbox.nativetoolbox.ui.components.rememberPrefString
import com.toolbox.nativetoolbox.ui.components.rememberToolPrefs
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

private class Entry(val dateIso: String, val amount: Double, val category: String, val note: String)

private val categories = listOf("餐饮", "交通", "购物", "居住", "娱乐", "医疗", "学习", "其他", "收入")

/** 每行格式：日期 金额 分类 备注（日期可省略，默认今天；收入写正数并选分类「收入」） */
private fun parseEntries(raw: String): List<Entry> = raw.lines().mapNotNull { line ->
    val parts = line.trim().split(Regex("[\\s,，]+")).filter { it.isNotBlank() }
    if (parts.size < 2) return@mapNotNull null

    var index = 0
    var dateIso = dateFormat.format(Date())
    val maybeDate = parts[0].replace('/', '-').replace('.', '-')
    if (runCatching { dateFormat.parse(maybeDate) }.getOrNull() != null) {
        dateIso = dateFormat.format(dateFormat.parse(maybeDate)!!)
        index = 1
    }
    val amount = parts.getOrNull(index)?.toDoubleOrNull() ?: return@mapNotNull null
    val category = parts.getOrNull(index + 1)?.takeIf { categories.contains(it) } ?: "其他"
    val note = parts.drop(index + if (categories.contains(parts.getOrNull(index + 1) ?: "")) 2 else 1)
        .joinToString(" ")
    Entry(dateIso, amount, category, note)
}

private fun money(v: Double): String = String.format("%.2f", v)

@Composable
fun BookkeepingToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    val toolPrefs = rememberToolPrefs("bookkeeping")
    var raw by rememberPrefString(toolPrefs, "raw", "")
    var amountInput by rememberSaveable { mutableStateOf("") }
    var categoryIndex by rememberSaveable { mutableStateOf(0) }
    var noteInput by rememberSaveable { mutableStateOf("") }
    var rangeIndex by rememberSaveable { mutableStateOf(0) }

    val entries = parseEntries(raw)

    val now = Calendar.getInstance()
    val monthPrefix = String.format("%04d-%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    val todayIso = dateFormat.format(Date())

    val filtered = when (rangeIndex) {
        0 -> entries.filter { it.dateIso == todayIso }
        1 -> entries.filter { it.dateIso.startsWith(monthPrefix) }
        else -> entries
    }

    val expense = filtered.filter { it.category != "收入" }.sumOf { it.amount }
    val income = filtered.filter { it.category == "收入" }.sumOf { it.amount }
    val byCategory = filtered.filter { it.category != "收入" }
        .groupBy { it.category }
        .mapValues { it.value.sumOf { e -> e.amount } }
        .toList()
        .sortedByDescending { it.second }

    fun add() {
        val amount = amountInput.trim().toDoubleOrNull() ?: return
        val line = todayIso + " " + money(amount) + " " + categories[categoryIndex] +
            (if (noteInput.isBlank()) "" else " " + noteInput.trim())
        raw = (raw.trim() + "\n" + line).trim()
        amountInput = ""
        noteInput = ""
    }

    ToolScaffold {
        item { SectionHeader("记一笔") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "金额",
                            mono = true
                        )
                        IosTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            modifier = Modifier.weight(2f),
                            placeholder = "备注（可选）"
                        )
                    }
                    SegmentedPicker(
                        options = categories.take(5),
                        selectedIndex = categoryIndex.coerceAtMost(4),
                        onSelected = { categoryIndex = it }
                    )
                    SegmentedPicker(
                        options = categories.drop(5),
                        selectedIndex = (categoryIndex - 5).coerceAtLeast(0),
                        onSelected = { categoryIndex = it + 5 }
                    )
                    SolidButton(onClick = { add() }, enabled = amountInput.trim().toDoubleOrNull() != null) {
                        Text("记到 " + categories[categoryIndex])
                    }
                }
            }
        }
        item { SectionHeader("统计范围") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("今天", "本月", "全部"),
                        selectedIndex = rangeIndex,
                        onSelected = { rangeIndex = it }
                    )
                }
            }
        }
        item { SectionHeader("汇总") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("支出", money(expense), Modifier.weight(1f))
                        StatCell("收入", money(income), Modifier.weight(1f))
                        StatCell("结余", money(income - expense), Modifier.weight(1f))
                    }
                    Text(
                        "共 " + filtered.size + " 笔记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        if (byCategory.isNotEmpty()) {
            item { SectionHeader("分类占比") }
            item {
                GroupedCard {
                    byCategory.forEachIndexed { index, (category, amount) ->
                        KeyValueRow(
                            category,
                            money(amount) + "　" + String.format("%.0f%%", amount / expense * 100),
                            copyable = false
                        )
                        if (index != byCategory.lastIndex) RowDivider()
                    }
                }
            }
        }
        if (filtered.isNotEmpty()) {
            item { SectionHeader("明细") }
            item {
                GroupedCard {
                    filtered.reversed().take(30).forEachIndexed { index, entry ->
                        KeyValueRow(
                            entry.dateIso.substring(5) + "　" + entry.category +
                                (if (entry.note.isBlank()) "" else "　" + entry.note),
                            (if (entry.category == "收入") "+" else "-") + money(entry.amount),
                            copyable = false
                        )
                        if (index != filtered.reversed().take(30).lastIndex) RowDivider()
                    }
                }
            }
        }
        item { SectionHeader("原始账本") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = raw,
                        onValueChange = { raw = it },
                        placeholder = "一行一笔：日期 金额 分类 备注\n例如：\n2026-07-25 32 餐饮 午饭\n2026-07-25 8000 收入 工资",
                        minHeight = 140.dp,
                        mono = true
                    )
                    OutputCard(text = raw.ifBlank { "还没有记录" }, label = "复制备份")
                    Text(
                        "记录只留在这个页面，退出会清空。复制上面这段存到备忘录就能长期保留，下次粘回来继续用。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
