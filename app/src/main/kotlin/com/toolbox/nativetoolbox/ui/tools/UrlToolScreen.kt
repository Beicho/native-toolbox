package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.util.ShareBus
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun UrlToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf(ShareBus.consume() ?: "") }
    var mode by rememberSaveable { mutableStateOf(0) }

    val output = runCatching {
        if (input.isEmpty()) ""
        else if (mode == 0) URLEncoder.encode(input, "UTF-8").replace("+", "%20")
        else URLDecoder.decode(input, "UTF-8")
    }.getOrElse { "解码失败:${it.message}" }

    ToolScaffold {
        item { SectionHeader("输入") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("编码", "解码"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = if (mode == 0) "输入文本或 URL…" else "粘贴百分号编码文本…",
                        minHeight = 140.dp,
                        mono = mode == 1
                    )
                }
            }
        }
        item { SectionHeader("输出") }
        item {
            GroupedCard {
                CardPadding {
                    OutputCard(text = output, label = if (mode == 0) "编码结果" else "解码结果")
                }
            }
        }
    }
}
