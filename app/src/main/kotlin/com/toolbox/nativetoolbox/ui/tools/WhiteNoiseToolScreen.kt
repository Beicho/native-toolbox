package com.toolbox.nativetoolbox.ui.tools

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 白噪音全部程序实时合成,不占安装包:
 * 白噪 = 均匀随机;粉噪 = Voss 滤波;雨 = 粉噪底 + 随机雨滴脉冲;海浪 = 低频包络调制的棕噪。
 */
private class NoiseEngine {
    @Volatile private var running = false
    private var thread: Thread? = null
    @Volatile var kind = 0
    @Volatile var volume = 0.6f

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            val sr = 32000
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder().setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                )
                .setBufferSizeInBytes(AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2)
                .build()
            track.play()
            val rnd = Random(System.nanoTime())
            val buf = ShortArray(2048)
            // 粉噪状态(Voss-McCartney 简化)
            val rows = FloatArray(8)
            var pinkSum = 0f
            var counter = 0
            // 棕噪状态
            var brown = 0f
            // 海浪包络相位 / 雨滴余量
            var wavePhase = 0.0
            var dropLeft = 0
            var dropAmp = 0f

            while (running) {
                val k = kind
                val vol = volume
                for (i in buf.indices) {
                    val sample: Float = when (k) {
                        0 -> rnd.nextFloat() * 2 - 1                       // 白噪
                        1 -> {                                              // 粉噪
                            counter++
                            var idx = 0
                            var c = counter
                            while (c and 1 == 0 && idx < 7) { c = c shr 1; idx++ }
                            pinkSum -= rows[idx]
                            rows[idx] = (rnd.nextFloat() * 2 - 1) * 0.5f
                            pinkSum += rows[idx]
                            (pinkSum / 3f)
                        }
                        2 -> {                                              // 雨
                            counter++
                            var idx = 0
                            var c = counter
                            while (c and 1 == 0 && idx < 7) { c = c shr 1; idx++ }
                            pinkSum -= rows[idx]
                            rows[idx] = (rnd.nextFloat() * 2 - 1) * 0.5f
                            pinkSum += rows[idx]
                            var s = pinkSum / 4f
                            if (dropLeft > 0) {
                                s += (rnd.nextFloat() * 2 - 1) * dropAmp * (dropLeft / 400f)
                                dropLeft--
                            } else if (rnd.nextInt(4000) == 0) {
                                dropLeft = 150 + rnd.nextInt(350)
                                dropAmp = 0.25f + rnd.nextFloat() * 0.5f
                            }
                            s
                        }
                        else -> {                                           // 海浪
                            brown += (rnd.nextFloat() * 2 - 1) * 0.02f
                            brown *= 0.998f
                            wavePhase += 2 * PI / (sr * (7.5))
                            val env = (sin(wavePhase) * 0.5 + 0.5).toFloat().let { it * it } + 0.06f
                            (brown * 3.2f * env)
                        }
                    }
                    buf[i] = (sample.coerceIn(-1f, 1f) * vol * 30000).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
            }
            track.stop(); track.release()
        }.apply { isDaemon = true; start() }
    }

    fun stop() { running = false; thread = null }
}

@Composable
fun WhiteNoiseToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var kind by rememberSaveable { mutableIntStateOf(2) }
    var volume by rememberSaveable { mutableFloatStateOf(0.6f) }
    var running by remember { mutableStateOf(false) }
    var timerMin by rememberSaveable { mutableIntStateOf(0) } // 0 = 不定时
    var remainSec by remember { mutableIntStateOf(0) }
    val engine = remember { NoiseEngine() }

    DisposableEffect(Unit) { onDispose { engine.stop() } }
    engine.kind = kind
    engine.volume = volume

    // 定时关闭
    androidx.compose.runtime.LaunchedEffect(running, timerMin) {
        if (running && timerMin > 0) {
            remainSec = timerMin * 60
            while (remainSec > 0 && running) {
                kotlinx.coroutines.delay(1000)
                remainSec--
            }
            if (running && remainSec <= 0) { engine.stop(); running = false }
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        listOf("均匀的沙沙声,盖住环境杂音", "更柔和的沙沙,适合长时间听", "淅淅沥沥的雨,偶尔有大雨滴", "一波一波的浪,7 秒一个周期")[kind],
                        style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel
                    )
                    Spacer(Modifier.height(10.dp))
                    SegmentedPicker(listOf("白噪", "粉噪", "雨声", "海浪"), kind, { kind = it }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("音量", style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.width(40.dp))
                        Slider(volume, { volume = it }, valueRange = 0.05f..1f, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("定时关闭", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(6.dp))
                    SegmentedPicker(listOf("不定时", "15分", "30分", "60分"), listOf(0, 15, 30, 60).indexOf(timerMin).coerceAtLeast(0), { timerMin = listOf(0, 15, 30, 60)[it] }, Modifier.fillMaxWidth())
                    if (running && timerMin > 0 && remainSec > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text("还剩 ${remainSec / 60}:${"%02d".format(remainSec % 60)}", style = MaterialTheme.typography.bodySmall, color = palette.accent)
                    }
                    Spacer(Modifier.height(12.dp))
                    SolidButton(
                        onClick = {
                            if (running) { engine.stop(); running = false }
                            else { engine.start(); running = true }
                        },
                        Modifier.fillMaxWidth()
                    ) { Text(if (running) "停止" else "开始播放") }
                    Spacer(Modifier.height(6.dp))
                    Text("声音实时生成,循环永不重复。锁屏后继续播,回这里停", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                }
            }
        }
    }
}
