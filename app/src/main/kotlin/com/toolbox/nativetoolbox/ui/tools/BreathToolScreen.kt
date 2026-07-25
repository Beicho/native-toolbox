package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 一种呼吸法 = 吸气/屏息/呼气/屏息 四段秒数 */
private class BreathPattern(
    val name: String,
    val inhale: Int,
    val hold1: Int,
    val exhale: Int,
    val hold2: Int,
    val desc: String
)

private val patterns = listOf(
    BreathPattern("4-7-8 助眠", 4, 7, 8, 0, "吸 4 秒、屏 7 秒、呼 8 秒。呼气拉长能让心率降下来，睡前用。"),
    BreathPattern("箱式呼吸", 4, 4, 4, 4, "四段都是 4 秒。军警和运动员用来快速稳定情绪。"),
    BreathPattern("生理性叹息", 4, 1, 6, 0, "短吸一口再补一口，然后长呼。缓解急性紧张最快。"),
    BreathPattern("平缓放松", 5, 0, 5, 0, "一分钟六次的慢呼吸，适合长时间练习。")
)

private class Phase(val label: String, val seconds: Int, val expand: Boolean?)

private fun phasesOf(p: BreathPattern): List<Phase> = buildList {
    add(Phase("吸气", p.inhale, true))
    if (p.hold1 > 0) add(Phase("屏息", p.hold1, null))
    add(Phase("呼气", p.exhale, false))
    if (p.hold2 > 0) add(Phase("屏息", p.hold2, null))
}

@Composable
fun BreathToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var patternIndex by rememberSaveable { mutableStateOf(0) }
    var running by rememberSaveable { mutableStateOf(false) }
    var phaseIndex by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(0) }
    var cycles by remember { mutableIntStateOf(0) }

    val pattern = patterns[patternIndex]
    val phases = remember(patternIndex) { phasesOf(pattern) }
    val phase = phases[phaseIndex.coerceIn(0, phases.lastIndex)]

    // 目标缩放：吸气涨到 1.0，呼气缩到 0.55，屏息保持
    var targetScale by remember { mutableStateOf(0.55f) }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = phase.seconds.coerceAtLeast(1) * 1000),
        label = "breath"
    )

    DisposableEffect(running, patternIndex) {
        if (!running) {
            onDispose { }
        } else {
            phaseIndex = 0
            remaining = phases[0].seconds
            targetScale = 1f
            val job = scope.launch {
                while (isActive && running) {
                    delay(1000)
                    if (remaining > 1) {
                        remaining -= 1
                    } else {
                        val next = (phaseIndex + 1) % phases.size
                        if (next == 0) cycles += 1
                        phaseIndex = next
                        remaining = phases[next].seconds
                        phases[next].expand?.let { expand -> targetScale = if (expand) 1f else 0.55f }
                    }
                }
            }
            onDispose { job.cancel() }
        }
    }

    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    ToolScaffold {
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(200.dp)
                                .scale(if (running) scale else 0.55f)
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.18f))
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (running) phase.label else "准备",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.label
                            )
                            Text(
                                if (running) remaining.toString() else "—",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Light,
                                color = palette.accent
                            )
                        }
                    }
                    Text(
                        if (running) "已完成 " + cycles + " 轮" else "跟着圆圈的大小呼吸",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryLabel
                    )
                    SolidButton(
                        onClick = {
                            running = !running
                            if (!running) {
                                cycles = 0
                                phaseIndex = 0
                                targetScale = 0.55f
                            }
                        },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) { Text(if (running) "结束" else "开始") }
                }
            }
        }
        item { SectionHeader("呼吸法") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = patterns.map { it.name },
                        selectedIndex = patternIndex,
                        onSelected = {
                            patternIndex = it
                            running = false
                            cycles = 0
                            phaseIndex = 0
                            targetScale = 0.55f
                        }
                    )
                    Text(
                        pattern.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader("节奏") }
        item {
            GroupedCard {
                KeyValueRow("吸气", pattern.inhale.toString() + " 秒", copyable = false)
                RowDivider()
                KeyValueRow("吸气后屏息", if (pattern.hold1 > 0) pattern.hold1.toString() + " 秒" else "不屏息", copyable = false)
                RowDivider()
                KeyValueRow("呼气", pattern.exhale.toString() + " 秒", copyable = false)
                RowDivider()
                KeyValueRow("呼气后屏息", if (pattern.hold2 > 0) pattern.hold2.toString() + " 秒" else "不屏息", copyable = false)
                RowDivider()
                KeyValueRow("一轮时长", (pattern.inhale + pattern.hold1 + pattern.exhale + pattern.hold2).toString() + " 秒", copyable = false)
            }
        }
    }
}
