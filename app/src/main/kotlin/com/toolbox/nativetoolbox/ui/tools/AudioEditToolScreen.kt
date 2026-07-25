package com.toolbox.nativetoolbox.ui.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.AudioKit
import com.toolbox.nativetoolbox.util.FileHelper
import com.toolbox.nativetoolbox.util.PcmPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

private fun fmtS(sec: Float): String = "%d:%02d".format(sec.toInt() / 60, sec.toInt() % 60)

@Composable
fun AudioEditToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pcm by remember { mutableStateOf<AudioKit.Pcm?>(null) }
    var range by remember { mutableStateOf(0f..10f) }
    var clips by remember { mutableStateOf<List<AudioKit.Pcm>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val player = remember { PcmPlayer() }

    DisposableEffect(Unit) { onDispose { player.stop() } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; status = "解码中…"
        scope.launch {
            pcm = withContext(Dispatchers.IO) { AudioKit.decode(context, uri) }
            if (pcm != null) {
                range = 0f..(pcm!!.durationMs / 1000f)
                status = ""
            } else status = "解码失败"
            busy = false
        }
    }

    // 波形抽样(每像素峰值)
    val waveform = remember(pcm) {
        val p = pcm ?: return@remember FloatArray(0)
        val buckets = 240
        val out = FloatArray(buckets)
        val per = (p.samples.size / buckets).coerceAtLeast(1)
        for (b in 0 until buckets) {
            var peak = 0
            var i = b * per
            val end = minOf(i + per, p.samples.size)
            while (i < end) {
                val v = abs(p.samples[i].toInt())
                if (v > peak) peak = v
                i += 16
            }
            out[b] = peak / 32768f
        }
        out
    }

    fun selection(): AudioKit.Pcm? {
        val p = pcm ?: return null
        return AudioKit.trim(p, (range.start * 1000).toLong(), (range.endInclusive * 1000).toLong())
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    val p = pcm
                    if (p == null) {
                        Text("裁剪一段声音,或把几段拼在一起", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        // 波形 + 选区
                        val accent = palette.accent
                        val dim = palette.sunkenBackground
                        val total = (p.durationMs / 1000f).coerceAtLeast(0.1f)
                        Canvas(Modifier.fillMaxWidth().height(84.dp)) {
                            val n = waveform.size
                            if (n > 0) {
                                val bw = size.width / n
                                val selStart = range.start / total * size.width
                                val selEnd = range.endInclusive / total * size.width
                                for (i in 0 until n) {
                                    val x = i * bw + bw / 2
                                    val h = (waveform[i] * size.height * 0.92f).coerceAtLeast(2f)
                                    drawLine(
                                        if (x in selStart..selEnd) accent else dim,
                                        Offset(x, size.height / 2 - h / 2),
                                        Offset(x, size.height / 2 + h / 2),
                                        strokeWidth = bw * 0.7f
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        RangeSlider(value = range, onValueChange = { range = it }, valueRange = 0f..total)
                        Text(
                            "选中 ${fmtS(range.start)} ~ ${fmtS(range.endInclusive)}(共 ${fmtS(range.endInclusive - range.start)})",
                            style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    SolidButton(onClick = { picker.launch("audio/*") }, Modifier.fillMaxWidth(), filled = pcm == null, enabled = !busy) {
                        Text(if (busy && pcm == null) "解码中…" else if (pcm == null) "选音频" else "换一个")
                    }
                }
            }
        }
        item {
            if (pcm != null) {
                GroupedCard {
                    CardPadding {
                        Row(Modifier.fillMaxWidth()) {
                            SolidButton(
                                onClick = {
                                    if (playing) { player.stop(); playing = false }
                                    else {
                                        val s = selection() ?: return@SolidButton
                                        playing = true
                                        player.play(s) { playing = false }
                                    }
                                },
                                Modifier.weight(1f), filled = false
                            ) { Text(if (playing) "停" else "试听选区") }
                            Spacer(Modifier.width(8.dp))
                            SolidButton(
                                onClick = {
                                    val s = selection() ?: return@SolidButton
                                    clips = clips + s
                                    status = "已加入拼接篮(${clips.size} 段)"
                                },
                                Modifier.weight(1f), filled = false
                            ) { Text("选区入篮") }
                        }
                        Spacer(Modifier.height(8.dp))
                        SolidButton(
                            onClick = {
                                busy = true; status = "导出中…"
                                scope.launch {
                                    val r = withContext(Dispatchers.Default) {
                                        runCatching {
                                            val s = selection() ?: throw Exception("没有选区")
                                            val tmp = File(context.cacheDir, "edit_${System.currentTimeMillis()}.m4a")
                                            if (!AudioKit.encodeM4a(s, tmp)) throw Exception("编码失败")
                                            val b = tmp.readBytes(); tmp.delete()
                                            FileHelper.saveToDownloads(context, "裁剪_${System.currentTimeMillis()}.m4a", b).getOrThrow()
                                        }
                                    }
                                    status = r.fold({ "已存到 $it" }, { "失败:${it.message}" })
                                    busy = false
                                }
                            },
                            Modifier.fillMaxWidth(), enabled = !busy
                        ) { Text("导出选区 m4a") }
                        if (status.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.contains("失败")) palette.red else palette.green)
                        }
                    }
                }
            }
        }
        item { if (clips.isNotEmpty()) SectionHeader("拼接篮(按顺序连成一段)") }
        item {
            if (clips.isNotEmpty()) {
                GroupedCard {
                    clips.forEachIndexed { i, c ->
                        KeyValueRow("第 ${i + 1} 段", "${c.durationMs / 1000} 秒", copyable = false)
                        if (i != clips.lastIndex) RowDivider()
                    }
                    CardPadding {
                        Row(Modifier.fillMaxWidth()) {
                            SolidButton(onClick = { clips = emptyList() }, Modifier.weight(1f), filled = false) { Text("清空") }
                            Spacer(Modifier.width(8.dp))
                            SolidButton(
                                onClick = {
                                    busy = true; status = "拼接导出中…"
                                    scope.launch {
                                        val r = withContext(Dispatchers.Default) {
                                            runCatching {
                                                val merged = AudioKit.concat(clips) ?: throw Exception("拼接失败")
                                                val tmp = File(context.cacheDir, "concat_${System.currentTimeMillis()}.m4a")
                                                if (!AudioKit.encodeM4a(merged, tmp)) throw Exception("编码失败")
                                                val b = tmp.readBytes(); tmp.delete()
                                                FileHelper.saveToDownloads(context, "拼接_${System.currentTimeMillis()}.m4a", b).getOrThrow()
                                            }
                                        }
                                        status = r.fold({ "已存到 $it" }, { "失败:${it.message}" })
                                        busy = false
                                    }
                                },
                                Modifier.weight(2f), enabled = !busy
                            ) { Text("拼接导出(${clips.size} 段)") }
                        }
                    }
                }
            }
        }
    }
}
