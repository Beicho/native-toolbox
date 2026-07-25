package com.toolbox.nativetoolbox.ui.tools

import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import com.toolbox.nativetoolbox.util.PermissionGate
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun fmtSec(s: Int): String = "%d:%02d".format(s / 60, s % 60)

@Composable
private fun RecorderContent() {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val dir = remember { File(context.filesDir, "recordings").apply { mkdirs() } }

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var marks by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var files by remember { mutableStateOf(dir.listFiles()?.sortedByDescending { it.name }?.toList() ?: emptyList()) }
    var status by remember { mutableStateOf("") }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingPath by remember { mutableStateOf("") }

    LaunchedEffect(recording) {
        while (recording) {
            kotlinx.coroutines.delay(1000)
            seconds++
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.stop(); recorder?.release() }
            runCatching { player?.release() }
        }
    }

    fun start() {
        val f = File(dir, SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".m4a")
        val r = MediaRecorder()
        runCatching {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128_000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
        }.onFailure { status = "录音启动失败:${it.message}"; r.release(); return }
        recorder = r
        currentFile = f
        seconds = 0
        marks = emptyList()
        recording = true
        status = ""
    }

    fun stop() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        recording = false
        val f = currentFile
        if (f != null && f.exists()) {
            // 书签写进同名 .txt
            if (marks.isNotEmpty()) {
                File(dir, f.nameWithoutExtension + ".marks").writeText(marks.joinToString(","))
            }
            status = "已保存 ${f.name}(${fmtSec(seconds)})"
        }
        files = dir.listFiles()?.filter { it.extension == "m4a" }?.sortedByDescending { it.name } ?: emptyList()
    }

    fun play(f: File) {
        runCatching { player?.release() }
        if (playingPath == f.absolutePath) { playingPath = ""; player = null; return }
        val p = MediaPlayer()
        runCatching {
            p.setDataSource(f.absolutePath)
            p.prepare()
            p.start()
            p.setOnCompletionListener { playingPath = "" }
        }.onFailure { status = "播放失败"; return }
        player = p
        playingPath = f.absolutePath
    }

    GroupedCard {
        CardPadding {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(fmtSec(seconds), fontSize = 56.sp, fontWeight = FontWeight.Bold, color = if (recording) palette.red else palette.label)
                if (marks.isNotEmpty()) {
                    Text("书签:" + marks.joinToString("  ") { fmtSec(it) }, style = MaterialTheme.typography.bodySmall, color = palette.accent)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    SolidButton(onClick = { if (recording) stop() else start() }, Modifier.weight(2f)) {
                        Text(if (recording) "停止并保存" else "开始录音")
                    }
                    if (recording) {
                        Spacer(Modifier.width(8.dp))
                        SolidButton(onClick = { marks = marks + seconds }, Modifier.weight(1f), filled = false) { Text("标记") }
                    }
                }
                if (status.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.contains("失败")) palette.red else palette.green)
                }
                Spacer(Modifier.height(4.dp))
                Text("录 128kbps AAC,开会录重点时点「标记」,回听不用从头翻", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
            }
        }
    }
    if (files.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        SectionHeader("我的录音(${files.size})")
        GroupedCard {
            files.take(20).forEachIndexed { i, f ->
                val markFile = File(dir, f.nameWithoutExtension + ".marks")
                val markText = if (markFile.exists()) markFile.readText().split(",").mapNotNull { it.toIntOrNull() }.joinToString(" ") { fmtSec(it) } else ""
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.nameWithoutExtension, style = MaterialTheme.typography.bodyMedium, color = palette.label)
                        Text(
                            FileHelper.formatFileSize(f.length()) + if (markText.isNotEmpty()) " · 书签 $markText" else "",
                            style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel
                        )
                    }
                    SolidButton(onClick = { play(f) }, Modifier.width(64.dp), height = 34.dp, filled = playingPath == f.absolutePath) {
                        Text(if (playingPath == f.absolutePath) "停" else "播")
                    }
                    Spacer(Modifier.width(6.dp))
                    SolidButton(onClick = {
                        val bytes = f.readBytes()
                        val r = FileHelper.saveToDownloads(context, f.name, bytes)
                        status = r.fold({ "已导出到 $it" }, { "导出失败" })
                    }, Modifier.width(64.dp), height = 34.dp, filled = false) { Text("导出") }
                    Spacer(Modifier.width(6.dp))
                    SolidButton(onClick = {
                        f.delete(); markFile.delete()
                        files = dir.listFiles()?.filter { it.extension == "m4a" }?.sortedByDescending { it.name } ?: emptyList()
                    }, Modifier.width(52.dp), height = 34.dp, filled = false) { Text("删") }
                }
                if (i != files.take(20).lastIndex) RowDivider()
            }
        }
    }
}

@Composable
fun RecorderToolScreen(onBack: () -> Unit) {
    ToolScaffold {
        item {
            PermissionGate(android.Manifest.permission.RECORD_AUDIO, "录音机需要麦克风") {
                RecorderContent()
            }
        }
    }
}
