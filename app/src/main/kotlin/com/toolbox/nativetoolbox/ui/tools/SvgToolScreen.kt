package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private fun minifySvg(svg: String, removeComments: Boolean, removeMeta: Boolean): String {
    var out = svg
    if (removeComments) out = out.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
    if (removeMeta) {
        out = out.replace(Regex("<metadata>.*?</metadata>", RegexOption.DOT_MATCHES_ALL), "")
        out = out.replace(Regex("<title>.*?</title>", RegexOption.DOT_MATCHES_ALL), "")
        out = out.replace(Regex("<desc>.*?</desc>", RegexOption.DOT_MATCHES_ALL), "")
        out = out.replace(Regex("\\s(sodipodi|inkscape|xmlns:sodipodi|xmlns:inkscape):[a-zA-Z-]+=\"[^\"]*\""), "")
    }
    out = out.replace(Regex(">\\s+<"), "><")
    out = out.replace(Regex("\\s{2,}"), " ")
    // 小数保留三位，SVG 里超长小数纯属浪费
    out = Regex("(\\d+\\.\\d{4,})").replace(out) { m ->
        String.format("%.3f", m.value.toDouble()).trimEnd('0').trimEnd('.')
    }
    return out.trim()
}

private fun extractAttr(svg: String, name: String): String? =
    Regex(name + "\\s*=\\s*\"([^\"]*)\"").find(svg)?.groupValues?.getOrNull(1)

private fun toDataUri(svg: String): String {
    val encoded = svg
        .replace("\"", "'")
        .replace("<", "%3C")
        .replace(">", "%3E")
        .replace("#", "%23")
        .replace("\n", "")
    return "data:image/svg+xml," + encoded
}

@Composable
fun SvgToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var input by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(0) }
    var removeComments by rememberSaveable { mutableStateOf(true) }
    var removeMeta by rememberSaveable { mutableStateOf(true) }

    val isSvg = input.contains("<svg", true)
    val minified = if (!isSvg) "" else minifySvg(input, removeComments, removeMeta)
    val originalSize = input.toByteArray(Charsets.UTF_8).size
    val minifiedSize = minified.toByteArray(Charsets.UTF_8).size
    val saved = if (originalSize == 0) 0.0 else (originalSize - minifiedSize) * 100.0 / originalSize

    val width = extractAttr(input, "width")
    val height = extractAttr(input, "height")
    val viewBox = extractAttr(input, "viewBox")
    val pathCount = Regex("<path").findAll(input).count()
    val groupCount = Regex("<g[\\s>]").findAll(input).count()
    val hasStyle = input.contains("<style") || input.contains("style=")

    val output = when (mode) {
        0 -> minified
        1 -> toDataUri(minified)
        else -> buildString {
            appendLine("<!-- 在 HTML 里内联使用 -->")
            appendLine(minified)
            appendLine()
            appendLine("<!-- 作为背景图使用 -->")
            append("background-image: url(\"" + toDataUri(minified) + "\");")
        }
    }

    ToolScaffold {
        item { SectionHeader("SVG 源码") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "粘贴 SVG 代码",
                        minHeight = 150.dp,
                        mono = true
                    )
                    if (input.isNotBlank() && !isSvg) {
                        Text(
                            "这段内容里没找到 <svg 标签",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    }
                }
            }
        }
        if (isSvg) {
            item { SectionHeader("体积") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("原始", originalSize.toString() + " B", Modifier.weight(1f))
                            StatCell("压缩后", minifiedSize.toString() + " B", Modifier.weight(1f))
                            StatCell("省下", String.format("%.0f%%", saved), Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SectionHeader("结构信息") }
            item {
                GroupedCard {
                    KeyValueRow("宽", width ?: "未标注", copyable = false)
                    RowDivider()
                    KeyValueRow("高", height ?: "未标注", copyable = false)
                    RowDivider()
                    KeyValueRow("viewBox", viewBox ?: "未标注", copyable = false)
                    RowDivider()
                    KeyValueRow("path 数量", pathCount.toString(), copyable = false)
                    RowDivider()
                    KeyValueRow("分组数量", groupCount.toString(), copyable = false)
                    RowDivider()
                    KeyValueRow("含样式", if (hasStyle) "是" else "否", copyable = false)
                }
            }
            item { SectionHeader("优化选项") }
            item {
                GroupedCard {
                    ToggleRow("删掉注释", removeComments, onCheckedChange = { removeComments = it })
                    ToggleRow(
                        "删掉编辑器元数据",
                        removeMeta,
                        onCheckedChange = { removeMeta = it },
                        subtitle = "Inkscape、Illustrator 留下的无用属性"
                    )
                }
            }
            item { SectionHeader("输出格式") }
            item {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(
                            options = listOf("压缩后的 SVG", "Data URI", "使用示例"),
                            selectedIndex = mode,
                            onSelected = { mode = it }
                        )
                        OutputCard(text = output)
                    }
                }
            }
            if (viewBox == null) {
                item {
                    GroupedCard {
                        CardPadding {
                            Text(
                                "没有 viewBox 的 SVG 缩放时会出问题，建议补上 viewBox=\"0 0 宽 高\"。",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.orange
                            )
                        }
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "压缩只做安全的文本级优化：去注释、去元数据、压空白、小数保留三位。不改变图形结构，不做路径重写。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
