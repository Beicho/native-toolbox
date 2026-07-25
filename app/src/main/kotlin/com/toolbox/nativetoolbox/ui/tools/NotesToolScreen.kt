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
import com.toolbox.nativetoolbox.ui.components.CheckRow
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/** 清单项在单字符串里的编码：完成标记 + 内容，行间用换行分隔 */
private const val DONE_MARK = "[x] "
private const val TODO_MARK = "[ ] "

private class Task(val text: String, val done: Boolean)

private fun parseTasks(raw: String): List<Task> = raw.lines().mapNotNull { line ->
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return@mapNotNull null
    when {
        trimmed.startsWith(DONE_MARK) -> Task(trimmed.removePrefix(DONE_MARK), true)
        trimmed.startsWith(TODO_MARK) -> Task(trimmed.removePrefix(TODO_MARK), false)
        else -> Task(trimmed, false)
    }
}

private fun serialize(tasks: List<Task>): String =
    tasks.joinToString("\n") { (if (it.done) DONE_MARK else TODO_MARK) + it.text }

@Composable
fun NotesToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var mode by rememberSaveable { mutableStateOf(0) }
    val toolPrefs = rememberToolPrefs("notes")
    var note by rememberPrefString(toolPrefs, "note", "")
    var listRaw by rememberPrefString(toolPrefs, "listRaw", "")
    var newTask by rememberSaveable { mutableStateOf("") }

    val tasks = parseTasks(listRaw)
    val doneCount = tasks.count { it.done }

    fun toggle(index: Int) {
        val updated = tasks.mapIndexed { i, t -> if (i == index) Task(t.text, !t.done) else t }
        listRaw = serialize(updated)
    }

    fun add() {
        val text = newTask.trim()
        if (text.isEmpty()) return
        listRaw = serialize(tasks + Task(text, false))
        newTask = ""
    }

    ToolScaffold {
        item { SectionHeader("类型") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("便签", "待办清单"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    Text(
                        "内容只留在这个页面，退出会清空。要长期保存请复制到系统备忘录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        if (mode == 0) {
            item { SectionHeader("便签") }
            item {
                GroupedCard {
                    CardPadding {
                        IosTextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = "随手记点东西",
                            singleLine = false,
                            minLines = 8
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("字符", note.length.toString(), Modifier.weight(1f))
                            StatCell(
                                "行数",
                                if (note.isEmpty()) "0" else note.lines().size.toString(),
                                Modifier.weight(1f)
                            )
                            StatCell(
                                "词数",
                                note.split(Regex("\\s+")).count { it.isNotBlank() }.toString(),
                                Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SolidButton(
                                onClick = { note = "" },
                                modifier = Modifier.weight(1f),
                                filled = false,
                                enabled = note.isNotBlank()
                            ) { Text("清空") }
                        }
                    }
                }
            }
            if (note.isNotBlank()) {
                item { SectionHeader("复制") }
                item { GroupedCard { CardPadding { OutputCard(text = note, label = "便签内容") } } }
            }
        } else {
            item { SectionHeader("进度") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("总数", tasks.size.toString(), Modifier.weight(1f))
                            StatCell("已完成", doneCount.toString(), Modifier.weight(1f))
                            StatCell(
                                "完成度",
                                if (tasks.isEmpty()) "—" else String.format("%.0f%%", doneCount * 100.0 / tasks.size),
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            item { SectionHeader("添加") }
            item {
                GroupedCard {
                    CardPadding {
                        IosTextField(
                            value = newTask,
                            onValueChange = { newTask = it },
                            placeholder = "要做什么"
                        )
                        SolidButton(onClick = { add() }, enabled = newTask.isNotBlank()) { Text("加一条") }
                    }
                }
            }
            if (tasks.isNotEmpty()) {
                item { SectionHeader("清单（点一下打勾）") }
                item {
                    GroupedCard {
                        tasks.forEachIndexed { index, task ->
                            CheckRow(task.text, task.done) { toggle(index) }
                        }
                    }
                }
                item {
                    GroupedCard {
                        CardPadding {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SolidButton(
                                    onClick = { listRaw = serialize(tasks.filter { !it.done }) },
                                    modifier = Modifier.weight(1f),
                                    filled = false,
                                    enabled = doneCount > 0
                                ) { Text("清掉已完成") }
                                SolidButton(
                                    onClick = { listRaw = "" },
                                    modifier = Modifier.weight(1f),
                                    filled = false
                                ) { Text("全部清空") }
                            }
                        }
                    }
                }
                item { SectionHeader("导出") }
                item {
                    GroupedCard {
                        CardPadding {
                            OutputCard(
                                text = tasks.joinToString("\n") { (if (it.done) "✓ " else "○ ") + it.text },
                                label = "清单"
                            )
                        }
                    }
                }
            }
        }
    }
}
