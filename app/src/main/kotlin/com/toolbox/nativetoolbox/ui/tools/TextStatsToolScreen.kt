package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold

@Composable
fun TextStatsToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }

    val chars = input.length
    val charsNoSpace = input.count { !it.isWhitespace() }
    val cjk = input.count { it.code in 0x4E00..0x9FFF }
    val words = Regex("[A-Za-z]+(?:'[A-Za-z]+)?").findAll(input).count()
    val digits = Regex("\\d+").findAll(input).count()
    val lines = if (input.isEmpty()) 0 else input.lines().size
    val paragraphs = input.split(Regex("\\n\\s*\\n")).count { it.isNotBlank() }
    val punctuation = input.count { !it.isLetterOrDigit() && !it.isWhitespace() }

    ToolScaffold {
        item { SectionHeader("文本") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "输入或粘贴文本,实时统计…",
                        minHeight = 180.dp
                    )
                }
            }
        }
        item { SectionHeader("统计") }
        item {
            GroupedCard {
                CardPadding {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCell("总字符", chars.toString(), Modifier.weight(1f))
                        StatCell("不含空白", charsNoSpace.toString(), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCell("汉字", cjk.toString(), Modifier.weight(1f))
                        StatCell("英文单词", words.toString(), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCell("行数", lines.toString(), Modifier.weight(1f))
                        StatCell("段落", paragraphs.toString(), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCell("数字串", digits.toString(), Modifier.weight(1f))
                        StatCell("标点符号", punctuation.toString(), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
