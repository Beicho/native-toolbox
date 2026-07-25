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

private class RenameRule(val name: String, val hint: String)

private val modes = listOf(
    RenameRule("替换文字", "把文件名里的某段文字换成另一段"),
    RenameRule("加前后缀", "在文件名前面或后面统一加内容"),
    RenameRule("顺序编号", "按顺序重命名成 名称001、名称002"),
    RenameRule("大小写", "统一改成大写、小写或首字母大写")
)

private fun splitName(fileName: String): Pair<String, String> {
    val dot = fileName.lastIndexOf('.')
    return if (dot <= 0) fileName to "" else fileName.take(dot) to fileName.substring(dot)
}

private fun applyRename(
    names: List<String>,
    mode: Int,
    from: String,
    to: String,
    prefix: String,
    suffix: String,
    baseName: String,
    startNumber: Int,
    digits: Int,
    caseMode: Int,
    keepExtension: Boolean
): List<Pair<String, String>> = names.mapIndexed { index, original ->
    val (stem, ext) = splitName(original)
    val extension = if (keepExtension) ext else ""
    val newStem = when (mode) {
        0 -> if (from.isEmpty()) stem else stem.replace(from, to)
        1 -> prefix + stem + suffix
        2 -> baseName + (startNumber + index).toString().padStart(digits, '0')
        else -> when (caseMode) {
            0 -> stem.uppercase()
            1 -> stem.lowercase()
            else -> stem.split(' ').joinToString(" ") { word ->
                if (word.isEmpty()) word else word.first().uppercase() + word.drop(1).lowercase()
            }
        }
    }
    original to (newStem + extension)
}

@Composable
fun BatchRenameToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var raw by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(0) }
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var prefix by rememberSaveable { mutableStateOf("") }
    var suffix by rememberSaveable { mutableStateOf("") }
    var baseName by rememberSaveable { mutableStateOf("照片") }
    var startText by rememberSaveable { mutableStateOf("1") }
    var digitsText by rememberSaveable { mutableStateOf("3") }
    var caseMode by rememberSaveable { mutableStateOf(1) }
    var keepExtension by rememberSaveable { mutableStateOf(true) }

    val names = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val startNumber = startText.trim().toIntOrNull() ?: 1
    val digits = (digitsText.trim().toIntOrNull() ?: 3).coerceIn(1, 8)

    val pairs = applyRename(
        names, mode, from, to, prefix, suffix, baseName, startNumber, digits, caseMode, keepExtension
    )
    val changed = pairs.count { it.first != it.second }
    val duplicates = pairs.map { it.second }.groupingBy { it }.eachCount().count { it.value > 1 }

    ToolScaffold {
        item { SectionHeader("文件名清单") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = raw,
                        onValueChange = { raw = it },
                        placeholder = "一行一个文件名，例如：\nIMG_0001.jpg\nIMG_0002.jpg",
                        minHeight = 120.dp,
                        mono = true
                    )
                    Text(
                        if (names.isEmpty()) "把文件名粘进来（可以从文件管理器复制）"
                        else "共 " + names.size + " 个文件名",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader("重命名方式") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = modes.map { it.name },
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    Text(
                        modes[mode].hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                    when (mode) {
                        0 -> {
                            IosTextField(value = from, onValueChange = { from = it }, placeholder = "查找")
                            IosTextField(value = to, onValueChange = { to = it }, placeholder = "替换成（留空表示删掉）")
                        }
                        1 -> {
                            IosTextField(value = prefix, onValueChange = { prefix = it }, placeholder = "前缀")
                            IosTextField(value = suffix, onValueChange = { suffix = it }, placeholder = "后缀")
                        }
                        2 -> {
                            IosTextField(value = baseName, onValueChange = { baseName = it }, placeholder = "统一名称")
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IosTextField(
                                    value = startText,
                                    onValueChange = { startText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = "起始编号",
                                    mono = true
                                )
                                IosTextField(
                                    value = digitsText,
                                    onValueChange = { digitsText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = "补几位",
                                    mono = true
                                )
                            }
                        }
                        else -> {
                            SegmentedPicker(
                                options = listOf("全大写", "全小写", "首字母大写"),
                                selectedIndex = caseMode,
                                onSelected = { caseMode = it }
                            )
                        }
                    }
                }
                ToggleRow(
                    "保留原扩展名",
                    keepExtension,
                    onCheckedChange = { keepExtension = it },
                    subtitle = "关掉会去掉 .jpg 这类后缀，一般别关"
                )
            }
        }
        if (names.isNotEmpty()) {
            item { SectionHeader("统计") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("总数", names.size.toString(), Modifier.weight(1f))
                            StatCell("会改动", changed.toString(), Modifier.weight(1f))
                            StatCell("重名", duplicates.toString(), Modifier.weight(1f))
                        }
                        if (duplicates > 0) {
                            Text(
                                "有 " + duplicates + " 组结果重名了，这样改会互相覆盖，调整一下规则。",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.red
                            )
                        }
                    }
                }
            }
            item { SectionHeader("对照预览") }
            item {
                GroupedCard {
                    pairs.take(50).forEachIndexed { index, (old, new) ->
                        KeyValueRow(old, new, copyable = false)
                        if (index != pairs.take(50).lastIndex) RowDivider()
                    }
                }
            }
            item { SectionHeader("复制新文件名") }
            item {
                GroupedCard {
                    CardPadding {
                        OutputCard(text = pairs.joinToString("\n") { it.second }, label = "新名单")
                        Text(
                            "这个工具只生成名单，不会动你手机里的文件。复制后去文件管理器或电脑上批量改。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
    }
}
