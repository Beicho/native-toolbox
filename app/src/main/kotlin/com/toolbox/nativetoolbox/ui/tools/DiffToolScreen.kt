package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
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
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.theme.MonoStyle

private sealed class DiffLine(val text: String) {
    class Same(text: String) : DiffLine(text)
    class Added(text: String) : DiffLine(text)
    class Removed(text: String) : DiffLine(text)
}

/** 行级 LCS diff(限制 400 行内,防卡顿) */
private fun diff(a: List<String>, b: List<String>): List<DiffLine> {
    val n = a.size
    val m = b.size
    if (n > 400 || m > 400) {
        return listOf(DiffLine.Same("(文本过长,最多支持 400 行对比)"))
    }
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
    }
    val out = mutableListOf<DiffLine>()
    var i = 0
    var j = 0
    while (i < n && j < m) {
        when {
            a[i] == b[j] -> { out.add(DiffLine.Same(a[i])); i++; j++ }
            dp[i + 1][j] >= dp[i][j + 1] -> { out.add(DiffLine.Removed(a[i])); i++ }
            else -> { out.add(DiffLine.Added(b[j])); j++ }
        }
    }
    while (i < n) { out.add(DiffLine.Removed(a[i])); i++ }
    while (j < m) { out.add(DiffLine.Added(b[j])); j++ }
    return out
}

@Composable
fun DiffToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var left by rememberSaveable { mutableStateOf("") }
    var right by rememberSaveable { mutableStateOf("") }

    val result = if (left.isEmpty() && right.isEmpty()) emptyList()
    else diff(left.lines(), right.lines())
    val changed = result.count { it !is DiffLine.Same }

    ToolScaffold {
        item { SectionHeader("原文本") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(value = left, onValueChange = { left = it }, placeholder = "旧版本…", minHeight = 110.dp, mono = true)
                }
            }
        }
        item { SectionHeader("新文本") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(value = right, onValueChange = { right = it }, placeholder = "新版本…", minHeight = 110.dp, mono = true)
                }
            }
        }
        item { SectionHeader(if (result.isEmpty()) "对比结果" else "对比结果 · $changed 处差异") }
        item {
            GroupedCard {
                if (result.isEmpty()) {
                    Text(
                        "两边都输入后自动对比",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.tertiaryLabel
                    )
                } else {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        result.take(300).forEach { line ->
                            val (prefix, bg, fg) = when (line) {
                                is DiffLine.Added -> Triple("+ ", palette.green.copy(alpha = 0.12f), palette.green)
                                is DiffLine.Removed -> Triple("- ", palette.red.copy(alpha = 0.12f), palette.red)
                                is DiffLine.Same -> Triple("  ", androidx.compose.ui.graphics.Color.Transparent, palette.secondaryLabel)
                            }
                            Text(
                                prefix + line.text,
                                Modifier
                                    .fillMaxWidth()
                                    .background(bg)
                                    .padding(horizontal = 16.dp, vertical = 1.dp),
                                style = MonoStyle,
                                color = fg
                            )
                        }
                    }
                }
            }
        }
    }
}
