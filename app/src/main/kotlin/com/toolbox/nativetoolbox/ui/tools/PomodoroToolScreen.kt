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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val STAGE_FOCUS = 0
private const val STAGE_SHORT = 1
private const val STAGE_LONG = 2

private fun clock(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return String.format("%02d:%02d", s / 60, s % 60)
}

private fun vibrate(context: android.content.Context) {
    runCatching {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                as android.os.VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        val pattern = longArrayOf(0, 250, 150, 250)
        vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
    }
}

@Composable
fun PomodoroToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current

    var focusMinutes by rememberSaveable { mutableStateOf("25") }
    var shortMinutes by rememberSaveable { mutableStateOf("5") }
    var longMinutes by rememberSaveable { mutableStateOf("15") }
    var roundsBeforeLong by rememberSaveable { mutableStateOf("4") }
    var keepAwake by rememberSaveable { mutableStateOf(false) }
    var vibrateOnSwitch by rememberSaveable { mutableStateOf(true) }

    var stage by remember { mutableIntStateOf(STAGE_FOCUS) }
    var remaining by remember { mutableIntStateOf(25 * 60) }
    var running by rememberSaveable { mutableStateOf(false) }
    var completedFocus by remember { mutableIntStateOf(0) }

    val focusSec = (focusMinutes.trim().toIntOrNull() ?: 25).coerceIn(1, 180) * 60
    val shortSec = (shortMinutes.trim().toIntOrNull() ?: 5).coerceIn(1, 60) * 60
    val longSec = (longMinutes.trim().toIntOrNull() ?: 15).coerceIn(1, 60) * 60
    val roundLimit = (roundsBeforeLong.trim().toIntOrNull() ?: 4).coerceIn(1, 12)

    fun durationOf(target: Int): Int = when (target) {
        STAGE_FOCUS -> focusSec
        STAGE_SHORT -> shortSec
        else -> longSec
    }

    fun reset(target: Int) {
        stage = target
        remaining = durationOf(target)
    }

    DisposableEffect(running) {
        if (!running) {
            onDispose { }
        } else {
            val job = scope.launch {
                while (isActive && running) {
                    delay(1000)
                    if (remaining > 1) {
                        remaining -= 1
                    } else {
                        if (vibrateOnSwitch) vibrate(context)
                        if (stage == STAGE_FOCUS) {
                            completedFocus += 1
                            val next = if (completedFocus % roundLimit == 0) STAGE_LONG else STAGE_SHORT
                            reset(next)
                        } else {
                            reset(STAGE_FOCUS)
                        }
                    }
                }
            }
            onDispose { job.cancel() }
        }
    }

    DisposableEffect(keepAwake, running) {
        view.keepScreenOn = keepAwake && running
        onDispose { view.keepScreenOn = false }
    }

    val stageName = when (stage) {
        STAGE_FOCUS -> "专注"
        STAGE_SHORT -> "短休息"
        else -> "长休息"
    }
    val stageColor = when (stage) {
        STAGE_FOCUS -> palette.accent
        STAGE_SHORT -> palette.green
        else -> palette.teal
    }

    ToolScaffold {
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(stageName, style = MaterialTheme.typography.titleMedium, color = stageColor)
                    Text(
                        clock(remaining),
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Light,
                        color = palette.label
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SolidButton(onClick = { running = !running }) {
                            Text(if (running) "暂停" else "开始")
                        }
                        SolidButton(onClick = { reset(stage) }, filled = false) { Text("重来这一段") }
                        SolidButton(
                            onClick = {
                                running = false
                                completedFocus = 0
                                reset(STAGE_FOCUS)
                            },
                            filled = false
                        ) { Text("全部重置") }
                    }
                }
            }
        }
        item { SectionHeader("今日进度") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("完成专注", completedFocus.toString() + " 个", Modifier.weight(1f))
                        StatCell("专注时长", (completedFocus * focusSec / 60).toString() + " 分", Modifier.weight(1f))
                        StatCell(
                            "距长休息",
                            (roundLimit - completedFocus % roundLimit).toString() + " 个",
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        item { SectionHeader("时长设置") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = focusMinutes,
                            onValueChange = { focusMinutes = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "专注分钟",
                            mono = true
                        )
                        IosTextField(
                            value = shortMinutes,
                            onValueChange = { shortMinutes = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "短休分钟",
                            mono = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = longMinutes,
                            onValueChange = { longMinutes = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "长休分钟",
                            mono = true
                        )
                        IosTextField(
                            value = roundsBeforeLong,
                            onValueChange = { roundsBeforeLong = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "几轮后长休",
                            mono = true
                        )
                    }
                    Text(
                        "改完设置点「重来这一段」生效。经典番茄工作法是 25 + 5，每四轮长休 15 分钟。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
                ToggleRow("切换阶段时震动提醒", vibrateOnSwitch, onCheckedChange = { vibrateOnSwitch = it })
                ToggleRow(
                    "计时期间屏幕常亮",
                    keepAwake,
                    onCheckedChange = { keepAwake = it },
                    subtitle = "关掉更省电，但息屏后计时仍在走"
                )
            }
        }
        item { SectionHeader("当前节奏") }
        item {
            GroupedCard {
                KeyValueRow("专注", (focusSec / 60).toString() + " 分钟", copyable = false)
                RowDivider()
                KeyValueRow("短休息", (shortSec / 60).toString() + " 分钟", copyable = false)
                RowDivider()
                KeyValueRow("长休息", (longSec / 60).toString() + " 分钟", copyable = false)
                RowDivider()
                KeyValueRow("长休间隔", "每 " + roundLimit + " 个专注", copyable = false)
            }
        }
    }
}
