package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.mutableLongStateOf
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
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VideoFrameToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var retriever by remember { mutableStateOf<MediaMetadataRetriever?>(null) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var posMs by remember { mutableFloatStateOf(0f) }
    var frame by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { runCatching { retriever?.release() } } }

    fun grab(precise: Boolean) {
        val r = retriever ?: return
        busy = true
        scope.launch {
            frame = withContext(Dispatchers.IO) {
                runCatching {
                    r.getFrameAtTime(
                        (posMs * 1000).toLong(),
                        if (precise) MediaMetadataRetriever.OPTION_CLOSEST else MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                }.getOrNull()
            }
            if (frame == null) status = "这个时间点取不到画面"
            busy = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { retriever?.release() }
        status = ""
        val r = MediaMetadataRetriever()
        runCatching { r.setDataSource(context, uri) }
            .onFailure { status = "视频打不开"; return@rememberLauncherForActivityResult }
        retriever = r
        durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
        posMs = 0f
        grab(false)
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    val f = frame
                    if (f != null) {
                        Image(f.asImageBitmap(), contentDescription = "当前帧", modifier = Modifier.fillMaxWidth().height(230.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.height(10.dp))
                    } else {
                        Text("从视频里精准截出高清单帧,比截屏清楚", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    }
                    SolidButton(onClick = { picker.launch("video/*") }, Modifier.fillMaxWidth(), filled = retriever == null) {
                        Text(if (retriever == null) "选视频" else "换一个")
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                    }
                }
            }
        }
        item {
            if (retriever != null && durationMs > 0) {
                GroupedCard {
                    CardPadding {
                        Text(
                            "位置 %d:%02d.%d / %d:%02d".format(
                                (posMs / 60000).toInt(), (posMs / 1000 % 60).toInt(), (posMs % 1000 / 100).toInt(),
                                durationMs / 60000, durationMs / 1000 % 60
                            ),
                            style = MaterialTheme.typography.bodyMedium, color = palette.label
                        )
                        Slider(posMs, { posMs = it }, valueRange = 0f..durationMs.toFloat(), onValueChangeFinished = { grab(false) }, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth()) {
                            SolidButton(onClick = { posMs = (posMs - 100).coerceAtLeast(0f); grab(true) }, Modifier.weight(1f), filled = false) { Text("-0.1s") }
                            Spacer(Modifier.width(6.dp))
                            SolidButton(onClick = { posMs = (posMs + 100).coerceAtMost(durationMs.toFloat()); grab(true) }, Modifier.weight(1f), filled = false) { Text("+0.1s") }
                            Spacer(Modifier.width(6.dp))
                            SolidButton(onClick = { grab(true) }, Modifier.weight(1f), filled = false, enabled = !busy) { Text("精确取帧") }
                        }
                        Spacer(Modifier.height(8.dp))
                        SolidButton(
                            onClick = {
                                val f = frame ?: return@SolidButton
                                scope.launch {
                                    val bytes = withContext(Dispatchers.Default) { ImageUtil.encode(f, Bitmap.CompressFormat.JPEG, 95) }
                                    val r = withContext(Dispatchers.IO) {
                                        ImageUtil.saveToPictures(context, "frame_${(posMs / 1000).toInt()}s_${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")
                                    }
                                    status = r.fold({ "已存到相册" }, { "保存失败:${it.message}" })
                                }
                            },
                            Modifier.fillMaxWidth(), enabled = frame != null
                        ) { Text("保存这一帧") }
                        if (status.startsWith("已存")) {
                            Spacer(Modifier.height(6.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.green)
                        }
                    }
                }
            }
        }
    }
}
