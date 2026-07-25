package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
fun GifMakeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var frames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var speedIdx by rememberSaveable { mutableStateOf(1) } // 慢/中/快
    var sizeIdx by rememberSaveable { mutableStateOf(1) }  // 240/360/480
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                uris.mapNotNull { ImageUtil.loadBitmap(context, it, 1080) }
            }
            frames = frames + loaded
            status = if (loaded.size < uris.size) "有 ${uris.size - loaded.size} 张读不出来,已跳过" else ""
        }
    }

    fun export() {
        if (frames.size < 2) { status = "至少要两张图才能动起来"; return }
        busy = true; status = "合成中…"
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val delay = listOf(600, 300, 120)[speedIdx]
                    val target = listOf(240, 360, 480)[sizeIdx]
                    val first = frames.first()
                    val w = target
                    val h = (target.toLong() * first.height / first.width).toInt().coerceAtLeast(1)
                    val buf = ByteArrayOutputStream()
                    val enc = GifEncoder(buf)
                    enc.start(w, h)
                    for (f in frames) enc.addFrame(f, delay)
                    enc.finish()
                    buf.toByteArray()
                }.getOrNull()
            }
            if (result == null) {
                status = "合成失败,试试减少几张图"
            } else {
                val r = withContext(Dispatchers.IO) {
                    ImageUtil.saveToPictures(context, "gif_${System.currentTimeMillis()}.gif", result, "image/gif")
                }
                status = r.fold({ "已存到相册($it),${FileHelper.formatFileSize(result.size.toLong())}" }, { "保存失败:${it.message}" })
            }
            busy = false
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    if (frames.isEmpty()) {
                        Text("选几张图,按顺序拼成一张会动的 GIF", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                    } else {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            frames.forEachIndexed { i, f ->
                                Image(
                                    f.asImageBitmap(), contentDescription = "第 ${i + 1} 帧",
                                    modifier = Modifier.size(76.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("${frames.size} 帧,按选择顺序播放", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(onClick = { picker.launch("image/*") }, Modifier.weight(1f), filled = frames.isEmpty()) {
                            Text(if (frames.isEmpty()) "选图" else "继续加")
                        }
                        if (frames.isNotEmpty()) {
                            SolidButton(onClick = { frames = emptyList(); status = "" }, Modifier.weight(1f), filled = false) { Text("清空") }
                        }
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text("播放速度", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(6.dp))
                    SegmentedPicker(listOf("慢", "中", "快"), speedIdx, { speedIdx = it }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Text("画面宽度", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(6.dp))
                    SegmentedPicker(listOf("240", "360", "480"), sizeIdx, { sizeIdx = it }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    SolidButton(onClick = { export() }, Modifier.fillMaxWidth(), enabled = frames.size >= 2 && !busy) {
                        Text(if (busy) "合成中…" else "合成 GIF")
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("已存")) palette.green else palette.secondaryLabel)
                    }
                }
            }
        }
    }
}
