package com.toolbox.nativetoolbox.ui.tools

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.PI
import kotlin.math.sin

/** 生成一声「嗒」:短正弦爆音,accent 用更高频率 */
private fun clickPcm(sampleRate: Int, accent: Boolean): ShortArray {
    val ms = 28
    val n = sampleRate * ms / 1000
    val freq = if (accent) 1800.0 else 1100.0
    return ShortArray(n) { i ->
        val env = (1.0 - i.toDouble() / n).let { it * it }  // 快速衰减
        (sin(2 * PI * freq * i / sampleRate) * env * 26000).toInt().toShort()
    }
}

/** 节拍线程:AudioTrack 静态写入循环播放不好控,直接用流模式按拍写 */
private class MetronomeEngine {
    private var thread: Thread? = null
    @Volatile private var running = false
    @Volatile var bpm = 90
    @Volatile var beatsPerBar = 4
    @Volatile var onBeat: ((Int) -> Unit)? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            val sr = 44100
            val accent = clickPcm(sr, true)
            val normal = clickPcm(sr, false)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder().setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                )
                .setBufferSizeInBytes(AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2)
                .build()
            track.play()
            var beat = 0
            var nextAt = System.nanoTime()
            while (running) {
                val isAccent = beat % beatsPerBar == 0
                onBeat?.invoke(beat % beatsPerBar)
                val pcm = if (isAccent) accent else normal
                track.write(pcm, 0, pcm.size)
                beat++
                nextAt += (60_000_000_000L / bpm)
                val sleepNs = nextAt - System.nanoTime()
                if (sleepNs > 0) {
                    try { Thread.sleep(sleepNs / 1_000_000, (sleepNs % 1_000_000).toInt()) } catch (_: InterruptedException) { break }
                } else nextAt = System.nanoTime() // 落后太多就重新对表
            }
            track.stop(); track.release()
        }.apply { isDaemon = true; start() }
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }
}

@Composable
fun MetronomeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var bpm by rememberSaveable { mutableIntStateOf(90) }
    var beats by rememberSaveable { mutableIntStateOf(4) }
    var running by remember { mutableStateOf(false) }
    var currentBeat by remember { mutableIntStateOf(-1) }
    val engine = remember { MetronomeEngine() }

    DisposableEffect(Unit) { onDispose { engine.stop() } }
    engine.bpm = bpm
    engine.beatsPerBar = beats
    engine.onBeat = { b -> currentBeat = b }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$bpm", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = palette.label)
                        Text("BPM · " + when {
                            bpm < 60 -> "很慢 Largo"
                            bpm < 76 -> "慢板 Adagio"
                            bpm < 108 -> "行板 Andante"
                            bpm < 132 -> "中快 Moderato"
                            bpm < 168 -> "快板 Allegro"
                            else -> "急板 Presto"
                        }, style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                        Spacer(Modifier.height(12.dp))
                        // 拍点灯
                        val accent = palette.accent
                        val dim = palette.sunkenBackground
                        val orange = palette.orange
                        Canvas(Modifier.fillMaxWidth().height(26.dp)) {
                            val gap = size.width / beats
                            for (i in 0 until beats) {
                                drawCircle(
                                    when {
                                        i == currentBeat && i == 0 -> orange
                                        i == currentBeat -> accent
                                        else -> dim
                                    },
                                    radius = if (i == currentBeat) 13f else 9f,
                                    center = Offset(gap * i + gap / 2, size.height / 2)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Slider(bpm.toFloat(), { bpm = it.toInt() }, valueRange = 40f..220f, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth()) {
                        listOf(-5, -1, +1, +5).forEach { d ->
                            SolidButton(onClick = { bpm = (bpm + d).coerceIn(40, 220) }, Modifier.weight(1f), filled = false) {
                                Text(if (d > 0) "+$d" else "$d")
                            }
                            if (d != 5) Spacer(Modifier.width(6.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("每小节拍数", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(6.dp))
                    SegmentedPicker(listOf("2", "3", "4", "6"), listOf(2, 3, 4, 6).indexOf(beats).coerceAtLeast(0), { beats = listOf(2, 3, 4, 6)[it] }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    SolidButton(
                        onClick = {
                            if (running) { engine.stop(); running = false; currentBeat = -1 }
                            else { engine.start(); running = true }
                        },
                        Modifier.fillMaxWidth()
                    ) { Text(if (running) "停止" else "开始") }
                }
            }
        }
    }
}
