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
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private val levelPatterns = listOf(
    "错误" to Regex("(?i)\\b(error|fatal|severe|exception|panic)\\b|\\bE/"),
    "警告" to Regex("(?i)\\b(warn|warning)\\b|\\bW/"),
    "信息" to Regex("(?i)\\b(info)\\b|\\bI/"),
    "调试" to Regex("(?i)\\b(debug|verbose|trace)\\b|\\b[DV]/")
)

private val ipPattern = Regex("(?<!\\d)((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(?!\\d)")
private val statusPattern = Regex("\"\\s(\\d{3})\\s|\\bHTTP/\\d\\.\\d\"?\\s(\\d{3})\\b|\\sstatus[=:]\\s?(\\d{3})\\b")
private val timePattern = Regex("(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2})")
private val stackHead = Regex("(?i)^\\s*(at\\s+\\S+|Caused by:.*|[A-Za-z.$]+(Exception|Error):.*)$")

@Composable
fun LogAnalyzeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var raw by rememberSaveable { mutableStateOf("") }
    var keyword by rememberSaveable { mutableStateOf("") }
    var levelFilter by rememberSaveable { mutableStateOf(0) }
    var dedupe by rememberSaveable { mutableStateOf(true) }

    val lines = raw.lines().filter { it.isNotBlank() }
    val counts = levelPatterns.map { (name, re) -> name to lines.count { re.containsMatchIn(it) } }

    val filtered = lines
        .filter { line ->
            levelFilter == 0 || levelPatterns[levelFilter - 1].second.containsMatchIn(line)
        }
        .filter { line ->
            keyword.isBlank() || line.contains(keyword.trim(), ignoreCase = true)
        }

    val shown = if (!dedupe) filtered else {
        // 去掉时间戳和数字后判重，把「同一条错误刷屏」折叠成一条
        val seen = LinkedHashMap<String, Int>()
        filtered.forEach { line ->
            val key = line.replace(timePattern, "").replace(Regex("\\d+"), "#").trim()
            seen[key] = (seen[key] ?: 0) + 1
        }
        seen.entries.map { (key, count) ->
            if (count > 1) "[出现 " + count + " 次] " + key else key
        }
    }

    val ips = lines.flatMap { ipPattern.findAll(it).map { m -> m.value }.toList() }
        .groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }.take(8)

    val statuses = lines.flatMap { line ->
        statusPattern.findAll(line).mapNotNull { m ->
            m.groupValues.drop(1).firstOrNull { it.isNotBlank() }
        }.toList()
    }.groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }.take(8)

    val stackLines = lines.filter { stackHead.containsMatchIn(it) }
    val firstTime = lines.firstNotNullOfOrNull { timePattern.find(it)?.value }
    val lastTime = lines.lastOrNull { timePattern.containsMatchIn(it) }?.let { timePattern.find(it)?.value }

    ToolScaffold {
        item { SectionHeader("粘贴日志") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = raw,
                        onValueChange = { raw = it },
                        placeholder = "粘贴 logcat、nginx access log、服务端日志都行",
                        minHeight = 140.dp,
                        mono = true
                    )
                    Text(
                        if (lines.isEmpty()) "全部在本地分析，不上传任何内容。"
                        else "共 " + lines.size + " 行",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        if (lines.isNotEmpty()) {
            item { SectionHeader("级别分布") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            counts.forEach { (name, count) ->
                                StatCell(name, count.toString(), Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item { SectionHeader("时间范围") }
            item {
                GroupedCard {
                    KeyValueRow("最早", firstTime ?: "没识别到时间戳", copyable = false)
                    RowDivider()
                    KeyValueRow("最晚", lastTime ?: "没识别到时间戳", copyable = false)
                    RowDivider()
                    KeyValueRow("堆栈相关行", stackLines.size.toString(), copyable = false)
                }
            }
            if (statuses.isNotEmpty()) {
                item { SectionHeader("HTTP 状态码") }
                item {
                    GroupedCard {
                        statuses.forEachIndexed { index, entry ->
                            KeyValueRow(entry.key, entry.value.toString() + " 次", copyable = false)
                            if (index != statuses.lastIndex) RowDivider()
                        }
                    }
                }
            }
            if (ips.isNotEmpty()) {
                item { SectionHeader("出现最多的 IP") }
                item {
                    GroupedCard {
                        ips.forEachIndexed { index, entry ->
                            KeyValueRow(entry.key, entry.value.toString() + " 次")
                            if (index != ips.lastIndex) RowDivider()
                        }
                    }
                }
            }
            item { SectionHeader("筛选") }
            item {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(
                            options = listOf("全部") + levelPatterns.map { it.first },
                            selectedIndex = levelFilter,
                            onSelected = { levelFilter = it }
                        )
                        IosTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            placeholder = "关键词过滤"
                        )
                    }
                    ToggleRow(
                        "折叠重复行",
                        dedupe,
                        onCheckedChange = { dedupe = it },
                        subtitle = "忽略时间和数字差异，把刷屏的同类日志合并"
                    )
                }
            }
            item { SectionHeader("结果（" + shown.size + " 条）") }
            item {
                GroupedCard {
                    CardPadding {
                        if (shown.isEmpty()) {
                            Text(
                                "没有匹配的行",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryLabel
                            )
                        } else {
                            OutputCard(text = shown.take(200).joinToString("\n"), label = "最多显示 200 条")
                        }
                    }
                }
            }
        }
    }
}
