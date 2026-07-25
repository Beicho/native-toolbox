package com.toolbox.nativetoolbox.ui.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.AudioKit
import com.toolbox.nativetoolbox.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun AudioConvertToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pcm by remember { mutableStateOf<AudioKit.Pcm?>(null) }
    var target by rememberSaveable { mutableStateOf(0) } // 0 m4a 1 wav
    var quality by rememberSaveable { mutableStateOf(1) } // 96/128/192
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; status = "解码中…"
        scope.launch {
            pcm = withContext(Dispatchers.IO) { AudioKit.decode(context, uri) }
            status = if (pcm == null) "解码失败:格式不支持或文件损坏" else ""
            busy = false
        }
    }

    fun convert() {
        val p = pcm ?: return
        busy = true; status = "转换中…"
        scope.launch {
            val r = withContext(Dispatchers.Default) {
                runCatching {
                    if (target == 1) {
                        val buf = ByteArrayOutputStream()
                        AudioKit.writeWav(p, buf)
                        FileHelper.saveToDownloads(context, "转换_${System.currentTimeMillis()}.wav", buf.toByteArray()).getOrThrow()
                    } else {
                        val bitrate = listOf(96_000, 128_000, 192_000)[quality]
                        val tmp = File(context.cacheDir, "conv_${System.currentTimeMillis()}.m4a")
                        if (!AudioKit.encodeM4a(p, tmp, bitrate)) throw Exception("编码失败")
                        val bytes = tmp.readBytes()
                        tmp.delete()
                        FileHelper.saveToDownloads(context, "转换_${System.currentTimeMillis()}.m4a", bytes).getOrThrow()
                    }
                }
            }
            status = r.fold({ "已存到 $it" }, { "失败:${it.message}" })
            busy = false
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        if (pcm == null) "把 mp3 / ogg / flac / 视频音轨转成通用格式"
                        else "已载入:${pcm!!.durationMs / 1000} 秒 · ${pcm!!.sampleRate} Hz · ${if (pcm!!.channels >= 2) "立体声" else "单声道"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (pcm == null) palette.tertiaryLabel else palette.label
                    )
                    Spacer(Modifier.height(12.dp))
                    SolidButton(onClick = { picker.launch("*/*") }, Modifier.fillMaxWidth(), filled = pcm == null, enabled = !busy) {
                        Text(if (busy && pcm == null) "解码中…" else if (pcm == null) "选音频或视频" else "换一个")
                    }
                }
            }
        }
        item {
            if (pcm != null) {
                GroupedCard {
                    CardPadding {
                        Text("输出格式", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(6.dp))
                        SegmentedPicker(listOf("m4a(通用小巧)", "wav(无损较大)"), target, { target = it }, Modifier.fillMaxWidth())
                        if (target == 0) {
                            Spacer(Modifier.height(10.dp))
                            Text("音质", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                            Spacer(Modifier.height(6.dp))
                            SegmentedPicker(listOf("96k", "128k", "192k"), quality, { quality = it }, Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(12.dp))
                        SolidButton(onClick = { convert() }, Modifier.fillMaxWidth(), enabled = !busy) {
                            Text(if (busy) "转换中…" else "开始转换")
                        }
                        if (status.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.contains("失败")) palette.red else palette.green)
                        }
                    }
                }
            }
        }
        item {
            GroupedCard {
                KeyValueRow("能读什么", "mp3 / aac / m4a / ogg / flac / wav / amr,以及视频的声音", copyable = false)
                RowDivider()
                KeyValueRow("出什么", "m4a(AAC)或 wav。微信 QQ 剪映都认", copyable = false)
            }
        }
    }
}
