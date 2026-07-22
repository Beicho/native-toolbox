package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val cronFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/** 解析一个 cron 字段为匹配集合 */
private fun parseField(field: String, min: Int, max: Int): Set<Int>? {
    val result = mutableSetOf<Int>()
    for (part in field.split(",")) {
        val stepSplit = part.split("/")
        if (stepSplit.size > 2) return null
        val step = if (stepSplit.size == 2) stepSplit[1].toIntOrNull() ?: return null else 1
        if (step <= 0) return null
        val range = stepSplit[0]
        val (start, end) = when {
            range == "*" || range.isEmpty() -> min to max
            range.contains("-") -> {
                val ab = range.split("-")
                if (ab.size != 2) return null
                val a = ab[0].toIntOrNull() ?: return null
                val b = ab[1].toIntOrNull() ?: return null
                a to b
            }
            else -> {
                val v = range.toIntOrNull() ?: return null
                if (stepSplit.size == 2) v to max else v to v
            }
        }
        if (start < min || end > max || start > end) return null
        var v = start
        while (v <= end) {
            result.add(v)
            v += step
        }
    }
    return result
}

private fun fieldDesc(field: String, unit: String): String = when {
    field == "*" -> "每$unit"
    field.startsWith("*/") -> "每 ${field.removePrefix("*/")} $unit"
    else -> "$unit $field"
}

@Composable
fun CronToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("*/5 * * * *") }

    val fields = input.trim().split(Regex("\\s+"))
    val valid = fields.size == 5
    val minutes = if (valid) parseField(fields[0], 0, 59) else null
    val hours = if (valid) parseField(fields[1], 0, 23) else null
    val doms = if (valid) parseField(fields[2], 1, 31) else null
    val months = if (valid) parseField(fields[3], 1, 12) else null
    val dows = if (valid) parseField(fields[4], 0, 7)?.map { if (it == 7) 0 else it }?.toSet() else null
    val ok = minutes != null && hours != null && doms != null && months != null && dows != null

    val desc = if (!ok) "格式:分 时 日 月 周(5 段,空格分隔)" else buildString {
        append(fieldDesc(fields[0], "分钟"))
        append(" · ")
        append(fieldDesc(fields[1], "小时"))
        if (fields[2] != "*") append(" · 每月 ${fields[2]} 日")
        if (fields[3] != "*") append(" · ${fields[3]} 月")
        if (fields[4] != "*") append(" · 周${fields[4]}")
    }

    // 未来 5 次执行时间(逐分钟扫描,上限 2 年)
    val nextRuns = if (!ok) emptyList() else buildList {
        var t = LocalDateTime.now().withSecond(0).withNano(0).plusMinutes(1)
        var scanned = 0
        while (size < 5 && scanned < 2 * 366 * 24 * 60) {
            val dowMatch = dows!!.contains(t.dayOfWeek.value % 7)
            val domMatch = doms!!.contains(t.dayOfMonth)
            // 标准 cron:日与周任一匹配(除非其一为 *)
            val dayOk = when {
                fields[2] == "*" && fields[4] == "*" -> true
                fields[2] == "*" -> dowMatch
                fields[4] == "*" -> domMatch
                else -> domMatch || dowMatch
            }
            if (minutes!!.contains(t.minute) && hours!!.contains(t.hour) && months!!.contains(t.monthValue) && dayOk) {
                add(t)
            }
            t = t.plusMinutes(1)
            scanned++
        }
    }

    ToolScaffold {
        item { SectionHeader("Cron 表达式(分 时 日 月 周)") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "*/5 * * * *",
                        mono = true
                    )
                    OutputCard(text = desc, label = "含义")
                }
            }
        }
        item { SectionHeader("未来 5 次执行") }
        item {
            GroupedCard {
                if (nextRuns.isEmpty()) {
                    KeyValueRow("等待有效表达式", "", copyable = false)
                } else {
                    nextRuns.forEachIndexed { index, t ->
                        if (index > 0) RowDivider()
                        KeyValueRow("第 ${index + 1} 次", t.format(cronFmt))
                    }
                }
            }
        }
    }
}
