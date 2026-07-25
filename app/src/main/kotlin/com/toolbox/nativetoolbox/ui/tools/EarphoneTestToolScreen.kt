package com.toolbox.nativetoolbox.ui.tools

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.PI
import kotlin.math.sin

/** 双声道正弦发生器:left/right 开关 + 频率,支持扫频 */
private class ToneEngine {
    @Volatile private var running = false
    private var thread: Thread? = null
    @Volatile var freq = 440.0
    @Volatile var left = true
    @Volatile var right = true
    @Volatile var sweep = false

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            val sr = 44100
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder().setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
                )
                .setBufferSizeInBytes(AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 2)
                .build()
            track.play()
            val buf = ShortArray(2048 * 2)
            var phase = 0.0
            var sweepT = 0.0
            while (running) {
                val f0 = freq
                for (i in 0 until buf.size / 2) {
                    val f = if (sweep) {
                        // 20Hz→18kHz 对数扫频 12 秒循环
                        sweepT += 1.0 / sr
                        if (sweepT > 12.0) sweepT = 0.0
                        20.0 * Math.pow(900.0, sweepT / 12.0)
                    } else f0
                    phase += 2 * PI * f / sr
                    if (phase > 2 * PI) phase -= 2 * PI
                    val v = (sin(phase) * 22000).toInt().toShort()
                    buf[i * 2] = if (left) v else 0
                    buf[i * 2 + 1] = if (right) v else 0
                }
                track.write(buf, 0, buf.size)
            }
            track.stop(); track.release()
        }.apply { isDaemon = true; start() }
    }

    fun stop() { running = false; thread = null }
}

@Composable
fun EarphoneTestToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val engine = remember { ToneEngine() }
    var playing by remember { mutableStateOf("") } // "" | left | right | both | sweep | freq

    DisposableEffect(Unit) { onDispose { engine.stop() } }

    fun play(mode: String, freq: Double = 440.0) {
        engine.stop()
        if (playing == mode) { playing = ""; return }
        engine.freq = freq
        engine.sweep = mode == "sweep"
        engine.left = mode != "right"
        engine.right = mode != "left"
        engine.start()
        playing = mode
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Text("戴上耳机逐项测,看看有没有偏科", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.label)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        SolidButton(onClick = { play("left") }, Modifier.weight(1f), filled = playing == "left") { Text("只响左耳") }
                        Spacer(Modifier.width(8.dp))
                        SolidButton(onClick = { play("right") }, Modifier.weight(1f), filled = playing == "right") { Text("只响右耳") }
                    }
                    Spacer(Modifier.height(8.dp))
                    SolidButton(onClick = { play("both") }, Modifier.fillMaxWidth(), filled = playing == "both") { Text("双声道同响(听声音是否居中)") }
                    Spacer(Modifier.height(8.dp))
                    SolidButton(onClick = { play("sweep") }, Modifier.fillMaxWidth(), filled = playing == "sweep") { Text("20Hz→18kHz 扫频(测频响范围)") }
                    if (playing == "sweep") {
                        Spacer(Modifier.height(6.dp))
                        Text("从低频扫到高频,12 秒一轮。听不到开头是低频弱,听不到结尾是高频衰(年龄大了正常)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    }
                }
            }
        }
        item { SectionHeader("定频测试") }
        item {
            GroupedCard {
                listOf(
                    Triple("低音 60 Hz", 60.0, "感受下潜"),
                    Triple("中低 250 Hz", 250.0, "人声厚度"),
                    Triple("中频 1 kHz", 1000.0, "标准参考音"),
                    Triple("高频 8 kHz", 8000.0, "明亮度"),
                    Triple("极高 15 kHz", 15000.0, "多数成年人已听不清"),
                ).forEachIndexed { i, (name, f, desc) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            KeyValueRow(name, desc, copyable = false)
                        }
                        SolidButton(
                            onClick = { play("f$f", f) },
                            Modifier.width(72.dp), height = 34.dp, filled = playing == "f$f"
                        ) { Text(if (playing == "f$f") "停" else "播") }
                        Spacer(Modifier.width(12.dp))
                    }
                    if (i != 4) RowDivider()
                }
            }
        }
        item {
            SolidButton(
                onClick = { engine.stop(); playing = "" },
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                filled = false, enabled = playing.isNotEmpty()
            ) { Text("全部停止") }
        }
    }
}
