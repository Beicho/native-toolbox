package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.net.AstroApi
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private class DayInfo(val date: String, val name: String, val isOffDay: Boolean)

private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

private fun daysBetween(fromIso: String, toIso: String): Int? {
    val from = runCatching { isoFormat.parse(fromIso) }.getOrNull() ?: return null
    val to = runCatching { isoFormat.parse(toIso) }.getOrNull() ?: return null
    return Math.round((to.time - from.time) / 86_400_000.0).toInt()
}

@Composable
fun HolidayToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    val thisYear = Calendar.getInstance().get(Calendar.YEAR)
    var yearIndex by rememberSaveable { mutableStateOf(0) }
    var raw by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var filterIndex by rememberSaveable { mutableStateOf(0) }

    val years = listOf(thisYear, thisYear + 1)
    val year = years[yearIndex]

    fun load() {
        loading = true
        status = ""
        scope.launch {
            AstroApi.get("/holiday", mapOf("year" to year.toString()))
                .onSuccess { res ->
                    raw = res.data.toString()
                    status = cachedHint(res.cachedAt)
                }
                .onFailure { e ->
                    raw = null
                    status = e.message ?: "获取失败，请检查网络"
                }
            loading = false
        }
    }

    LaunchedEffect(year) { load() }

    val json = raw?.let { runCatching { JSONObject(it) }.getOrNull() }
    val days: List<DayInfo> = json?.let { obj ->
        val container = obj.optJSONObject("days") ?: obj.optJSONObject("data") ?: obj
        val list = ArrayList<DayInfo>()
        val arr = obj.optJSONArray("days")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                list.add(
                    DayInfo(
                        date = item.optString("date"),
                        name = item.optString("name"),
                        isOffDay = item.optBoolean("isOffDay", true)
                    )
                )
            }
        } else {
            // 兼容 {"2026-01-01": {...}} 这种以日期为键的结构
            container.keys().forEach { key ->
                val item = container.optJSONObject(key) ?: return@forEach
                list.add(
                    DayInfo(
                        date = key,
                        name = item.optString("name"),
                        isOffDay = item.optBoolean("isOffDay", true)
                    )
                )
            }
        }
        list.sortedBy { it.date }
    } ?: emptyList()

    val todayIso = isoFormat.format(Date())
    val offDays = days.filter { it.isOffDay }
    val workDays = days.filter { !it.isOffDay }
    val nextOff = offDays.firstOrNull { it.date >= todayIso }
    val countdown = nextOff?.let { daysBetween(todayIso, it.date) }

    val shown = when (filterIndex) {
        0 -> days
        1 -> offDays
        else -> workDays
    }.filter { it.date >= todayIso || filterIndex == 0 }

    ToolScaffold {
        item { SectionHeader("年份") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = years.map { it.toString() + " 年" },
                        selectedIndex = yearIndex,
                        onSelected = { yearIndex = it }
                    )
                    SolidButton(onClick = { load() }, enabled = !loading, filled = false) {
                        Text(if (loading) "获取中…" else "刷新")
                    }
                    Text(
                        "数据来自国务院办公厅发布的放假安排，需要联网获取。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        if (status.isNotBlank()) {
            item {
                GroupedCard {
                    CardPadding {
                        Text(status, style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    }
                }
            }
        }
        if (days.isNotEmpty()) {
            item { SectionHeader("下一个假期") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("假期", nextOff?.name ?: "—", Modifier.weight(1f))
                            StatCell(
                                "还有",
                                countdown?.let { if (it == 0) "就是今天" else it.toString() + " 天" } ?: "—",
                                Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("全年放假", offDays.size.toString() + " 天", Modifier.weight(1f))
                            StatCell("调休上班", workDays.size.toString() + " 天", Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SectionHeader("安排明细") }
            item {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(
                            options = listOf("全部", "放假", "补班"),
                            selectedIndex = filterIndex,
                            onSelected = { filterIndex = it }
                        )
                    }
                }
            }
            item {
                GroupedCard {
                    if (shown.isEmpty()) {
                        CardPadding {
                            Text(
                                "今年剩下没有这类日期了",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryLabel
                            )
                        }
                    } else {
                        shown.forEachIndexed { index, day ->
                            KeyValueRow(
                                day.date + "　" + day.name,
                                if (day.isOffDay) "放假" else "补班",
                                copyable = false
                            )
                            if (index != shown.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}
