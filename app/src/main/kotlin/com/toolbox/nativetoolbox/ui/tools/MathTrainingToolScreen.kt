package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
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
import kotlin.random.Random

private class Question(val text: String, val answer: Int)

private fun makeQuestion(level: Int, ops: Int): Question {
    val max = when (level) {
        0 -> 20
        1 -> 100
        else -> 1000
    }
    val opChoices = when (ops) {
        0 -> listOf('+', '-')
        1 -> listOf('×', '÷')
        else -> listOf('+', '-', '×', '÷')
    }
    val op = opChoices.random()
    return when (op) {
        '+' -> {
            val a = Random.nextInt(1, max)
            val b = Random.nextInt(1, max)
            Question("$a + $b", a + b)
        }
        '-' -> {
            val a = Random.nextInt(1, max)
            val b = Random.nextInt(1, a + 1)
            Question("$a − $b", a - b)
        }
        '×' -> {
            val bound = when (level) {
                0 -> 10
                1 -> 20
                else -> 50
            }
            val a = Random.nextInt(2, bound)
            val b = Random.nextInt(2, bound)
            Question("$a × $b", a * b)
        }
        else -> {
            val bound = when (level) {
                0 -> 10
                1 -> 20
                else -> 50
            }
            val b = Random.nextInt(2, bound)
            val answer = Random.nextInt(2, bound)
            Question("${b * answer} ÷ $b", answer)
        }
    }
}

@Composable
fun MathTrainingToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var level by rememberSaveable { mutableStateOf(1) }
    var opsMode by rememberSaveable { mutableStateOf(2) }
    var durationIndex by rememberSaveable { mutableStateOf(1) }

    var running by rememberSaveable { mutableStateOf(false) }
    var question by remember { mutableStateOf(makeQuestion(1, 2)) }
    var input by rememberSaveable { mutableStateOf("") }
    var correct by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(60) }
    var feedback by rememberSaveable { mutableStateOf("") }
    var bestScore by rememberSaveable { mutableStateOf(0) }

    val durations = listOf(30, 60, 120)

    DisposableEffect(running) {
        if (!running) {
            onDispose { }
        } else {
            val job = scope.launch {
                while (isActive && running && remaining > 0) {
                    delay(1000)
                    remaining -= 1
                }
                if (isActive && remaining <= 0) {
                    running = false
                    if (correct > bestScore) bestScore = correct
                }
            }
            onDispose { job.cancel() }
        }
    }

    fun start() {
        correct = 0
        wrong = 0
        input = ""
        feedback = ""
        remaining = durations[durationIndex]
        question = makeQuestion(level, opsMode)
        running = true
    }

    fun submit() {
        val value = input.trim().toIntOrNull() ?: return
        if (value == question.answer) {
            correct += 1
            feedback = "对"
        } else {
            wrong += 1
            feedback = "错了，正确答案是 " + question.answer
        }
        input = ""
        question = makeQuestion(level, opsMode)
    }

    val total = correct + wrong
    val accuracy = if (total == 0) 0.0 else correct.toDouble() / total * 100

    ToolScaffold {
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (running) {
                        Text(
                            remaining.toString() + " 秒",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (remaining <= 10) palette.red else palette.secondaryLabel
                        )
                        Text(
                            question.text + " = ?",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Light,
                            color = palette.label
                        )
                        IosTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.padding(horizontal = 32.dp),
                            placeholder = "输入答案",
                            mono = true
                        )
                        SolidButton(onClick = { submit() }, enabled = input.isNotBlank()) { Text("提交") }
                        if (feedback.isNotBlank()) {
                            Text(
                                feedback,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (feedback == "对") palette.green else palette.red
                            )
                        }
                    } else {
                        Text(
                            if (total > 0) "本轮答对 " + correct + " 题" else "速算训练",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light,
                            color = palette.label
                        )
                        if (total > 0) {
                            Text(
                                String.format("正确率 %.0f%%", accuracy),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryLabel
                            )
                        }
                        SolidButton(onClick = { start() }) { Text(if (total > 0) "再来一轮" else "开始") }
                    }
                }
            }
        }
        item { SectionHeader("难度") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("20 以内", "100 以内", "1000 以内"),
                        selectedIndex = level,
                        onSelected = { level = it }
                    )
                    SegmentedPicker(
                        options = listOf("加减", "乘除", "混合"),
                        selectedIndex = opsMode,
                        onSelected = { opsMode = it }
                    )
                    SegmentedPicker(
                        options = durations.map { it.toString() + " 秒" },
                        selectedIndex = durationIndex,
                        onSelected = { durationIndex = it }
                    )
                    Text(
                        "除法都能整除，减法不出负数，适合小朋友练。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("统计") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("答对", correct.toString(), Modifier.weight(1f))
                        StatCell("答错", wrong.toString(), Modifier.weight(1f))
                        StatCell("最好成绩", bestScore.toString(), Modifier.weight(1f))
                    }
                }
                KeyValueRow("本轮题数", total.toString(), copyable = false)
                RowDivider()
                KeyValueRow("正确率", if (total == 0) "—" else String.format("%.1f%%", accuracy), copyable = false)
                RowDivider()
                KeyValueRow(
                    "平均速度",
                    if (correct == 0 || running) "—"
                    else String.format("%.1f 秒/题", durations[durationIndex].toDouble() / total),
                    copyable = false
                )
            }
        }
    }
}
