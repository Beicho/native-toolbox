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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val weekdayNames = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

private data class CdEvent(val id: String, val name: String, val dateIso: String)

private fun todayStart(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun daysUntil(dateIso: String): Int? {
    val target = runCatching { iso.parse(dateIso) }.getOrNull() ?: return null
    return Math.round((target.time - todayStart()) / 86_400_000.0).toInt()
}

private fun weekdayOf(dateIso: String): String {
    val target = runCatching { iso.parse(dateIso) }.getOrNull() ?: return ""
    val cal = Calendar.getInstance().apply { time = target }
    return weekdayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
}

/**
 * 倒数日。数据走 AstroStore —— 主页卡片、桌面小组件读的是同一份。
 * 长按条目删除(带确认色变化),不再用「一大坨文本自己编辑」那种奇怪交互。
 */
@Composable
fun CountdownDayToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var nameInput by rememberSaveable { mutableStateOf("") }
    var dateInput by rememberSaveable { mutableStateOf("") }
    var version by remember { mutableStateOf(0) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    val events = remember(version) {
        AstroStore.all(AstroStore.Collection.COUNTDOWN).map {
            CdEvent(it.id, it.str("title").ifBlank { "倒数日" }, it.str("dateIso"))
        }
    }
    val withDays = events.mapNotNull { e -> daysUntil(e.dateIso)?.let { e to it } }
    val upcoming = withDays.filter { it.second >= 0 }.sortedBy { it.second }
    val past = withDays.filter { it.second < 0 }.sortedByDescending { it.second }
    val nearest = upcoming.firstOrNull()

    fun add() {
        val name = nameInput.trim()
        val date = dateInput.trim().replace('/', '-').replace('.', '-')
        if (name.isBlank() || runCatching { iso.parse(date) }.getOrNull() == null) return
        AstroStore.add(AstroStore.Collection.COUNTDOWN) {
            put("title", name)
            put("dateIso", iso.format(iso.parse(date)!!))
        }
        version++
        nameInput = ""
        dateInput = ""
    }

    fun delete(id: String) {
        AstroStore.remove(AstroStore.Collection.COUNTDOWN, id)
        pendingDelete = null
        version++
    }

    /** 一行倒数日:点一下无事,再点删除按钮需要二次确认(变红) */
    @Composable
    fun eventRow(event: CdEvent, days: Int, isLast: Boolean) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    event.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = palette.label
                )
                Text(
                    event.dateIso + " " + weekdayOf(event.dateIso),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.tertiaryLabel
                )
            }
            Text(
                when {
                    days == 0 -> "今天"
                    days > 0 -> "$days 天"
                    else -> "已过 ${-days} 天"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    days == 0 -> palette.orange
                    days in 1..7 -> palette.orange
                    days > 0 -> palette.accent
                    else -> palette.tertiaryLabel
                }
            )
            Spacer(Modifier.width(12.dp))
            // 删除:第一次点变红问「确定?」,再点才真删。误触率比弹窗低,速度比弹窗快
            val confirming = pendingDelete == event.id
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (confirming) palette.red else palette.sunkenBackground)
                    .clickable { if (confirming) delete(event.id) else pendingDelete = event.id }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    if (confirming) "确定?" else "删",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (confirming) androidx.compose.ui.graphics.Color.White else palette.secondaryLabel
                )
            }
        }
        if (!isLast) RowDivider()
    }

    ToolScaffold {
        // 头部:最近的一个大字突出 —— 这是打开这个页面最想看的信息
        item {
            GroupedCard {
                CardPadding {
                    if (nearest != null) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                nearest.first.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.label
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    if (nearest.second == 0) "今天" else "${nearest.second}",
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (nearest.second <= 7) palette.orange else palette.accent
                                )
                                if (nearest.second > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "天后",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.secondaryLabel,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                            }
                            Text(
                                nearest.first.dateIso + " " + weekdayOf(nearest.first.dateIso),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.tertiaryLabel
                            )
                        }
                    } else {
                        Text(
                            "记下重要的日子,主页和桌面小组件上都会倒着数给你看",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.tertiaryLabel,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IosTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            modifier = Modifier.weight(1.2f),
                            placeholder = "什么日子"
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
                    ) { Text("添加") }
                }
            }
        }
        if (upcoming.isNotEmpty()) {
            item { SectionHeader("还没到(${upcoming.size})") }
            item {
                GroupedCard {
                    upcoming.forEachIndexed { index, (event, days) ->
                        eventRow(event, days, index == upcoming.lastIndex)
                    }
                }
            }
        }
        if (past.isNotEmpty()) {
            item { SectionHeader("已经过去(${past.size})") }
            item {
                GroupedCard {
                    past.forEachIndexed { index, (event, days) ->
                        eventRow(event, days, index == past.lastIndex)
                    }
                }
            }
        }
    }
}
