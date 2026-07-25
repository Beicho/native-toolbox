package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.net.AstroApi
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.rememberCopy
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 收藏列表在单个字符串里的分隔符，用不可见控制字符避免和句子内容冲突 */
private const val SEPARATOR = "\u0001"

private val typeOptions = listOf(
    "随机" to "",
    "动画" to "a",
    "文学" to "d",
    "原创" to "e",
    "网络" to "f",
    "影视" to "h",
    "诗词" to "i",
    "哲学" to "k"
)

@Composable
fun HitokotoToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    val copy = rememberCopy()

    var typeIndex by rememberSaveable { mutableStateOf(0) }
    var sentence by rememberSaveable { mutableStateOf("") }
    var fromText by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var collected by rememberSaveable { mutableStateOf("") }

    fun load() {
        loading = true
        status = ""
        scope.launch {
            val params = typeOptions[typeIndex].second.let {
                if (it.isBlank()) emptyMap() else mapOf("c" to it)
            }
            AstroApi.get("/hitokoto", params)
                .onSuccess { res ->
                    val obj: JSONObject = res.data
                    sentence = obj.optString("hitokoto").ifBlank { obj.optString("text") }
                    fromText = obj.optString("from")
                    author = obj.optString("from_who").ifBlank { obj.optString("author") }
                    status = cachedHint(res.cachedAt)
                    if (sentence.isBlank()) status = "没拿到句子，再试一次"
                }
                .onFailure { e -> status = e.message ?: "获取失败，请检查网络" }
            loading = false
        }
    }

    LaunchedEffect(Unit) { if (sentence.isBlank()) load() }

    val collectedList = collected.split(SEPARATOR).filter { it.isNotBlank() }

    ToolScaffold {
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        sentence.ifBlank { if (loading) "取一句…" else "—" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.label,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val source = listOf(author, fromText).filter { it.isNotBlank() }.joinToString("《") +
                        if (fromText.isNotBlank()) "》" else ""
                    if (source.isNotBlank()) {
                        Text(
                            "—— " + source,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryLabel,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = { load() },
                            modifier = Modifier.weight(1f),
                            enabled = !loading
                        ) { Text(if (loading) "取句中…" else "换一句") }
                        SolidButton(
                            onClick = { if (sentence.isNotBlank()) copy(sentence) },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = sentence.isNotBlank()
                        ) { Text("复制") }
                        SolidButton(
                            onClick = {
                                if (sentence.isNotBlank() && !collectedList.contains(sentence)) {
                                    collected = (listOf(sentence) + collectedList).take(30).joinToString(SEPARATOR)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = sentence.isNotBlank()
                        ) { Text("收藏") }
                    }
                    if (status.isNotBlank()) {
                        Text(status, style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    }
                    Text(
                        "这个功能需要联网。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("句子类型") }
        item {
            GroupedCard {
                CardPadding {
                    typeOptions.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { option ->
                                val index = typeOptions.indexOf(option)
                                SolidButton(
                                    onClick = {
                                        typeIndex = index
                                        load()
                                    },
                                    modifier = Modifier.weight(1f),
                                    filled = typeIndex == index,
                                    height = 38.dp
                                ) { Text(option.first, style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                }
            }
        }
        if (collectedList.isNotEmpty()) {
            item { SectionHeader("收藏（" + collectedList.size + "）") }
            item {
                GroupedCard {
                    collectedList.forEachIndexed { index, text ->
                        KeyValueRow((index + 1).toString(), text)
                        if (index != collectedList.lastIndex) RowDivider()
                    }
                }
            }
            item {
                GroupedCard {
                    CardPadding {
                        SolidButton(onClick = { collected = "" }, filled = false) { Text("清空收藏") }
                        Text(
                            "收藏只留在当前页面，退出会清空。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
    }
}
