package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private class Zone(val city: String, val id: String)

private val zones = listOf(
    Zone("北京", "Asia/Shanghai"),
    Zone("香港", "Asia/Hong_Kong"),
    Zone("东京", "Asia/Tokyo"),
    Zone("首尔", "Asia/Seoul"),
    Zone("新加坡", "Asia/Singapore"),
    Zone("曼谷", "Asia/Bangkok"),
    Zone("新德里", "Asia/Kolkata"),
    Zone("迪拜", "Asia/Dubai"),
    Zone("莫斯科", "Europe/Moscow"),
    Zone("柏林", "Europe/Berlin"),
    Zone("巴黎", "Europe/Paris"),
    Zone("伦敦", "Europe/London"),
    Zone("纽约", "America/New_York"),
    Zone("芝加哥", "America/Chicago"),
    Zone("洛杉矶", "America/Los_Angeles"),
    Zone("温哥华", "America/Vancouver"),
    Zone("圣保罗", "America/Sao_Paulo"),
    Zone("悉尼", "Australia/Sydney"),
    Zone("奥克兰", "Pacific/Auckland"),
    Zone("协调世界时", "UTC")
)

private fun offsetText(tz: TimeZone, at: Long): String {
    val minutes = tz.getOffset(at) / 60000
    val sign = if (minutes >= 0) "+" else "-"
    val abs = Math.abs(minutes)
    return "UTC" + sign + String.format("%02d:%02d", abs / 60, abs % 60)
}

@Composable
fun TimezoneToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var use24 by rememberSaveable { mutableStateOf(true) }
    var keyword by rememberSaveable { mutableStateOf("") }

    DisposableEffect(Unit) {
        val job = scope.launch {
            while (isActive) {
                now = System.currentTimeMillis()
                delay(1000)
            }
        }
        onDispose { job.cancel() }
    }

    val localZone = TimeZone.getDefault()
    val pattern = if (use24) "HH:mm:ss" else "hh:mm:ss a"
    val filtered = if (keyword.isBlank()) zones
    else zones.filter { it.city.contains(keyword.trim()) || it.id.contains(keyword.trim(), ignoreCase = true) }

    fun timeIn(zone: Zone): String {
        val tz = TimeZone.getTimeZone(zone.id)
        val fmt = SimpleDateFormat(pattern, Locale.getDefault())
        fmt.timeZone = tz
        return fmt.format(Date(now))
    }

    fun dayLabel(zone: Zone): String {
        val tz = TimeZone.getTimeZone(zone.id)
        val localCal = Calendar.getInstance(localZone).apply { timeInMillis = now }
        val zoneCal = Calendar.getInstance(tz).apply { timeInMillis = now }
        val diff = zoneCal.get(Calendar.DAY_OF_YEAR) - localCal.get(Calendar.DAY_OF_YEAR)
        return when {
            diff == 0 -> "今天"
            diff == 1 || diff < -300 -> "明天"
            diff == -1 || diff > 300 -> "昨天"
            else -> ""
        }
    }

    ToolScaffold {
        item { SectionHeader("本机时间") }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        SimpleDateFormat(if (use24) "yyyy-MM-dd HH:mm:ss" else "yyyy-MM-dd hh:mm:ss a", Locale.getDefault())
                            .format(Date(now)),
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.label
                    )
                    Text(
                        localZone.id + "　" + offsetText(localZone, now),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
                ToggleRow("24 小时制", use24, onCheckedChange = { use24 = it })
            }
        }
        item { SectionHeader("搜索城市") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        placeholder = "输入城市名或时区，如 纽约 / Tokyo"
                    )
                }
            }
        }
        item { SectionHeader(if (filtered.isEmpty()) "没找到" else "世界时间") }
        item {
            GroupedCard {
                if (filtered.isEmpty()) {
                    CardPadding {
                        Text(
                            "换个关键词试试",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    }
                } else {
                    filtered.forEachIndexed { index, zone ->
                        val tz = TimeZone.getTimeZone(zone.id)
                        val day = dayLabel(zone)
                        KeyValueRow(
                            zone.city + "　" + offsetText(tz, now),
                            timeIn(zone) + if (day.isNotBlank() && day != "今天") "（" + day + "）" else "",
                            copyable = false
                        )
                        if (index != filtered.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
