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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import com.toolbox.nativetoolbox.util.GifEncoder
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun VideoToGifToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var retriever by remember { mutableStateOf<MediaMetadataRetriever?>(null) }
    var durationMs by remember { mutableStateOf(0L) }
    var range by remember { mutableStateOf(0f..3f) }
    var fpsIdx by rememberSaveable { mutableStateOf(1) }   // 5/8/12
    var sizeIdx by rememberSaveable { mutableStateOf(1) }  // 240/320/400
    var cover by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }

    DisposableEffect(Unit) { onDispose { runCatching { retriever?.release() } } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { retriever?.release() }
        val r = MediaMetadataRetriever()
        runCatching { r.setDataSource(context, uri) }
            .onFailure { status = "视频打不开"; return@rememberLauncherForActivityResult }
        retriever = r
        durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
        range = 0f..minOf(3f, durationMs / 1000f)
        cover = runCatching { r.getFrameAtTime(0) }.getOrNull()
        status = ""
    }

    fun make() {
        val r = retriever ?: return
        busy = true; progress = 0; status = ""
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val fps = listOf(5, 8, 12)[fpsIdx]
                    val width = listOf(240, 320, 400)[sizeIdx]
                    val startUs = (range.start * 1_000_000).toLong()
                    val endUs = (range.endInclusive * 1_000_000).toLong()
                    val frameCount = ((endUs - startUs) / 1_000_000.0 * fps).toInt().coerceIn(2, 120)
                    val stepUs = (endUs - startUs) / frameCount

                    var w = width; var h = 0
                    val buf = ByteArrayOutputStream()
                    var enc: GifEncoder? = null
                    for (i in 0 until frameCount) {
                        val t = startUs + i * stepUs
                        val frame = r.getFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: continue
                        if (h == 0) {
                            h = (width.toLong() * frame.height / frame.width).toInt().coerceAtLeast(2)
                            enc = GifEncoder(buf).also { it.start(w, h) }
                        }
                        enc!!.addFrame(frame, 1000 / fps)
                        frame.recycle()
                        progress = (i + 1) * 100 / frameCount
                    }
                    enc?.finish() ?: throw Exception("一帧都取不出来")
                    buf.toByteArray()
                }
            }
            result.onSuccess { bytes ->
                val save = withContext(Dispatchers.IO) {
                    ImageUtil.saveToPictures(context, "video_${System.currentTimeMillis()}.gif", bytes, "image/gif")
                }
                status = save.fold({ "已存到相册(${FileHelper.formatFileSize(bytes.size.toLong())})" }, { "保存失败:${it.message}" })
            }.onFailure { status = "生成失败:${it.message}" }
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
                        Text(
                            "截取 %.1fs ~ %.1fs(%.1f 秒)".format(range.start, range.endInclusive, range.endInclusive - range.start),
                            style = MaterialTheme.typography.bodyMedium, color = palette.label
                        )
                        RangeSlider(
                            value = range,
                            onValueChange = { r2 ->
                                range = if (r2.endInclusive - r2.start > 10f) {
                                    if (r2.start != range.start) r2.start..(r2.start + 10f) else (r2.endInclusive - 10f)..r2.endInclusive
                                } else r2
                            },
                            valueRange = 0f..(durationMs / 1000f).coerceAtLeast(0.5f)
                        )
                        Text("最长 10 秒(GIF 太长会巨大)", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(8.dp))
                    } else {
                        Text("视频选一段,变成能发群里的 GIF", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    }
                    SolidButton(onClick = { picker.launch("video/*") }, Modifier.fillMaxWidth(), filled = retriever == null) {
                        Text(if (retriever == null) "选视频" else "换一个")
                    }
                }
            }
        }
        item {
            if (retriever != null) {
                GroupedCard {
                    CardPadding {
                        Text("流畅度", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(6.dp))
                        SegmentedPicker(listOf("省流 5fps", "均衡 8fps", "流畅 12fps"), fpsIdx, { fpsIdx = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Text("画面宽度", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(6.dp))
                        SegmentedPicker(listOf("240", "320", "400"), sizeIdx, { sizeIdx = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        SolidButton(onClick = { make() }, Modifier.fillMaxWidth(), enabled = !busy) {
                            Text(if (busy) "生成中 $progress%…" else "生成 GIF")
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
