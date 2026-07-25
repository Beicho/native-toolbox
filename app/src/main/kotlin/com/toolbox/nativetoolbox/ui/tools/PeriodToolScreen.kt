package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val DAY_MS = 86400000L

private fun loadStarts(prefs: android.content.SharedPreferences): List<Long> = runCatching {
    val arr = JSONArray(prefs.getString("starts", "[]"))
    (0 until arr.length()).map { arr.getLong(it) }.sorted()
}.getOrDefault(emptyList())

private fun saveStarts(prefs: android.content.SharedPreferences, list: List<Long>) {
    val arr = JSONArray(); list.sorted().takeLast(24).forEach { arr.put(it) }
    prefs.edit().putString("starts", arr.toString()).apply()
}

private fun todayStart(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun PeriodToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("period", android.content.Context.MODE_PRIVATE) }
    var starts by remember { mutableStateOf(loadStarts(prefs)) }
    var status by remember { mutableStateOf("") }

    val df = remember { SimpleDateFormat("M月d日", Locale.CHINESE) }

    // 周期 = 最近几次间隔均值,样本不足用 28
    val cycles = starts.zipWithNext { a, b -> ((b - a) / DAY_MS).toInt() }.filter { it in 15..60 }
    val cycleLen = if (cycles.isEmpty()) 28 else cycles.takeLast(6).average().toInt()
    val last = starts.lastOrNull()
    val today = todayStart()

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    if (last == null) {
                        Text("记录每次经期的第一天,几次之后就能帮你预测", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                    } else {
                        val dayInCycle = ((today - last) / DAY_MS).toInt() + 1
                        val nextStart = last + cycleLen.toLong() * DAY_MS
                        val daysToNext = ((nextStart - today) / DAY_MS).toInt()
                        val ovulation = nextStart - 14L * DAY_MS
                        val fertileStart = ovulation - 5L * DAY_MS
                        val fertileEnd = ovulation + 1L * DAY_MS

                        Text(
                            when {
                                daysToNext in 0..2 -> "预计这几天就来,提前备好"
                                daysToNext < 0 -> "比预计晚了 ${-daysToNext} 天"
                                today in fertileStart..fertileEnd -> "现在处于易孕期"
                                else -> "$daysToNext 天后来"
                            },
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.label
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("预计下次:${df.format(Date(nextStart))}", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("周期第", "$dayInCycle 天", Modifier.weight(1f))
                            StatCell("平均周期", "$cycleLen 天", Modifier.weight(1f))
                            StatCell("记录", "${starts.size} 次", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "易孕窗口:${df.format(Date(fertileStart))} ~ ${df.format(Date(fertileEnd))}(排卵约 ${df.format(Date(ovulation))})",
                            style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SolidButton(
                        onClick = {
                            if (starts.contains(today)) { status = "今天已经记过了" }
                            else {
                                starts = (starts + today).sorted()
                                saveStarts(prefs, starts)
                                status = "记好了"
                            }
                        },
                        Modifier.fillMaxWidth()
                    ) { Text("今天来了,记一笔") }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(status, style = MaterialTheme.typography.bodySmall, color = if (status == "记好了") palette.green else palette.orange)
                    }
                }
            }
        }
        item { if (starts.isNotEmpty()) SectionHeader("历史记录") }
        item {
            if (starts.isNotEmpty()) {
                GroupedCard {
                    val recent = starts.sortedDescending().take(8)
                    recent.forEachIndexed { i, t ->
                        val gap = if (i < recent.size - 1) "间隔 ${((t - recent[i + 1]) / DAY_MS).toInt()} 天" else ""
                        KeyValueRow(df.format(Date(t)), gap, copyable = false)
                        if (i != recent.lastIndex) RowDivider()
                    }
                }
            }
        }
        item {
            if (starts.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SolidButton(
                        onClick = {
                            starts = starts.dropLast(1)
                            saveStarts(prefs, starts)
                            status = "已撤销最近一条"
                        },
                        Modifier.fillMaxWidth(), filled = false
                    ) { Text("撤销最近一条(点错了用)") }
                }
            }
        }
        item {
            Text(
                "数据只存在手机里。周期长期不规律建议咨询医生",
                Modifier.fillMaxWidth().padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = palette.tertiaryLabel,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
