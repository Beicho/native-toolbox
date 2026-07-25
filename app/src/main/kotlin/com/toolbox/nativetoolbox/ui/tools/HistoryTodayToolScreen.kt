package com.toolbox.nativetoolbox.ui.tools

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.net.AstroApi
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.CnConvert
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class HistEvent(val year: Int, val text: String)

@Composable
fun HistoryTodayToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var events by remember { mutableStateOf<List<HistEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var cachedNote by remember { mutableStateOf("") }
    var retry by remember { mutableStateOf(0) }

    val title = remember { SimpleDateFormat("M 月 d 日", Locale.CHINESE).format(Date()) }

    LaunchedEffect(retry) {
        loading = true; error = ""
        val cal = Calendar.getInstance()
        val r = AstroApi.get(
            "/today",
            mapOf("m" to "%02d".format(cal.get(Calendar.MONTH) + 1), "d" to "%02d".format(cal.get(Calendar.DAY_OF_MONTH)))
        )
        r.onSuccess { res ->
            if (res.cachedAt > 0) {
                cachedNote = "离线数据 · " + SimpleDateFormat("M月d日 HH:mm", Locale.CHINESE).format(Date(res.cachedAt))
            }
            runCatching {
                val found = res.data.optBoolean("found", false)
                if (!found) { error = "今天的数据还没准备好,明天再来看看"; return@runCatching }
                val arr = res.data.getJSONObject("events").getJSONArray("selected")
                val list = ArrayList<HistEvent>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    // 维基数据是繁体,就地转简体
                    list.add(HistEvent(o.optInt("y"), CnConvert.toSimplified(o.optString("t"))))
                }
                events = list.sortedByDescending { it.year }
            }.onFailure { error = "数据格式异常" }
        }.onFailure { error = it.message ?: "加载失败" }
        loading = false
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Text("历史上的 $title", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.label)
                    if (cachedNote.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(cachedNote, style = MaterialTheme.typography.bodySmall, color = palette.orange)
                    }
                    if (loading) {
                        Spacer(Modifier.height(8.dp))
                        Text("翻历史书中…", style = MaterialTheme.typography.bodyMedium, color = palette.tertiaryLabel)
                    }
                    if (error.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                        Spacer(Modifier.height(10.dp))
                        SolidButton(onClick = { retry++ }, Modifier.fillMaxWidth(), filled = false) { Text("重试") }
                    }
                }
            }
        }
        item { if (events.isNotEmpty()) SectionHeader("这一天发生过(${events.size} 件)") }
        item {
            if (events.isNotEmpty()) {
                GroupedCard {
                    events.forEachIndexed { i, e ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(
                                "${e.year}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent,
                                modifier = Modifier.width(56.dp)
                            )
                            Text(e.text, style = MaterialTheme.typography.bodyMedium, color = palette.label)
                        }
                        if (i != events.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
