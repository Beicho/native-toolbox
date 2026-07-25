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
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold

private val morseTable = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
    'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
    'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
    'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
    'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
    'Z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
    '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
    '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.",
    '!' to "-.-.--", '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-",
    '&' to ".-...", ':' to "---...", ';' to "-.-.-.", '=' to "-...-",
    '+' to ".-.-.", '-' to "-....-", '_' to "..--.-", '"' to ".-..-.",
    '$' to "...-..-", '@' to ".--.-."
)
private val reverseMorse = morseTable.entries.associate { (k, v) -> v to k }

private fun toMorse(text: String): String = text.trim().uppercase()
    .split(Regex("\\s+"))
    .filter { it.isNotEmpty() }
    .joinToString(" / ") { word ->
        word.mapNotNull { morseTable[it] }.joinToString(" ")
    }

private fun fromMorse(code: String): String = code.trim()
    .split(Regex("\\s*/\\s*|\\s{3,}"))
    .filter { it.isNotBlank() }
    .joinToString(" ") { word ->
        word.trim().split(Regex("\\s+")).mapNotNull { reverseMorse[it] }.joinToString("")
    }

@Composable
fun MorseToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(0) }

    val result = when {
        input.isBlank() -> ""
        mode == 0 -> toMorse(input)
        else -> fromMorse(input)
    }
    val hint = when {
        input.isBlank() -> "输入内容后自动转换"
        result.isBlank() && mode == 0 -> "这段文字里没有摩斯码支持的字符（支持英文、数字和常见标点）"
        result.isBlank() -> "没识别出摩斯码，点分隔用空格，单词之间用 / 或三个空格"
        else -> ""
    }

    ToolScaffold {
        item { SectionHeader("输入") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("文字转摩斯", "摩斯转文字"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = if (mode == 0) "输入英文或数字，例如 SOS" else "输入摩斯码，例如 ... --- ...",
                        mono = mode == 1
                    )
                }
            }
        }
        item { SectionHeader(if (hint.isNotBlank()) "说明" else "结果") }
        item {
            GroupedCard {
                CardPadding {
                    if (hint.isNotBlank()) {
                        androidx.compose.material3.Text(
                            hint,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = com.toolbox.nativetoolbox.ui.theme.LocalIosPalette.current.secondaryLabel
                        )
                    } else {
                        OutputCard(text = result, label = if (mode == 0) "摩斯码" else "文字")
                    }
                }
            }
        }
    }
}
