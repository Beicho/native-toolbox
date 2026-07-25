package com.toolbox.nativetoolbox.ui.tools

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.PermissionGate
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

private val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

/** 频率 → (音名, 八度, 音分偏差) */
private fun freqToNote(freq: Double): Triple<String, Int, Int> {
    val midi = 69 + 12 * ln(freq / 440.0) / ln(2.0)
    val nearest = midi.roundToInt()
    val cents = ((midi - nearest) * 100).roundToInt()
    return Triple(NOTE_NAMES[(nearest % 12 + 12) % 12], nearest / 12 - 1, cents)
}

/** 自相关基频估计,置信不足返回 null */
private fun detectPitch(pcm: ShortArray, sr: Int): Double? {
    // 能量门限
    var energy = 0.0
    for (s in pcm) energy += s.toDouble() * s
    if (energy / pcm.size < 2_000_00) return null

    val minLag = sr / 1200   // 最高 1200Hz
    val maxLag = sr / 60     // 最低 60Hz
    var bestLag = -1
    var bestCorr = 0.0
    var lag = minLag
    while (lag <= maxLag) {
        var corr = 0.0
        for (i in 0 until pcm.size - lag) corr += pcm[i].toDouble() * pcm[i + lag]
        if (corr > bestCorr) { bestCorr = corr; bestLag = lag }
        lag++
    }
    if (bestLag <= 0) return null
    // 归一化置信
    if (bestCorr / energy < 0.3) return null
    // 抛物线插值细化
    val l = bestLag
    fun corrAt(g: Int): Double {
        var c = 0.0
        for (i in 0 until pcm.size - g) c += pcm[i].toDouble() * pcm[i + g]
        return c
    }
    val c0 = corrAt(l - 1); val c1 = corrAt(l); val c2 = corrAt(l + 1)
    val denom = 2 * (2 * c1 - c0 - c2)
    val shift = if (abs(denom) > 1e-9) (c0 - c2) / denom else 0.0
    return sr / (l + shift)
}

@SuppressLint("MissingPermission")
@Composable
private fun TunerContent() {
    val palette = LocalIosPalette.current
    var running by remember { mutableStateOf(false) }
    var freq by remember { mutableStateOf<Double?>(null) }
    var stopper by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun start() {
        val sr = 22050
        val bufSize = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
        val rec = runCatching {
            AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize * 2)
        }.getOrNull() ?: return
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return }
        rec.startRecording()
        running = true
        val t = Thread {
            val window = ShortArray(4096)
            while (running) {
                var got = 0
                while (got < window.size && running) {
                    val r = rec.read(window, got, window.size - got)
                    if (r <= 0) break
                    got += r
                }
                if (got == window.size) {
                    freq = detectPitch(window, sr)
                }
            }
            rec.stop(); rec.release()
        }.apply { isDaemon = true; start() }
        stopper = { running = false; runCatching { t.interrupt() } }
    }

    DisposableEffect(Unit) { onDispose { stopper?.invoke() } }

    GroupedCard {
        CardPadding {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val f = freq
                if (f != null) {
                    val (note, octave, cents) = freqToNote(f)
                    Text("$note$octave", fontSize = 68.sp, fontWeight = FontWeight.Bold,
                        color = if (abs(cents) <= 5) palette.green else palette.label)
                    Text("%.1f Hz · ${if (cents > 0) "+" else ""}$cents 音分".format(f), style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                } else {
                    Text("--", fontSize = 68.sp, fontWeight = FontWeight.Bold, color = palette.tertiaryLabel)
                    Text(if (running) "对着麦克风弹一个音" else "点开始,靠近乐器", style = MaterialTheme.typography.bodyMedium, color = palette.tertiaryLabel)
                }
                Spacer(Modifier.height(10.dp))
                // 音分指针条:-50 ~ +50
                val green = palette.green
                val red = palette.red
                val track = palette.sunkenBackground
                val label = palette.label
                Canvas(Modifier.fillMaxWidth().height(46.dp)) {
                    val cy = size.height * 0.6f
                    drawLine(track, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 8f, cap = StrokeCap.Round)
                    // 中线
                    drawLine(green, Offset(size.width / 2, cy - 16), Offset(size.width / 2, cy + 16), strokeWidth = 4f)
                    val f2 = freq
                    if (f2 != null) {
                        val cents = freqToNote(f2).third.coerceIn(-50, 50)
                        val x = size.width / 2 + size.width / 2 * (cents / 50f) * 0.94f
                        drawCircle(if (abs(cents) <= 5) green else if (abs(cents) <= 20) label else red, 13f, Offset(x, cy))
                    }
                }
                Spacer(Modifier.height(8.dp))
                SolidButton(
                    onClick = { if (running) { stopper?.invoke(); freq = null } else start() },
                    Modifier.fillMaxWidth()
                ) { Text(if (running) "停止" else "开始调音") }
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    SectionHeader("常用定音参考")
    GroupedCard {
        listOf(
            "吉他 6 弦" to "E2 · A2 · D3 · G3 · B3 · E4",
            "尤克里里" to "G4 · C4 · E4 · A4",
            "小提琴" to "G3 · D4 · A4 · E5",
            "标准音 A4" to "440 Hz",
        ).forEachIndexed { i, (k, v) ->
            KeyValueRow(k, v, copyable = false)
            if (i != 3) RowDivider()
        }
    }
}

@Composable
fun TunerToolScreen(onBack: () -> Unit) {
    ToolScaffold {
        item {
            PermissionGate(android.Manifest.permission.RECORD_AUDIO, "调音要用麦克风听音高,声音不保存") {
                TunerContent()
            }
        }
    }
}
