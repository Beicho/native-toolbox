package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.theme.MonoStyle

private class CodeTheme(
    val name: String,
    val background: Color,
    val plain: Color,
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val gutter: Color
)

private val themes = listOf(
    CodeTheme(
        "深色", Color(0xFF1E1E2E), Color(0xFFCDD6F4), Color(0xFFCBA6F7),
        Color(0xFFA6E3A1), Color(0xFF6C7086), Color(0xFFFAB387), Color(0xFF45475A)
    ),
    CodeTheme(
        "浅色", Color(0xFFFAFAFA), Color(0xFF24292E), Color(0xFFD73A49),
        Color(0xFF032F62), Color(0xFF6A737D), Color(0xFF005CC5), Color(0xFFBBBBBB)
    ),
    CodeTheme(
        "墨蓝", Color(0xFF0D1117), Color(0xFFC9D1D9), Color(0xFFFF7B72),
        Color(0xFFA5D6FF), Color(0xFF8B949E), Color(0xFF79C0FF), Color(0xFF30363D)
    )
)

private val keywords = setOf(
    "fun", "val", "var", "class", "object", "interface", "if", "else", "when", "for", "while",
    "return", "import", "package", "private", "public", "internal", "override", "suspend",
    "data", "sealed", "companion", "init", "try", "catch", "finally", "throw", "is", "as", "in",
    "function", "const", "let", "async", "await", "export", "default", "new", "this", "null",
    "true", "false", "def", "self", "elif", "lambda", "import", "from", "print",
    "public", "static", "void", "int", "String", "boolean", "struct", "type", "func", "go",
    "select", "insert", "update", "delete", "where", "join"
)

/** 极简高亮：注释 → 字符串 → 数字 → 关键字，够看就行，不做完整词法分析 */
private fun highlight(line: String, theme: CodeTheme): AnnotatedString = buildAnnotatedString {
    val commentIndex = listOf(line.indexOf("//"), line.indexOf('#'), line.indexOf("--"))
        .filter { it >= 0 }.minOrNull() ?: -1
    val codePart = if (commentIndex >= 0) line.take(commentIndex) else line
    val commentPart = if (commentIndex >= 0) line.substring(commentIndex) else ""

    var i = 0
    while (i < codePart.length) {
        val c = codePart[i]
        when {
            c == '"' || c == '\'' -> {
                val end = codePart.indexOf(c, i + 1)
                val stop = if (end < 0) codePart.length else end + 1
                withStyle(SpanStyle(color = theme.string)) { append(codePart.substring(i, stop)) }
                i = stop
            }
            c.isDigit() -> {
                var j = i
                while (j < codePart.length && (codePart[j].isDigit() || codePart[j] == '.')) j++
                withStyle(SpanStyle(color = theme.number)) { append(codePart.substring(i, j)) }
                i = j
            }
            c.isLetter() || c == '_' -> {
                var j = i
                while (j < codePart.length && (codePart[j].isLetterOrDigit() || codePart[j] == '_')) j++
                val word = codePart.substring(i, j)
                if (keywords.contains(word)) {
                    withStyle(SpanStyle(color = theme.keyword)) { append(word) }
                } else {
                    withStyle(SpanStyle(color = theme.plain)) { append(word) }
                }
                i = j
            }
            else -> {
                withStyle(SpanStyle(color = theme.plain)) { append(c) }
                i++
            }
        }
    }
    if (commentPart.isNotEmpty()) {
        withStyle(SpanStyle(color = theme.comment)) { append(commentPart) }
    }
}

@Composable
fun CodeScreenshotToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val hScroll = rememberScrollState()

    var code by rememberSaveable { mutableStateOf("") }
    var themeIndex by rememberSaveable { mutableStateOf(0) }
    var showLineNumbers by rememberSaveable { mutableStateOf(true) }
    var title by rememberSaveable { mutableStateOf("") }
    var fontIndex by rememberSaveable { mutableStateOf(1) }

    val theme = themes[themeIndex]
    val fontSizes = listOf(11, 13, 15)
    val lines = code.lines()
    val gutterWidth = lines.size.toString().length

    ToolScaffold {
        item { SectionHeader("预览（截图这一块）") }
        item {
            GroupedCard {
                CardPadding {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.background)
                            .padding(14.dp)
                    ) {
                        Column {
                            if (title.isNotBlank()) {
                                Text(
                                    title,
                                    color = theme.comment,
                                    fontSize = (fontSizes[fontIndex] + 1).sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            Row(Modifier.horizontalScroll(hScroll)) {
                                if (showLineNumbers) {
                                    Column(Modifier.padding(end = 12.dp)) {
                                        lines.indices.forEach { index ->
                                            Text(
                                                (index + 1).toString().padStart(gutterWidth),
                                                style = MonoStyle.copy(
                                                    color = theme.gutter,
                                                    fontSize = fontSizes[fontIndex].sp,
                                                    lineHeight = (fontSizes[fontIndex] * 1.55).sp
                                                )
                                            )
                                        }
                                    }
                                }
                                Column {
                                    if (code.isBlank()) {
                                        Text(
                                            "在下面粘贴代码",
                                            style = MonoStyle.copy(
                                                color = theme.comment,
                                                fontSize = fontSizes[fontIndex].sp
                                            )
                                        )
                                    } else {
                                        lines.forEach { line ->
                                            Text(
                                                highlight(line, theme),
                                                style = MonoStyle.copy(
                                                    fontSize = fontSizes[fontIndex].sp,
                                                    lineHeight = (fontSizes[fontIndex] * 1.55).sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "用系统截图截取上面这块深色区域，就得到一张可以直接发出去的代码图。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("样式") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = themes.map { it.name },
                        selectedIndex = themeIndex,
                        onSelected = { themeIndex = it }
                    )
                    SegmentedPicker(
                        options = listOf("小", "中", "大"),
                        selectedIndex = fontIndex,
                        onSelected = { fontIndex = it }
                    )
                    IosTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "标题或文件名（可选）"
                    )
                }
                ToggleRow("显示行号", showLineNumbers, onCheckedChange = { showLineNumbers = it })
            }
        }
        item { SectionHeader("代码") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = code,
                        onValueChange = { code = it },
                        placeholder = "粘贴代码",
                        minHeight = 160.dp,
                        mono = true
                    )
                    Text(
                        "高亮是通用规则，支持 Kotlin、Java、JS、Python、Go、SQL 等常见语法的关键字、字符串、数字和注释。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
