package com.toolbox.nativetoolbox.ui.tools

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.toolbox.nativetoolbox.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun fmtDuration(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60)
    else "%d:%02d".format(s / 60, s % 60)
}

private data class VideoMeta(
    val rows: List<Pair<String, String>>,
    val thumb: android.graphics.Bitmap?,
)

private fun readMeta(context: android.content.Context, uri: Uri): VideoMeta? = runCatching {
    val rows = ArrayList<Pair<String, String>>()
    val mmr = MediaMetadataRetriever()
    mmr.setDataSource(context, uri)
    val durMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
    val w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
    val h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
    val rotation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
    val bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
    val size = FileHelper.getFileSize(context, uri)

    rows.add("时长" to fmtDuration(durMs))
    if (w != null && h != null) rows.add("分辨率" to "$w×$h" + (if (rotation != null && rotation != "0") "(旋转 $rotation°)" else ""))
    if (size > 0) rows.add("文件大小" to FileHelper.formatFileSize(size))
    if (bitrate != null) rows.add("总码率" to "${bitrate / 1000} kbps")

    // 轨道细节
    val ex = MediaExtractor()
    ex.setDataSource(context, uri, null)
    for (i in 0 until ex.trackCount) {
        val f = ex.getTrackFormat(i)
        val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
        if (mime.startsWith("video/")) {
            rows.add("视频编码" to mime.removePrefix("video/").uppercase())
            if (f.containsKey(MediaFormat.KEY_FRAME_RATE)) rows.add("帧率" to "${f.getInteger(MediaFormat.KEY_FRAME_RATE)} fps")
        } else if (mime.startsWith("audio/")) {
            rows.add("音频编码" to mime.removePrefix("audio/").uppercase())
            if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) rows.add("采样率" to "${f.getInteger(MediaFormat.KEY_SAMPLE_RATE)} Hz")
            if (f.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) rows.add("声道" to if (f.getInteger(MediaFormat.KEY_CHANNEL_COUNT) >= 2) "立体声" else "单声道")
        }
    }
    ex.release()
    val thumb = mmr.getFrameAtTime(durMs * 1000 / 4)
    mmr.release()
    VideoMeta(rows, thumb)
}.getOrNull()

@Composable
fun VideoInfoToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var meta by remember { mutableStateOf<VideoMeta?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; status = ""
        scope.launch {
            meta = withContext(Dispatchers.IO) { readMeta(context, uri) }
            if (meta == null) status = "解析失败,这个文件可能不是视频"
            busy = false
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    val t = meta?.thumb
                    if (t != null) {
                        Image(t.asImageBitmap(), contentDescription = "视频封面", modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.height(12.dp))
                    } else if (meta == null) {
                        Text("选一个视频,看编码、码率、帧率这些底细", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    }
                    SolidButton(onClick = { picker.launch("video/*") }, Modifier.fillMaxWidth(), filled = meta == null, enabled = !busy) {
                        Text(if (busy) "解析中…" else if (meta == null) "选视频" else "换一个")
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                    }
                }
            }
        }
        item { if (meta != null) SectionHeader("详细参数") }
        item {
            val m = meta
            if (m != null) {
                GroupedCard {
                    m.rows.forEachIndexed { i, (k, v) ->
                        KeyValueRow(k, v)
                        if (i != m.rows.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
