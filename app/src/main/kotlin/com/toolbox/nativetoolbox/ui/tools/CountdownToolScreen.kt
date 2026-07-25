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
import androidx.compose.runtime.mutableLongStateOf
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
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private fun stopwatchText(millis: Long): String {
    val total = millis.coerceAtLeast(0)
    val minutes = total / 60000
    val seconds = (total % 60000) / 1000
    val centis = (total % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, centis)
}

private fun countdownText(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

private fun buzz(context: android.content.Context) {
    runCatching {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                as android.os.VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
    }
}

@Composable
fun CountdownToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current

    var mode by rememberSaveable { mutableStateOf(0) }
    var keepAwake by rememberSaveable { mutableStateOf(true) }

    // 秒表
    var swRunning by rememberSaveable { mutableStateOf(false) }
    var swElapsed by remember { mutableLongStateOf(0L) }
    var swStartAt by remember { mutableLongStateOf(0L) }
    var laps by rememberSaveable { mutableStateOf("") }

    // 倒计时
    var minutesText by rememberSaveable { mutableStateOf("5") }
    var secondsText by rememberSaveable { mutableStateOf("0") }
    var cdRunning by rememberSaveable { mutableStateOf(false) }
    var cdRemain by remember { mutableLongStateOf(5 * 60_000L) }
    var cdEndAt by remember { mutableLongStateOf(0L) }
    var finished by rememberSaveable { mutableStateOf(false) }

    // 用绝对时间戳算差值，避免 delay 累积漂移
    DisposableEffect(swRunning) {
        if (!swRunning) {
            onDispose { }
        } else {
            swStartAt = System.currentTimeMillis() - swElapsed
            val job = scope.launch {
                while (isActive && swRunning) {
                    swElapsed = System.currentTimeMillis() - swStartAt
                    delay(30)
                }
            }
            onDispose { job.cancel() }
        }
    }

    DisposableEffect(cdRunning) {
        if (!cdRunning) {
            onDispose { }
        } else {
            cdEndAt = System.currentTimeMillis() + cdRemain
            val job = scope.launch {
                while (isActive && cdRunning) {
                    val left = cdEndAt - System.currentTimeMillis()
                    cdRemain = left.coerceAtLeast(0)
                    if (left <= 0) {
                        cdRunning = false
                        finished = true
                        buzz(context)
                        break
                    }
                    delay(100)
                }
            }
            onDispose { job.cancel() }
        }
    }

    DisposableEffect(keepAwake, swRunning, cdRunning) {
        view.keepScreenOn = keepAwake && (swRunning || cdRunning)
        onDispose { view.keepScreenOn = false }
    }

    ToolScaffold {
        item { SectionHeader("模式") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("倒计时", "秒表"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                }
            }
        }
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (mode == 0) {
                        Text(
                            countdownText(cdRemain),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Light,
                            color = if (finished) palette.red else palette.label
                        )
                        if (finished) {
                            Text("时间到", style = MaterialTheme.typography.titleMedium, color = palette.red)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SolidButton(
                                onClick = {
                                    finished = false
                                    if (cdRemain <= 0) {
                                        val m = minutesText.trim().toLongOrNull() ?: 0
                                        val s = secondsText.trim().toLongOrNull() ?: 0
                                        cdRemain = (m * 60 + s) * 1000
                                    }
                                    if (cdRemain > 0) cdRunning = !cdRunning
                                }
                            ) { Text(if (cdRunning) "暂停" else "开始") }
                            SolidButton(
                                onClick = {
                                    cdRunning = false
                                    finished = false
                                    val m = minutesText.trim().toLongOrNull() ?: 0
                                    val s = secondsText.trim().toLongOrNull() ?: 0
                                    cdRemain = (m * 60 + s) * 1000
                                },
                                filled = false
                            ) { Text("重置") }
                        }
                    } else {
                        Text(
                            stopwatchText(swElapsed),
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Light,
                            color = palette.label
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SolidButton(onClick = { swRunning = !swRunning }) {
                                Text(if (swRunning) "暂停" else if (swElapsed > 0) "继续" else "开始")
                            }
                            SolidButton(
                                onClick = {
                                    if (swRunning) {
                                        laps = (listOf(stopwatchText(swElapsed)) +
                                            laps.split("|").filter { it.isNotBlank() }).take(20).joinToString("|")
                                    }
                                },
                                filled = false,
                                enabled = swRunning
                            ) { Text("计圈") }
                            SolidButton(
                                onClick = {
                                    swRunning = false
                                    swElapsed = 0
                                    laps = ""
                                },
                                filled = false
                            ) { Text("清零") }
                        }
                    }
                }
            }
        }
        if (mode == 0) {
            item { SectionHeader("设定时长") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IosTextField(
                                value = minutesText,
                                onValueChange = { minutesText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = "分钟",
                                mono = true
                            )
                            IosTextField(
                                value = secondsText,
                                onValueChange = { secondsText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = "秒",
                                mono = true
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 3, 5, 10, 25).forEach { preset ->
                                SolidButton(
                                    onClick = {
                                        minutesText = preset.toString()
                                        secondsText = "0"
                                        cdRunning = false
                                        finished = false
                                        cdRemain = preset * 60_000L
                                    },
                                    modifier = Modifier.weight(1f),
                                    filled = false,
                                    height = 38.dp
                                ) { Text(preset.toString() + "分", style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                        Text(
                            "结束时会连续震动提醒。息屏后计时依然准确，因为用的是绝对时间。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        } else if (laps.isNotBlank()) {
            item { SectionHeader("圈速") }
            item {
                GroupedCard {
                    val items = laps.split("|").filter { it.isNotBlank() }
                    items.forEachIndexed { index, value ->
                        KeyValueRow("第 " + (items.size - index) + " 圈", value, copyable = false)
                        if (index != items.lastIndex) RowDivider()
                    }
                }
            }
        }
        item { SectionHeader("其他") }
        item {
            GroupedCard {
                ToggleRow("计时期间屏幕常亮", keepAwake, onCheckedChange = { keepAwake = it })
            }
        }
    }
}
