package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

@Composable
fun JsonToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var input by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(0) }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun run() {
        error = null
        output = ""
        if (input.isBlank()) return
        try {
            output = when (mode) {
                0 -> when (val parsed = JSONTokener(input).nextValue()) {
                    is JSONObject -> parsed.toString(4)
                    is JSONArray -> parsed.toString(4)
                    else -> parsed.toString()
                }
                1 -> when (val parsed = JSONTokener(input).nextValue()) {
                    is JSONObject -> parsed.toString()
                    is JSONArray -> parsed.toString()
                    else -> parsed.toString()
                }
                2 -> JSONObject.quote(input)
                else -> {
                    val trimmed = input.trim()
                    val quoted = if (trimmed.startsWith("\"")) trimmed else "\"$trimmed\""
                    JSONTokener(quoted).nextValue().toString()
                }
            }
        } catch (e: Exception) {
            error = "解析失败:${e.message}"
        }
    }

    ToolScaffold(title = "JSON 工具", onBack = onBack) {
        item { SectionHeader("输入") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it; run() },
                        placeholder = "粘贴 JSON 文本…",
                        minHeight = 160.dp,
                        mono = true
                    )
                    SegmentedPicker(
                        options = listOf("格式化", "压缩", "转义", "去转义"),
                        selectedIndex = mode,
                        onSelected = { mode = it; run() }
                    )
                }
            }
        }
        item { SectionHeader("输出") }
        item {
            GroupedCard {
                CardPadding {
                    if (error != null) {
                        Text(
                            error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    }
                    OutputCard(text = output, label = "结果")
                }
            }
        }
    }
}
