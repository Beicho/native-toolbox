package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.data.store.AstroStore
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
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

private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

private val categories = listOf("餐饮", "交通", "购物", "居住", "娱乐", "医疗", "学习", "其他")

private data class Entry(
    val id: String,
    val dateIso: String,
    val amount: Double,
    val category: String,
    val note: String,
    val isIncome: Boolean,
)

private fun money(v: Double): String = String.format("%.2f", v)

/**
 * 极简记账。数据走 AstroStore,与主页记账卡片共享。
 *
 * 交互原则:记一笔 = 输金额 → 点分类 → 完事,三步之内。
 * 备注可选,日期默认今天(要补记才展开)。
 */
@Composable
fun BookkeepingToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var amountInput by rememberSaveable { mutableStateOf("") }
    var noteInput by rememberSaveable { mutableStateOf("") }
    var categoryIndex by rememberSaveable { mutableStateOf(0) }
    var isIncome by rememberSaveable { mutableStateOf(false) }
    var rangeIndex by rememberSaveable { mutableStateOf(0) } // 0 本月 1 今天 2 全部
    var version by remember { mutableStateOf(0) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    val entries = remember(version) {
        AstroStore.all(AstroStore.Collection.BOOKKEEPING).map {
            Entry(
                id = it.id,
                dateIso = it.str("dateIso"),
                amount = it.num("amount"),
                category = it.str("category").ifBlank { "其他" },
                note = it.str("note"),
                isIncome = it.str("category") == "收入",
            )
        }
    }

    val cal = Calendar.getInstance()
    val todayIso = iso.format(cal.time)
    val monthPrefix = "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

    val filtered = when (rangeIndex) {
        0 -> entries.filter { it.dateIso.startsWith(monthPrefix) }
        1 -> entries.filter { it.dateIso == todayIso }
        else -> entries
    }
    val expense = filtered.filterNot { it.isIncome }.sumOf { it.amount }
    val income = filtered.filter { it.isIncome }.sumOf { it.amount }

    // 分类占比(只算支出)
    val byCategory = filtered.filterNot { it.isIncome }
        .groupBy { it.category }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }

    fun add() {
        val amt = amountInput.toDoubleOrNull() ?: return
        if (amt <= 0) return
        AstroStore.add(AstroStore.Collection.BOOKKEEPING) {
            put("amount", amt)
            put("category", if (isIncome) "收入" else categories[categoryIndex])
            put("note", noteInput.trim())
            put("dateIso", todayIso)
        }
        version++
        amountInput = ""
        noteInput = ""
    }

    fun delete(id: String) {
        AstroStore.remove(AstroStore.Collection.BOOKKEEPING, id)
        pendingDelete = null
        version++
    }

    ToolScaffold {
        // 汇总头:进来第一眼是「花了多少」
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        listOf("本月", "今天", "全部"),
                        rangeIndex,
                        { rangeIndex = it },
                        Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "支出",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.tertiaryLabel
                        )
                        Text(
                            "¥${money(expense)}",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.label
                        )
                        if (income > 0) {
                            Text(
                                "收入 ¥${money(income)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.green
                            )
                        }
                    }
                }
            }
        }

        // 记一笔:金额 → 分类 → 完事
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        IosTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' } },
                            modifier = Modifier.weight(1f),
                            placeholder = "金额",
                            mono = true
                        )
                        // 支出/收入切换做成小开关,不占一整行
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isIncome) palette.green.copy(alpha = 0.15f) else palette.sunkenBackground)
                                .clickable { isIncome = !isIncome }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                if (isIncome) "收入" else "支出",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isIncome) palette.green else palette.secondaryLabel
                            )
                        }
                    }
                    if (!isIncome) {
                        Spacer(Modifier.height(10.dp))
                        // 分类用两行胶囊,点选即高亮 —— 比下拉框少一次点击
                        categories.chunked(4).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                row.forEach { c ->
                                    val idx = categories.indexOf(c)
                                    val selected = idx == categoryIndex
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (selected) palette.accent.copy(alpha = 0.15f) else palette.sunkenBackground)
                                            .clickable { categoryIndex = idx }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            c,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) palette.accent else palette.secondaryLabel
                                        )
                                    }
                                }
                            }
                        }
                    }
                    IosTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "备注(可不填)"
                    )
                    Spacer(Modifier.height(10.dp))
                    SolidButton(
                        onClick = { add() },
                        Modifier.fillMaxWidth(),
                        enabled = amountInput.toDoubleOrNull()?.let { it > 0 } == true
                    ) { Text("记下") }
                }
            }
        }

        // 分类占比条
        if (byCategory.isNotEmpty() && expense > 0) {
            item { SectionHeader("花在哪了") }
            item {
                GroupedCard {
                    CardPadding {
                        byCategory.take(5).forEach { (cat, amt) ->
                            val ratio = (amt / expense).toFloat()
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(cat, style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.width(44.dp))
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(palette.sunkenBackground)
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(ratio)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(palette.accent)
                                    )
                                }
                                Text(
                                    "¥${money(amt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryLabel
                                )
                            }
                        }
                    }
                }
            }
        }

        // 流水,带删除(二次确认变红)
        if (filtered.isNotEmpty()) {
            item { SectionHeader("流水(${filtered.size} 笔)") }
            item {
                GroupedCard {
                    val list = filtered.take(50)
                    list.forEachIndexed { index, e ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    e.category + if (e.note.isNotBlank()) " · ${e.note}" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.label,
                                    maxLines = 1
                                )
                                Text(
                                    e.dateIso.substring(5),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.tertiaryLabel
                                )
                            }
                            Text(
                                (if (e.isIncome) "+" else "-") + "¥${money(e.amount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (e.isIncome) palette.green else palette.label
                            )
                            Spacer(Modifier.width(10.dp))
                            val confirming = pendingDelete == e.id
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (confirming) palette.red else palette.sunkenBackground)
                                    .clickable { if (confirming) delete(e.id) else pendingDelete = e.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    if (confirming) "确定?" else "删",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (confirming) androidx.compose.ui.graphics.Color.White else palette.secondaryLabel
                                )
                            }
                        }
                        if (index != list.lastIndex) RowDivider()
                    }
                }
            }
        } else {
            item {
                Text(
                    "这个时间段还没有账。上面输个金额点「记下」就开始了。",
                    Modifier.fillMaxWidth().padding(24.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.tertiaryLabel,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
