package com.toolbox.nativetoolbox.ui.tools

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.AudioKit
import com.toolbox.nativetoolbox.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * 无损抽音轨:AAC 音轨直接重封装成 m4a(零转码零损失);
 * 其他编码(mp3/opus 等)自动解码重编 AAC。
 */
private fun extractLossless(context: android.content.Context, uri: Uri, out: File): String? = runCatching {
    val extractor = MediaExtractor()
    extractor.setDataSource(context, uri, null)
    var track = -1
    var format: MediaFormat? = null
    for (i in 0 until extractor.trackCount) {
        val f = extractor.getTrackFormat(i)
        if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) { track = i; format = f; break }
    }
    if (track < 0 || format == null) { extractor.release(); return null }
    val mime = format.getString(MediaFormat.KEY_MIME)!!
    if (mime != MediaFormat.MIMETYPE_AUDIO_AAC) { extractor.release(); return "NEED_TRANSCODE" }

    extractor.selectTrack(track)
    val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    val dst = muxer.addTrack(format)
    muxer.start()
    val buf = ByteBuffer.allocate(1 shl 20)
    val info = MediaCodec.BufferInfo()
    while (true) {
        val n = extractor.readSampleData(buf, 0)
        if (n < 0) break
        info.offset = 0
        info.size = n
        info.presentationTimeUs = extractor.sampleTime
        info.flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
        muxer.writeSampleData(dst, buf, info)
        extractor.advance()
    }
    muxer.stop(); muxer.release(); extractor.release()
    "OK"
}.getOrNull()

@Composable
fun AudioExtractToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf<List<String>>(emptyList()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; status = "提取中…"
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val tmp = File(context.cacheDir, "extract_${System.currentTimeMillis()}.m4a")
                    when (extractLossless(context, uri, tmp)) {
                        "OK" -> {
                            val b = tmp.readBytes(); tmp.delete()
                            FileHelper.saveToDownloads(context, "提取音频_${System.currentTimeMillis()}.m4a", b).getOrThrow() to "无损"
                        }
                        "NEED_TRANSCODE" -> {
                            tmp.delete()
                            val pcm = AudioKit.decode(context, uri) ?: throw Exception("音轨解码失败")
                            val t2 = File(context.cacheDir, "extract2_${System.currentTimeMillis()}.m4a")
                            if (!AudioKit.encodeM4a(pcm, t2, 192_000)) throw Exception("编码失败")
                            val b = t2.readBytes(); t2.delete()
                            FileHelper.saveToDownloads(context, "提取音频_${System.currentTimeMillis()}.m4a", b).getOrThrow() to "转码 192k"
                        }
                        else -> throw Exception("这个文件里没有音轨")
                    }
                }
            }
            r.onSuccess { (path, how) ->
                status = ""
                done = listOf("$path($how)") + done
            }.onFailure { status = "失败:${it.message}" }
            busy = false
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Text("把视频里的声音抽出来存成音频", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                    Spacer(Modifier.height(4.dp))
                    Text("视频是 AAC 音轨时直接无损拆出,一秒钟搞定", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(12.dp))
                    SolidButton(onClick = { picker.launch("video/*") }, Modifier.fillMaxWidth(), enabled = !busy) {
                        Text(if (busy) "提取中…" else "选视频")
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                    }
                }
            }
        }
        item {
            if (done.isNotEmpty()) {
                GroupedCard {
                    done.forEachIndexed { i, d ->
                        KeyValueRow("已提取", d, copyable = false)
                        if (i != done.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
