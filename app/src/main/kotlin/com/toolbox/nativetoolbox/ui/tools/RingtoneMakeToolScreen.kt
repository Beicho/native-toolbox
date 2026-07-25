package com.toolbox.nativetoolbox.ui.tools

import android.content.ContentValues
import android.content.Intent
import android.media.RingtoneManager
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.AudioKit
import com.toolbox.nativetoolbox.util.PcmPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun fmtMs(ms: Long): String = "%d:%02d".format(ms / 60000, ms / 1000 % 60)

@Composable
fun RingtoneMakeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pcm by remember { mutableStateOf<AudioKit.Pcm?>(null) }
    var range by remember { mutableStateOf(0f..30f) }
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
                val dur = pcm!!.durationMs / 1000f
                range = 0f..minOf(30f, dur)
                status = ""
            } else status = "解码失败"
            busy = false
        }
    }

    fun clip(): AudioKit.Pcm? {
        val p = pcm ?: return null
        return AudioKit.trim(p, (range.start * 1000).toLong(), (range.endInclusive * 1000).toLong())
    }

    /** 导出 m4a 到媒体库 Ringtones 并可设为默认铃声 */
    fun export(setAs: Boolean) {
        busy = true; status = "导出中…"
        scope.launch {
            val r = withContext(Dispatchers.Default) {
                runCatching {
                    val c = clip() ?: throw Exception("先选音频")
                    val tmp = File(context.cacheDir, "ring_${System.currentTimeMillis()}.m4a")
                    if (!AudioKit.encodeM4a(c, tmp)) throw Exception("编码失败")
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, "AstroKit铃声_${System.currentTimeMillis()}.m4a")
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                        put(MediaStore.Audio.Media.IS_RINGTONE, true)
                        put(MediaStore.Audio.Media.RELATIVE_PATH, "Ringtones/AstroKit")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw Exception("媒体库写入失败")
                    context.contentResolver.openOutputStream(uri)?.use { it.write(tmp.readBytes()) }
                    tmp.delete()
                    uri
                }
            }
            r.onSuccess { uri ->
                if (setAs) {
                    if (Settings.System.canWrite(context)) {
                        runCatching {
                            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, uri)
                            status = "已设为来电铃声"
                        }.onFailure { status = "已导出,但设置铃声失败:${it.message}" }
                    } else {
                        status = "已导出。设铃声还需要「修改系统设置」权限,去开一下再回来"
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                    .setData(android.net.Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                } else status = "已导出到铃声库(系统设置里选铃声能看到)"
            }.onFailure { status = "失败:${it.message}" }
            busy = false
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    val p = pcm
                    if (p == null) {
                        Text("从歌里截一段做来电铃声", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Text(
                            "截取 ${fmtMs((range.start * 1000).toLong())} ~ ${fmtMs((range.endInclusive * 1000).toLong())}(${(range.endInclusive - range.start).toInt()} 秒)",
                            style = MaterialTheme.typography.bodyLarge, color = palette.label
                        )
                        Spacer(Modifier.height(6.dp))
                        RangeSlider(
                            value = range,
                            onValueChange = { r ->
                                // 限制最长 40 秒
                                range = if (r.endInclusive - r.start > 40f) {
                                    if (r.start != range.start) r.start..(r.start + 40f) else (r.endInclusive - 40f)..r.endInclusive
                                } else r
                            },
                            valueRange = 0f..(p.durationMs / 1000f).coerceAtLeast(1f)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    SolidButton(onClick = { picker.launch("audio/*") }, Modifier.fillMaxWidth(), filled = pcm == null, enabled = !busy) {
                        Text(if (busy && pcm == null) "解码中…" else if (pcm == null) "选歌" else "换一首")
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
                                        val c = clip() ?: return@SolidButton
                                        playing = true
                                        player.play(c) { playing = false }
                                    }
                                },
                                Modifier.weight(1f), filled = false
                            ) { Text(if (playing) "停止" else "试听这段") }
                            Spacer(Modifier.width(8.dp))
                            SolidButton(onClick = { export(false) }, Modifier.weight(1f), filled = false, enabled = !busy) { Text("只导出") }
                        }
                        Spacer(Modifier.height(8.dp))
                        SolidButton(onClick = { export(true) }, Modifier.fillMaxWidth(), enabled = !busy) {
                            Text(if (busy) "处理中…" else "导出并设为来电铃声")
                        }
                        if (status.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.contains("失败")) palette.red else palette.green)
                        }
                    }
                }
            }
        }
    }
}
