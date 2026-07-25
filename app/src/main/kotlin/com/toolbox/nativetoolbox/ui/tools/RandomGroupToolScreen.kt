package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.random.Random

private fun parseNames(raw: String): List<String> =
    raw.split(Regex("[\\n,，、;；\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }

/** 洗牌后按组数轮流发牌，保证各组人数最多差一个 */
private fun splitGroups(names: List<String>, groupCount: Int, seed: Int): List<List<String>> {
    if (names.isEmpty() || groupCount <= 0) return emptyList()
    val shuffled = names.shuffled(Random(seed))
    val groups = MutableList(groupCount) { ArrayList<String>() }
    shuffled.forEachIndexed { index, name -> groups[index % groupCount].add(name) }
    return groups.filter { it.isNotEmpty() }
}

@Composable
fun RandomGroupToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var raw by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(0) }
    var groupCountText by rememberSaveable { mutableStateOf("3") }
    var seed by rememberSaveable { mutableStateOf(1) }
    var picked by rememberSaveable { mutableStateOf("") }
    var noRepeat by rememberSaveable { mutableStateOf(true) }
    var pickedHistory by rememberSaveable { mutableStateOf("") }

    val names = parseNames(raw)
    val history = pickedHistory.split("|").filter { it.isNotBlank() }
    val remaining = if (noRepeat) names.filterNot { history.contains(it) } else names
    val groupCount = groupCountText.trim().toIntOrNull()?.coerceIn(1, 50) ?: 3
    val groups = if (mode == 0) splitGroups(names, groupCount, seed) else emptyList()

    val groupText = groups.mapIndexed { index, group ->
        "第 " + (index + 1) + " 组（" + group.size + " 人）\n" + group.joinToString("、")
    }.joinToString("\n\n")

    ToolScaffold {
        item { SectionHeader("名单") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = raw,
                        onValueChange = { raw = it },
                        placeholder = "一行一个名字，也可以用逗号、空格、顿号分隔"
                    )
                    Text(
                        if (names.isEmpty()) "还没有名字" else "共 " + names.size + " 人",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader("要做什么") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("随机分组", "随机点名"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    if (mode == 0) {
                        IosTextField(
                            value = groupCountText,
                            onValueChange = { groupCountText = it },
                            placeholder = "分几组",
                            mono = true
                        )
                        SolidButton(onClick = { seed += 1 }, enabled = names.isNotEmpty()) {
                            Text("重新分组")
                        }
                    } else {
                        SolidButton(
                            onClick = {
                                if (remaining.isNotEmpty()) {
                                    val choice = remaining.random()
                                    picked = choice
                                    if (noRepeat) {
                                        pickedHistory = (listOf(choice) + history).joinToString("|")
                                    }
                                }
                            },
                            enabled = remaining.isNotEmpty()
                        ) { Text(if (remaining.isEmpty()) "都点过了" else "点名") }
                    }
                }
                if (mode == 1) {
                    ToggleRow(
                        "点过的不再点",
                        noRepeat,
                        onCheckedChange = { noRepeat = it },
                        subtitle = "适合课堂轮流回答"
                    )
                }
            }
        }
        if (mode == 1) {
            item { SectionHeader("结果") }
            item {
                GroupedCard {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            picked.ifBlank { "—" },
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Light,
                            color = palette.accent
                        )
                        Text(
                            if (noRepeat) "已点 " + history.size + " 人，剩 " + remaining.size + " 人"
                            else "共 " + names.size + " 人可点",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                        if (history.isNotEmpty()) {
                            SolidButton(
                                onClick = {
                                    pickedHistory = ""
                                    picked = ""
                                },
                                filled = false,
                                height = 38.dp
                            ) { Text("重新开始") }
                        }
                    }
                }
            }
            if (history.isNotEmpty()) {
                item { SectionHeader("已点过") }
                item {
                    GroupedCard {
                        CardPadding {
                            Text(
                                history.joinToString("、"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryLabel
                            )
                        }
                    }
                }
            }
        } else {
            item { SectionHeader(if (groups.isEmpty()) "分组结果" else "分成 " + groups.size + " 组") }
            item {
                GroupedCard {
                    CardPadding {
                        if (groups.isEmpty()) {
                            Text(
                                "先填名单",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryLabel
                            )
                        } else {
                            OutputCard(text = groupText, label = "分组名单")
                        }
                    }
                }
            }
        }
    }
}
