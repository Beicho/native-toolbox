package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.net.AstroApi
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val targets = listOf("英文" to "en", "中文" to "zh", "日文" to "ja", "韩文" to "ko")

private fun formatCachedAt(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

@Composable
fun TranslateToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var input by rememberSaveable { mutableStateOf("") }
    var targetIndex by rememberSaveable { mutableStateOf(0) }
    var modeIndex by rememberSaveable { mutableStateOf(0) }
    var output by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }

    fun translate() {
        val text = input.trim()
        if (text.isEmpty()) {
            status = "先输入要翻译的内容"
            return
        }
        loading = true
        status = ""
        output = ""
        scope.launch {
            val body = JSONObject()
                .put("mode", if (modeIndex == 0) "text" else "word")
                .put("text", text)
                .put("to", targets[targetIndex].second)
            AstroApi.post("/translate", body)
                .onSuccess { res ->
                    output = res.data.optString("result").ifBlank {
                        res.data.optString("translation")
                    }
                    status = if (res.cachedAt > 0) "网络不通，这是 ${formatCachedAt(res.cachedAt)} 的缓存结果" else ""
                    if (output.isBlank()) status = "没拿到翻译结果，稍后再试"
                }
                .onFailure { e -> status = e.message ?: "翻译失败，请检查网络" }
            loading = false
        }
    }

    ToolScaffold {
        item { SectionHeader("原文") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "输入要翻译的文字或单词"
                    )
                    SegmentedPicker(
                        options = listOf("整段翻译", "查单词"),
                        selectedIndex = modeIndex,
                        onSelected = { modeIndex = it }
                    )
                    SegmentedPicker(
                        options = targets.map { it.first },
                        selectedIndex = targetIndex,
                        onSelected = { targetIndex = it }
                    )
                    SolidButton(onClick = { translate() }, enabled = !loading) {
                        Text(if (loading) "翻译中…" else "开始翻译")
                    }
                    Text(
                        "这个功能需要联网。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("译文") }
        item {
            GroupedCard {
                CardPadding {
                    if (status.isNotBlank()) {
                        Text(status, style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    }
                    if (output.isNotBlank()) {
                        OutputCard(text = output, label = "译文")
                    } else if (status.isBlank()) {
                        Text(
                            "翻译结果会显示在这里",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
    }
}
