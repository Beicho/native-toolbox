package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 中英文之间补空格（盘古之白） */
private fun spaceCjkLatin(text: String): String {
    var out = Regex("([\\u4e00-\\u9fa5\\u3040-\\u30ff])([A-Za-z0-9@#$%^&*])").replace(text) { "${it.groupValues[1]} ${it.groupValues[2]}" }
    out = Regex("([A-Za-z0-9!~%^&*;:,.?)\\]}])([\\u4e00-\\u9fa5\\u3040-\\u30ff])").replace(out) { "${it.groupValues[1]} ${it.groupValues[2]}" }
    return out
}

/** 半角标点换成中文全角（只在中文语境） */
private fun normalizePunctuation(text: String): String {
    var out = text
    val pairs = listOf("," to "，", ";" to "；", ":" to "：", "!" to "！", "?" to "？")
    pairs.forEach { (half, full) ->
        out = Regex("([\\u4e00-\\u9fa5])\\Q$half\\E").replace(out) { "${it.groupValues[1]}$full" }
    }
    out = Regex("([\\u4e00-\\u9fa5])\\.(?![0-9])").replace(out) { "${it.groupValues[1]}。" }
    return out
}

private fun squeezeBlankLines(text: String): String =
    text.replace(Regex("\n{3,}"), "\n\n").lines().joinToString("\n") { it.trimEnd() }

private fun stripIndent(text: String): String =
    text.lines().joinToString("\n") { it.trimStart('　', ' ', '\t') }

@Composable
fun TextFormatToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var addSpace by rememberSaveable { mutableStateOf(true) }
    var fixPunct by rememberSaveable { mutableStateOf(true) }
    var squeeze by rememberSaveable { mutableStateOf(true) }
    var unindent by rememberSaveable { mutableStateOf(false) }

    var out = input
    if (unindent) out = stripIndent(out)
    if (addSpace) out = spaceCjkLatin(out)
    if (fixPunct) out = normalizePunctuation(out)
    if (squeeze) out = squeezeBlankLines(out)

    ToolScaffold {
        item { SectionHeader("原文") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "粘贴一段中英混排的文字"
                    )
                }
            }
        }
        item { SectionHeader("排版规则") }
        item {
            GroupedCard {
                ToggleRow("中英之间加空格", addSpace, onCheckedChange = { addSpace = it })
                ToggleRow("半角标点转中文", fixPunct, onCheckedChange = { fixPunct = it })
                ToggleRow("压缩多余空行", squeeze, onCheckedChange = { squeeze = it })
                ToggleRow("去掉每行开头缩进", unindent, onCheckedChange = { unindent = it })
            }
        }
        item { SectionHeader("整理后") }
        item {
            GroupedCard {
                CardPadding {
                    OutputCard(text = out)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("字符", out.length.toString(), Modifier.weight(1f))
                        StatCell("行数", if (out.isEmpty()) "0" else out.lines().size.toString(), Modifier.weight(1f))
                        StatCell(
                            "改动",
                            (out.length - input.length).let { if (it > 0) "+$it" else it.toString() },
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
