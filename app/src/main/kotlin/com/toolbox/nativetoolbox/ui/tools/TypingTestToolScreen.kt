package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val englishTexts = listOf(
    "The quick brown fox jumps over the lazy dog while the sun sets behind the hills.",
    "Simplicity is the ultimate sophistication, and clarity beats cleverness every time.",
    "Good code is its own best documentation, but a short comment still saves an hour.",
    "Move fast, but leave the campsite cleaner than you found it for the next person."
)

private val chineseTexts = listOf(
    "山重水复疑无路，柳暗花明又一村。",
    "工具应该安静地待在那里，需要的时候一伸手就能用上。",
    "写下来的东西才算真正想清楚了，模糊的想法经不起一句一句的推敲。",
    "慢一点没关系，方向对了就不算走弯路。"
)

private val codeTexts = listOf(
    "val result = items.filter { it.isActive }.map { it.name }.sorted()",
    "if (user == null) return Result.failure(IllegalStateException(\"no user\"))",
    "fun sum(a: Int, b: Int): Int = a + b // 简单到不需要测试",
    "for (i in 0 until size) { total += matrix[i][i] }"
)

private fun buildDiff(target: String, typed: String, correct: androidx.compose.ui.graphics.Color, wrong: androidx.compose.ui.graphics.Color, pending: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        target.forEachIndexed { index, c ->
            when {
                index >= typed.length -> withStyle(SpanStyle(color = pending)) { append(c) }
                typed[index] == c -> withStyle(SpanStyle(color = correct)) { append(c) }
                else -> withStyle(SpanStyle(color = wrong)) { append(c) }
            }
        }
    }

@Composable
fun TypingTestToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var categoryIndex by rememberSaveable { mutableStateOf(0) }
    var target by rememberSaveable { mutableStateOf(englishTexts.first()) }
    var typed by rememberSaveable { mutableStateOf("") }
    var running by rememberSaveable { mutableStateOf(false) }
    var startAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableIntStateOf(0) }
    var bestWpm by rememberSaveable { mutableStateOf(0) }

    val pool = when (categoryIndex) {
        0 -> englishTexts
        1 -> chineseTexts
        else -> codeTexts
    }

    DisposableEffect(running) {
        if (!running) {
            onDispose { }
        } else {
            val job = scope.launch {
                while (isActive && running) {
                    delay(200)
                    elapsed = ((System.currentTimeMillis() - startAt) / 1000).toInt()
                }
            }
            onDispose { job.cancel() }
        }
    }

    val correctChars = typed.filterIndexed { index, c -> index < target.length && target[index] == c }.length
    val accuracy = if (typed.isEmpty()) 100.0 else correctChars * 100.0 / typed.length
    val minutes = if (elapsed == 0) 0.0 else elapsed / 60.0
    // 英文按每 5 字符一词，中文和代码直接按字符计
    val unit = if (categoryIndex == 0) 5.0 else 1.0
    val wpm = if (minutes <= 0) 0 else Math.round(correctChars / unit / minutes).toInt()
    val finished = typed.length >= target.length

    // 同上：打完一段的结算放副作用里，不能在组合期写状态
    LaunchedEffect(finished) {
        if (finished && running) {
            running = false
            if (wpm > bestWpm) bestWpm = wpm
        }
    }

    fun start(newText: String) {
        target = newText
        typed = ""
        elapsed = 0
        startAt = System.currentTimeMillis()
        running = true
    }

    ToolScaffold {
        item { SectionHeader("成绩") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell(if (categoryIndex == 0) "WPM" else "字/分", wpm.toString(), Modifier.weight(1f))
                        StatCell("正确率", String.format("%.0f%%", accuracy), Modifier.weight(1f))
                        StatCell("用时", elapsed.toString() + " 秒", Modifier.weight(1f))
                    }
                    if (finished) {
                        Text(
                            "打完了。" + (if (wpm >= bestWpm && wpm > 0) "刷新了个人最好成绩。" else "个人最好 " + bestWpm),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.green
                        )
                    }
                }
            }
        }
        item { SectionHeader("目标文本") }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        buildDiff(target, typed, palette.green, palette.red, palette.tertiaryLabel),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        item { SectionHeader("在这里打") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = typed,
                        onValueChange = {
                            if (!running && it.isNotEmpty()) {
                                startAt = System.currentTimeMillis()
                                running = true
                            }
                            if (it.length <= target.length) typed = it
                        },
                        placeholder = "开始输入就自动计时",
                        minHeight = 100.dp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = { start(pool.random()) },
                            modifier = Modifier.weight(1f)
                        ) { Text("换一段") }
                        SolidButton(
                            onClick = { start(target) },
                            modifier = Modifier.weight(1f),
                            filled = false
                        ) { Text("重打这段") }
                    }
                }
            }
        }
        item { SectionHeader("文本类型") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("英文", "中文", "代码"),
                        selectedIndex = categoryIndex,
                        onSelected = {
                            categoryIndex = it
                            start(
                                when (it) {
                                    0 -> englishTexts.random()
                                    1 -> chineseTexts.random()
                                    else -> codeTexts.random()
                                }
                            )
                        }
                    )
                    Text(
                        if (categoryIndex == 0) "英文按国际惯例每 5 个字符算一个词。"
                        else "中文和代码直接按字符数计速。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("参考水平") }
        item {
            GroupedCard {
                val refs = if (categoryIndex == 0) listOf(
                    "20 WPM 以下" to "刚开始练",
                    "30 到 40" to "日常够用",
                    "50 到 70" to "熟练打字员",
                    "80 以上" to "很快了"
                ) else listOf(
                    "30 字/分以下" to "刚开始练",
                    "50 到 80" to "日常够用",
                    "100 到 150" to "熟练",
                    "150 以上" to "很快了"
                )
                refs.forEachIndexed { index, (k, v) ->
                    KeyValueRow(k, v, copyable = false)
                    if (index != refs.lastIndex) RowDivider()
                }
            }
        }
    }
}
