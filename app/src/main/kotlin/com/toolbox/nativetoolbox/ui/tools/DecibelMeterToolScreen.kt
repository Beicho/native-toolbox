package com.toolbox.nativetoolbox.ui.tools

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.util.PermissionGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

private const val SAMPLE_RATE = 44100

/** 参考等级：日常场景对应的大致分贝值 */
private val references = listOf(
    "30 dB 以下" to "很安静，像深夜卧室",
    "30 到 45" to "安静的图书馆、办公室",
    "45 到 60" to "正常交谈、普通室内",
    "60 到 75" to "热闹的餐厅、马路边",
    "75 到 85" to "地铁车厢、吵闹环境",
    "85 以上" to "长期暴露会伤听力，要戴耳塞"
)

private fun levelDescription(db: Float): String = when {
    db < 30 -> "很安静"
    db < 45 -> "安静"
    db < 60 -> "正常"
    db < 75 -> "偏吵"
    db < 85 -> "吵"
    else -> "很吵，注意护耳"
}

@Composable
fun DecibelMeterToolScreen(onBack: () -> Unit) {
    ToolScaffold {
        item { SectionHeader("分贝仪") }
        item {
            GroupedCard {
                CardPadding {
                    PermissionGate(Manifest.permission.RECORD_AUDIO, "测环境噪音需要用麦克风采样") {
                        DecibelBody()
                    }
                }
            }
        }
    }
}

@Composable
private fun DecibelBody() {
    val palette = com.toolbox.nativetoolbox.ui.theme.LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var running by remember { mutableStateOf(false) }
    var current by remember { mutableFloatStateOf(0f) }
    var peak by remember { mutableFloatStateOf(0f) }
    var minimum by remember { mutableFloatStateOf(999f) }
    var samples by remember { mutableStateOf(0) }
    var sum by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf("") }

    DisposableEffect(running) {
        if (!running) {
            onDispose { }
        } else {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)
            var recorder: AudioRecord? = null
            val job = scope.launch(Dispatchers.Default) {
                try {
                    recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                    if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                        error = "麦克风打不开，可能被其他应用占用"
                        running = false
                        return@launch
                    }
                    recorder?.startRecording()
                    val buffer = ShortArray(bufferSize / 2)
                    while (isActive && running) {
                        val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            // 均方根 → 相对满量程的分贝，再加经验偏移贴近实际声压级
                            var square = 0.0
                            for (i in 0 until read) {
                                val v = buffer[i].toDouble()
                                square += v * v
                            }
                            val rms = sqrt(square / read)
                            val db = if (rms <= 0) 0f else (20 * log10(rms / 32767.0) + 94).toFloat()
                            val clamped = db.coerceIn(0f, 130f)
                            current = clamped
                            if (clamped > peak) peak = clamped
                            if (clamped < minimum) minimum = clamped
                            sum += clamped
                            samples += 1
                        }
                        delay(120)
                    }
                } catch (e: Exception) {
                    error = "采样失败：" + (e.message ?: "")
                    running = false
                } finally {
                    runCatching {
                        recorder?.stop()
                        recorder?.release()
                    }
                }
            }
            onDispose {
                job.cancel()
                runCatching {
                    recorder?.stop()
                    recorder?.release()
                }
            }
        }
    }

    val average = if (samples == 0) 0f else sum / samples
    val barFraction = (current / 110f).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                if (running) String.format("%.1f", current) else "—",
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                color = when {
                    !running -> palette.secondaryLabel
                    current >= 85 -> palette.red
                    current >= 70 -> palette.orange
                    else -> palette.green
                }
            )
            Text("dB", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
            if (running) {
                Text(
                    levelDescription(current),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accent
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(palette.sunkenBackground)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(barFraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        when {
                            current >= 85 -> palette.red
                            current >= 70 -> palette.orange
                            else -> palette.green
                        }
                    )
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCell("峰值", if (peak > 0) String.format("%.1f", peak) else "—", Modifier.weight(1f))
            StatCell("平均", if (samples > 0) String.format("%.1f", average) else "—", Modifier.weight(1f))
            StatCell("最低", if (minimum < 999f) String.format("%.1f", minimum) else "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SolidButton(onClick = { running = !running }, modifier = Modifier.weight(1f)) {
                Text(if (running) "停止" else "开始测量")
            }
            SolidButton(
                onClick = {
                    peak = 0f
                    minimum = 999f
                    sum = 0f
                    samples = 0
                },
                modifier = Modifier.weight(1f),
                filled = false
            ) { Text("清空统计") }
        }
        if (error.isNotBlank()) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = palette.red)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            references.forEachIndexed { index, (range, desc) ->
                KeyValueRow(range, desc, copyable = false)
                if (index != references.lastIndex) RowDivider()
            }
        }
        Text(
            "手机麦克风没有校准过，读数只能当相对参考，不能用于噪音投诉取证。声音只用于实时计算，不录制、不保存、不上传。",
            style = MaterialTheme.typography.bodySmall,
            color = palette.tertiaryLabel
        )
    }
}
