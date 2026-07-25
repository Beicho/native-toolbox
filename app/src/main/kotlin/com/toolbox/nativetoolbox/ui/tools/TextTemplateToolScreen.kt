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
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.rememberCopy
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private const val SHELF_SEP = "\u0002"

/** 变量占位符：{{name}}，同名只需填一次 */
private val placeholderRe = Regex("\\{\\{\\s*([^}\\s]+)\\s*}}")

@Composable
fun TextTemplateToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val copy = rememberCopy()

    val toolPrefs = rememberToolPrefs("texttemplate")
    var template by rememberPrefString(toolPrefs, "template", "")
    var valuesRaw by rememberPrefString(toolPrefs, "valuesRaw", "")
    var savedTemplates by rememberPrefString(toolPrefs, "savedTemplates", "")
    var templateName by rememberSaveable { mutableStateOf("") }

    val placeholders = placeholderRe.findAll(template).map { it.groupValues[1] }.distinct().toList()

    val values = valuesRaw.lines().mapNotNull { line ->
        val idx = line.indexOfFirst { it == '=' || it == '：' || it == ':' }
        if (idx <= 0) null else line.take(idx).trim() to line.substring(idx + 1).trim()
    }.toMap()

    val filled = placeholderRe.replace(template) { m ->
        values[m.groupValues[1]] ?: m.value
    }
    val missing = placeholders.filter { values[it].isNullOrBlank() }
    val saved = savedTemplates.split(SHELF_SEP).filter { it.isNotBlank() }

    ToolScaffold {
        item { SectionHeader("模板") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = template,
                        onValueChange = { template = it },
                        placeholder = "用 {{名字}} 这样写变量，例如：\n您好 {{客户}}，您的订单 {{单号}} 已发出。",
                        minHeight = 120.dp
                    )
                    Text(
                        if (placeholders.isEmpty()) "还没有变量。用两个大括号包住变量名即可。"
                        else "识别到 " + placeholders.size + " 个变量：" + placeholders.joinToString("、"),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        if (placeholders.isNotEmpty()) {
            item { SectionHeader("填值") }
            item {
                GroupedCard {
                    CardPadding {
                        IosTextArea(
                            value = valuesRaw,
                            onValueChange = { valuesRaw = it },
                            placeholder = placeholders.joinToString("\n") { it + "=" },
                            minHeight = 110.dp,
                            mono = true
                        )
                        SolidButton(
                            onClick = { valuesRaw = placeholders.joinToString("\n") { it + "=" + (values[it] ?: "") } },
                            filled = false
                        ) { Text("按变量生成填空行") }
                        if (missing.isNotEmpty()) {
                            Text(
                                "还有 " + missing.size + " 个没填：" + missing.joinToString("、"),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.orange
                            )
                        }
                    }
                }
            }
            item { SectionHeader("变量对照") }
            item {
                GroupedCard {
                    placeholders.forEachIndexed { index, name ->
                        KeyValueRow(name, values[name]?.ifBlank { "（未填）" } ?: "（未填）", copyable = false)
                        if (index != placeholders.lastIndex) RowDivider()
                    }
                }
            }
        }
        item { SectionHeader("生成结果") }
        item {
            GroupedCard {
                CardPadding {
                    if (template.isBlank()) {
                        Text(
                            "先写一个模板",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    } else {
                        OutputCard(text = filled)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("字符", filled.length.toString(), Modifier.weight(1f))
                            StatCell("变量", placeholders.size.toString(), Modifier.weight(1f))
                            StatCell("未填", missing.size.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item { SectionHeader("模板暂存") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        placeholder = "给这个模板起个名"
                    )
                    SolidButton(
                        onClick = {
                            if (templateName.isNotBlank() && template.isNotBlank()) {
                                val entry = templateName.trim() + "\n" + template
                                savedTemplates = (listOf(entry) + saved).take(20).joinToString(SHELF_SEP)
                                templateName = ""
                            }
                        },
                        enabled = templateName.isNotBlank() && template.isNotBlank()
                    ) { Text("存起来") }
                    Text(
                        "只存在当前页面，退出会清空。要长期保存请复制到备忘录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        if (saved.isNotEmpty()) {
            item { SectionHeader("已存模板（" + saved.size + "）") }
            item {
                GroupedCard {
                    saved.forEachIndexed { index, entry ->
                        val name = entry.lineSequence().firstOrNull() ?: "未命名"
                        val body = entry.substringAfter('\n', "")
                        com.toolbox.nativetoolbox.ui.components.NavRow(
                            title = name,
                            value = body.take(20).replace("\n", " "),
                            onClick = { template = body }
                        )
                        if (index != saved.lastIndex) RowDivider()
                    }
                }
            }
            item {
                GroupedCard {
                    CardPadding {
                        SolidButton(onClick = { savedTemplates = "" }, filled = false) { Text("清空暂存") }
                    }
                }
            }
        }
    }
}

@Composable
fun ClipboardShelfToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val copy = rememberCopy()

    val toolPrefs = rememberToolPrefs("clipshelf")
    var shelf by rememberPrefString(toolPrefs, "shelf", "")
    var input by rememberSaveable { mutableStateOf("") }

    val items = shelf.split(SHELF_SEP).filter { it.isNotBlank() }

    fun add(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        shelf = (listOf(t) + items.filter { it != t }).take(30).joinToString(SHELF_SEP)
        input = ""
    }

    ToolScaffold {
        item { SectionHeader("放上架") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "粘贴要暂存的内容，可以是一段话、一串号码、一个链接",
                        minHeight = 100.dp
                    )
                    SolidButton(onClick = { add(input) }, enabled = input.isNotBlank()) {
                        Text("放进暂存架")
                    }
                    Text(
                        "手机剪贴板一次只能存一条，这里可以同时放 30 条，随时取用。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("统计") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("条数", items.size.toString(), Modifier.weight(1f))
                        StatCell(
                            "总字符",
                            items.sumOf { it.length }.toString(),
                            Modifier.weight(1f)
                        )
                        StatCell("上限", "30", Modifier.weight(1f))
                    }
                }
            }
        }
        if (items.isNotEmpty()) {
            item { SectionHeader("暂存架（点一条复制）") }
            item {
                GroupedCard {
                    items.forEachIndexed { index, text ->
                        KeyValueRow(
                            (index + 1).toString() + "　" + (if (text.length > 12) text.take(12) + "…" else text),
                            text
                        )
                        if (index != items.lastIndex) RowDivider()
                    }
                }
            }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SolidButton(
                                onClick = { copy(items.joinToString("\n")) },
                                modifier = Modifier.weight(1f),
                                filled = false
                            ) { Text("全部复制") }
                            SolidButton(
                                onClick = { shelf = "" },
                                modifier = Modifier.weight(1f),
                                filled = false
                            ) { Text("清空") }
                        }
                    }
                }
            }
        } else {
            item {
                GroupedCard {
                    CardPadding {
                        Text(
                            "架子是空的。放几条上来吧。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    }
                }
            }
        }
    }
}
