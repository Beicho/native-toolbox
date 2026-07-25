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
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val weekdayNames = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

private class Event(val name: String, val dateIso: String)

private fun parseEvents(raw: String): List<Event> = raw.lines().mapNotNull { line ->
    val parts = line.split(Regex("[\\s,，]+")).filter { it.isNotBlank() }
    if (parts.size < 2) return@mapNotNull null
    val dateText = parts.last().replace('/', '-').replace('.', '-')
    val normalized = runCatching {
        val d = iso.parse(dateText) ?: return@runCatching null
        iso.format(d)
    }.getOrNull() ?: return@mapNotNull null
    val name = parts.dropLast(1).joinToString(" ")
    Event(name.ifBlank { "未命名" }, normalized)
}

private fun todayStart(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun daysUntil(dateIso: String): Int? {
    val target = runCatching { iso.parse(dateIso) }.getOrNull() ?: return null
    return Math.round((target.time - todayStart()) / 86_400_000.0).toInt()
}

private fun weekdayOf(dateIso: String): String {
    val target = runCatching { iso.parse(dateIso) }.getOrNull() ?: return ""
    val cal = Calendar.getInstance().apply { time = target }
    return weekdayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
}

@Composable
fun CountdownDayToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var raw by rememberSaveable { mutableStateOf("") }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var dateInput by rememberSaveable { mutableStateOf("") }

    val events = parseEvents(raw)
    val withDays = events.mapNotNull { e -> daysUntil(e.dateIso)?.let { e to it } }
    val upcoming = withDays.filter { it.second >= 0 }.sortedBy { it.second }
    val past = withDays.filter { it.second < 0 }.sortedByDescending { it.second }
    val nearest = upcoming.firstOrNull()

    fun add() {
        val name = nameInput.trim()
        val date = dateInput.trim().replace('/', '-').replace('.', '-')
        if (name.isBlank() || runCatching { iso.parse(date) }.getOrNull() == null) return
        raw = (raw.trim() + "\n" + name + " " + date).trim()
        nameInput = ""
        dateInput = ""
    }

    ToolScaffold {
        item { SectionHeader("最近的日子") }
        item {
            GroupedCard {
                CardPadding {
                    if (nearest != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell(
                                nearest.first.name,
                                if (nearest.second == 0) "就是今天" else nearest.second.toString() + " 天",
                                Modifier.weight(1f)
                            )
                            StatCell(
                                "日期",
                                nearest.first.dateIso.substring(5) + " " + weekdayOf(nearest.first.dateIso),
                                Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            "还没有未来的日子。在下面加一个吧。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
        item { SectionHeader("添加") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "名称"
                        )
                        IosTextField(
                            value = dateInput,
                            onValueChange = { dateInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "2026-10-01",
                            mono = true
                        )
                    }
                    SolidButton(
                        onClick = { add() },
                        enabled = nameInput.isNotBlank() && dateInput.isNotBlank()
                    ) { Text("加进列表") }
                }
            }
        }
        if (upcoming.isNotEmpty()) {
            item { SectionHeader("还没到（" + upcoming.size + "）") }
            item {
                GroupedCard {
                    upcoming.forEachIndexed { index, (event, days) ->
                        KeyValueRow(
                            event.name + "　" + event.dateIso,
                            if (days == 0) "就是今天" else "还有 " + days + " 天",
                            copyable = false
                        )
                        if (index != upcoming.lastIndex) RowDivider()
                    }
                }
            }
        }
        if (past.isNotEmpty()) {
            item { SectionHeader("已经过去（" + past.size + "）") }
            item {
                GroupedCard {
                    past.forEachIndexed { index, (event, days) ->
                        KeyValueRow(
                            event.name + "　" + event.dateIso,
                            "已过 " + (-days) + " 天",
                            copyable = false
                        )
                        if (index != past.lastIndex) RowDivider()
                    }
                }
            }
        }
        item { SectionHeader("全部日子") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = raw,
                        onValueChange = { raw = it },
                        placeholder = "一行一个：名称 日期\n例如：\n国庆 2026-10-01\n生日 2026-08-15",
                        singleLine = false,
                        minLines = 4,
                        mono = true
                    )
                    Text(
                        "可以直接在这里编辑或删除。日期支持 2026-10-01、2026/10/01 两种写法。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
