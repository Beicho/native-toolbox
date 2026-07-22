package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.Text
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
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ShareBus
import java.util.Base64

@Composable
fun Base64ToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var input by rememberSaveable { mutableStateOf(ShareBus.consume() ?: "") }
    var mode by rememberSaveable { mutableStateOf(0) }
    var urlSafe by rememberSaveable { mutableStateOf(false) }

    val result: Result<String> = runCatching {
        if (input.isEmpty()) ""
        else if (mode == 0) {
            val encoder = if (urlSafe) Base64.getUrlEncoder() else Base64.getEncoder()
            encoder.encodeToString(input.toByteArray(Charsets.UTF_8))
        } else {
            val decoder = if (urlSafe) Base64.getUrlDecoder() else Base64.getDecoder()
            String(decoder.decode(input.trim()), Charsets.UTF_8)
        }
    }

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
                        placeholder = if (mode == 0) "输入要编码的文本…" else "粘贴 Base64…",
                        minHeight = 140.dp,
                        mono = mode == 1
                    )
                }
                ToggleRow("URL Safe", urlSafe, { urlSafe = it }, subtitle = "使用 - _ 代替 + /")
            }
        }
        item { SectionHeader("输出") }
        item {
            GroupedCard {
                CardPadding {
                    if (result.isFailure) {
                        Text(
                            "解码失败:不是合法的 Base64",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    }
                    OutputCard(text = result.getOrNull() ?: "", label = if (mode == 0) "Base64" else "明文")
                }
            }
        }
    }
}
