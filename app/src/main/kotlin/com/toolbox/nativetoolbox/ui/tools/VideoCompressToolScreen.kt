package com.toolbox.nativetoolbox.ui.tools

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import com.toolbox.nativetoolbox.util.VideoTranscoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VideoCompressToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var srcUri by remember { mutableStateOf<Uri?>(null) }
    var srcW by remember { mutableIntStateOf(0) }
    var srcH by remember { mutableIntStateOf(0) }
    var srcSize by remember { mutableStateOf(0L) }
    var srcDurMs by remember { mutableStateOf(0L) }
    var cover by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var level by rememberSaveable { mutableStateOf(1) } // 0 高清 1 均衡 2 极限省
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mmr = MediaMetadataRetriever()
        runCatching { mmr.setDataSource(context, uri) }
            .onFailure { status = "视频打不开"; return@rememberLauncherForActivityResult }
        srcUri = uri
        srcW = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        srcH = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        srcDurMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
        cover = runCatching { mmr.getFrameAtTime(0) }.getOrNull()
        mmr.release()
        srcSize = FileHelper.getFileSize(context, uri)
        status = ""
    }

    fun compress() {
        val uri = srcUri ?: return
        busy = true; progress = 0; status = "压缩中…(时间约为视频时长的一半)"
        scope.launch {
            val r = withContext(Dispatchers.Default) {
                runCatching {
                    val shortSide = listOf(1080, 720, 480)[level]
                    val bitrate = listOf(4_500_000, 2_000_000, 900_000)[level]
                    val (w, h) = VideoTranscoder.fit(srcW, srcH, shortSide)
                    val tmp = File(context.cacheDir, "compress_${System.currentTimeMillis()}.mp4")
                    val ok = VideoTranscoder.transcode(context, uri, tmp, VideoTranscoder.Params(w, h, bitrate)) { p -> progress = p }
                    if (!ok) { tmp.delete(); throw Exception("转码失败,这个视频的编码可能不支持") }
                    val bytes = tmp.readBytes()
                    tmp.delete()
                    val path = FileHelper.saveToDownloads(context, "压缩_${System.currentTimeMillis()}.mp4", bytes).getOrThrow()
                    path to bytes.size.toLong()
                }
            }
            status = r.fold(
                { (path, size) ->
                    val pct = if (srcSize > 0) "省了 ${(100 - size * 100 / srcSize)}%" else ""
                    "完成!${FileHelper.formatFileSize(srcSize)} → ${FileHelper.formatFileSize(size)} $pct,已存到 $path"
                },
                { "失败:${it.message}" }
            )
            busy = false
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    val c = cover
                    if (c != null) {
                        Image(c.asImageBitmap(), contentDescription = "视频封面", modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.height(10.dp))
                    } else {
                        Text("视频太大发不出去?压一压", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    }
                    SolidButton(onClick = { picker.launch("video/*") }, Modifier.fillMaxWidth(), filled = srcUri == null, enabled = !busy) {
                        Text(if (srcUri == null) "选视频" else "换一个")
                    }
                }
            }
        }
        item {
            if (srcUri != null) {
                GroupedCard {
                    KeyValueRow("原视频", "${srcW}×${srcH} · ${FileHelper.formatFileSize(srcSize)} · ${srcDurMs / 1000} 秒", copyable = false)
                    RowDivider()
                    KeyValueRow("压缩后约", run {
                        val bitrate = listOf(4_500_000, 2_000_000, 900_000)[level]
                        val est = (bitrate / 8L + 16_000) * (srcDurMs / 1000)
                        FileHelper.formatFileSize(est)
                    }, copyable = false)
                }
            }
        }
        item {
            if (srcUri != null) {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(listOf("高清 1080p", "均衡 720p", "极省 480p"), level, { level = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        SolidButton(onClick = { compress() }, Modifier.fillMaxWidth(), enabled = !busy) {
                            Text(if (busy) "压缩中 $progress%…" else "开始压缩")
                        }
                        if (status.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("失败")) palette.red else palette.green)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("画面重新编码,声音原样保留。压缩期间别锁屏", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                    }
                }
            }
        }
    }
}
