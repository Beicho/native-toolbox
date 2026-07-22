package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.theme.MonoStyle
import java.util.regex.Pattern

@Composable
fun RegexToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var pattern by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    var ignoreCase by rememberSaveable { mutableStateOf(false) }
    var multiline by rememberSaveable { mutableStateOf(false) }
    var dotAll by rememberSaveable { mutableStateOf(false) }

    var error: String? = null
    val matches: List<Pair<String, List<String>>> = if (pattern.isEmpty() || text.isEmpty()) {
        emptyList()
    } else {
        try {
            var flags = 0
            if (ignoreCase) flags = flags or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
            if (multiline) flags = flags or Pattern.MULTILINE
            if (dotAll) flags = flags or Pattern.DOTALL
            val compiled = Pattern.compile(pattern, flags)
            val matcher = compiled.matcher(text)
            buildList {
                var guard = 0
                while (matcher.find() && guard < 200) {
                    if (matcher.end() == matcher.start() && matcher.end() >= text.length) break
                    val groups = (1..matcher.groupCount()).map { g -> matcher.group(g) ?: "" }
                    add(matcher.group() to groups)
                    guard++
                    if (matcher.end() == matcher.start()) {
                        if (!matcher.find(matcher.end() + 1)) break
                    }
                }
            }
        } catch (e: Exception) {
            error = e.message
            emptyList()
        }
    }

    ToolScaffold(title = "正则测试", onBack = onBack) {
        item { SectionHeader("表达式") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        placeholder = "如 (\\d{4})-(\\d{2})",
                        mono = true
                    )
                }
                ToggleRow("忽略大小写", ignoreCase, { ignoreCase = it })
                ToggleRow("多行模式 ^$", multiline, { multiline = it })
                ToggleRow(". 匹配换行", dotAll, { dotAll = it })
            }
        }
        item { SectionHeader("测试文本") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = "粘贴要匹配的文本…",
                        minHeight = 120.dp
                    )
                }
            }
        }
        item { SectionHeader(if (error != null) "错误" else "匹配结果 · ${matches.size} 处") }
        item {
            GroupedCard {
                if (error != null) {
                    Text(
                        error ?: "",
                        Modifier.padding(16.dp),
                        style = MonoStyle,
                        color = palette.red
                    )
                } else if (matches.isEmpty()) {
                    Text(
                        "暂无匹配",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.tertiaryLabel
                    )
                } else {
                    matches.take(50).forEachIndexed { index, (whole, groups) ->
                        if (index > 0) RowDivider()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("#${index + 1}  $whole", style = MonoStyle, color = palette.label)
                            groups.forEachIndexed { g, value ->
                                Text(
                                    "  组${g + 1}: $value",
                                    style = MonoStyle,
                                    color = palette.secondaryLabel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
