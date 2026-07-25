package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.theme.MonoStyle

/** 行内标记解析：**粗** *斜* `代码` ~~删除~~ [文字](链接) */
@Composable
private fun inlineMarkdown(text: String): AnnotatedString {
    val palette = LocalIosPalette.current
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end < 0) { append(text.substring(i)); i = text.length }
                    else {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                        i = end + 2
                    }
                }
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end < 0) { append(text.substring(i)); i = text.length }
                    else {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = palette.tertiaryLabel)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end < 0) { append(text.substring(i)); i = text.length }
                    else {
                        withStyle(SpanStyle(fontFamily = MonoStyle.fontFamily, color = palette.pink)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    }
                }
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end < 0) { append(text.substring(i)); i = text.length }
                    else {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                        i = end + 1
                    }
                }
                text[i] == '[' -> {
                    val close = text.indexOf(']', i)
                    val open = if (close >= 0) text.indexOf('(', close) else -1
                    val end = if (open >= 0) text.indexOf(')', open) else -1
                    if (close < 0 || open != close + 1 || end < 0) { append(text[i]); i++ }
                    else {
                        withStyle(SpanStyle(color = palette.accent, textDecoration = TextDecoration.Underline)) {
                            append(text.substring(i + 1, close))
                        }
                        i = end + 1
                    }
                }
                else -> { append(text[i]); i++ }
            }
        }
    }
}

@Composable
private fun MarkdownRender(source: String) {
    val palette = LocalIosPalette.current
    var inCodeBlock = false
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        source.lines().forEach { rawLine ->
            val line = rawLine
            if (line.trimStart().startsWith("```")) {
                inCodeBlock = !inCodeBlock
                return@forEach
            }
            if (inCodeBlock) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(palette.sunkenBackground)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(line, style = MonoStyle.copy(color = palette.label, fontSize = 12.sp))
                }
                return@forEach
            }
            val trimmed = line.trimStart()
            when {
                trimmed.isEmpty() -> Box(Modifier.size(1.dp, 6.dp))
                trimmed.startsWith("###### ") -> Text(inlineMarkdown(trimmed.removePrefix("###### ")), style = MaterialTheme.typography.titleSmall, color = palette.label)
                trimmed.startsWith("##### ") -> Text(inlineMarkdown(trimmed.removePrefix("##### ")), style = MaterialTheme.typography.titleSmall, color = palette.label)
                trimmed.startsWith("#### ") -> Text(inlineMarkdown(trimmed.removePrefix("#### ")), style = MaterialTheme.typography.titleMedium, color = palette.label)
                trimmed.startsWith("### ") -> Text(inlineMarkdown(trimmed.removePrefix("### ")), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.label)
                trimmed.startsWith("## ") -> Text(inlineMarkdown(trimmed.removePrefix("## ")), style = MaterialTheme.typography.titleLarge, color = palette.label)
                trimmed.startsWith("# ") -> Text(inlineMarkdown(trimmed.removePrefix("# ")), style = MaterialTheme.typography.headlineSmall, color = palette.label)
                trimmed.startsWith("> ") -> Row {
                    Box(Modifier.size(3.dp, 20.dp).background(palette.accent))
                    Text(
                        inlineMarkdown(trimmed.removePrefix("> ")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryLabel,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                trimmed.startsWith("- [ ] ") -> Text(inlineMarkdown("○ " + trimmed.removePrefix("- [ ] ")), style = MaterialTheme.typography.bodyMedium, color = palette.label)
                trimmed.startsWith("- [x] ") -> Text(inlineMarkdown("✓ " + trimmed.removePrefix("- [x] ")), style = MaterialTheme.typography.bodyMedium, color = palette.green)
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> Text(
                    inlineMarkdown("・" + trimmed.drop(2)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.label,
                    modifier = Modifier.padding(start = (line.length - trimmed.length).coerceAtMost(8).dp)
                )
                Regex("^\\d+\\. ").containsMatchIn(trimmed) -> Text(inlineMarkdown(trimmed), style = MaterialTheme.typography.bodyMedium, color = palette.label)
                trimmed.startsWith("---") || trimmed.startsWith("***") -> Box(
                    Modifier.fillMaxWidth().size(1.dp).background(palette.separator)
                )
                else -> Text(inlineMarkdown(line), style = MaterialTheme.typography.bodyMedium, color = palette.label)
            }
        }
    }
}

private val sample = """
# 标题一
这是一段普通文字，包含**加粗**、*斜体*、`行内代码`和~~删除线~~。

## 列表
- 第一项
- 第二项
  - 嵌套项
- [x] 已完成
- [ ] 待办

> 这是一段引用。

```
代码块里的内容不会被解析
```

---
[链接文字](https://example.com)
""".trimIndent()

@Composable
fun MarkdownPreviewToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var source by rememberSaveable { mutableStateOf("") }
    var view by rememberSaveable { mutableStateOf(0) }

    val lines = source.lines()
    val headings = lines.count { it.trimStart().startsWith("#") }
    val codeBlocks = lines.count { it.trimStart().startsWith("```") } / 2
    val links = Regex("\\[[^]]*]\\([^)]*\\)").findAll(source).count()

    ToolScaffold {
        item { SectionHeader("视图") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("预览", "源码"),
                        selectedIndex = view,
                        onSelected = { view = it }
                    )
                    if (source.isBlank()) {
                        com.toolbox.nativetoolbox.ui.components.SolidButton(
                            onClick = { source = sample },
                            filled = false
                        ) { Text("填入示例看看效果") }
                    }
                }
            }
        }
        item { SectionHeader(if (view == 0) "预览" else "源码") }
        item {
            GroupedCard {
                CardPadding {
                    if (source.isBlank()) {
                        Text(
                            "在下面粘贴 Markdown，这里会实时渲染。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    } else if (view == 0) {
                        MarkdownRender(source)
                    } else {
                        Text(source, style = MonoStyle.copy(color = palette.label, fontSize = 12.sp))
                    }
                }
            }
        }
        item { SectionHeader("统计") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("字符", source.length.toString(), Modifier.weight(1f))
                        StatCell("行数", if (source.isEmpty()) "0" else lines.size.toString(), Modifier.weight(1f))
                        StatCell("标题", headings.toString(), Modifier.weight(1f))
                    }
                }
                KeyValueRow("代码块", codeBlocks.toString(), copyable = false)
                RowDivider()
                KeyValueRow("链接", links.toString(), copyable = false)
            }
        }
        item { SectionHeader("Markdown 源码") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = source,
                        onValueChange = { source = it },
                        placeholder = "粘贴 Markdown",
                        minHeight = 180.dp,
                        mono = true
                    )
                    Text(
                        "支持标题、粗斜体、行内代码、删除线、列表、任务清单、引用、代码块、分割线和链接。表格暂不支持。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
