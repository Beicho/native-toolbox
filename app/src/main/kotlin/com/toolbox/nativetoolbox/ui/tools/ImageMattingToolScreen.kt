package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
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
import com.toolbox.nativetoolbox.util.ImageUtil
import com.toolbox.nativetoolbox.util.Matting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val MAT_BG = listOf(
    "透明" to android.graphics.Color.TRANSPARENT,
    "白" to android.graphics.Color.WHITE,
    "红" to android.graphics.Color.rgb(219, 34, 42),
    "蓝" to android.graphics.Color.rgb(67, 142, 219),
    "绿" to android.graphics.Color.rgb(80, 175, 90),
)

@Composable
fun ImageMattingToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var cut by remember { mutableStateOf<Bitmap?>(null) }
    var bgIdx by rememberSaveable { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        source = ImageUtil.loadBitmap(context, uri, 1600)
        cut = null
        status = if (source == null) "图读不出来" else ""
        val s = source ?: return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val r = withContext(Dispatchers.Default) { Matting.cutout(s) }
            cut = r
            status = if (r == null) "没识别出人像。这个功能对「人」效果最好,物体建议用打码涂抹" else ""
            busy = false
        }
    }

    val display = remember(cut, bgIdx) {
        cut?.let { if (MAT_BG[bgIdx].second == android.graphics.Color.TRANSPARENT) it else Matting.compose(it, MAT_BG[bgIdx].second) }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    val d = display ?: source
                    if (d == null) {
                        Text("选一张有人的照片,自动把人抠出来", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Image(d.asImageBitmap(), contentDescription = "抠图预览", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                        Spacer(Modifier.height(12.dp))
                    }
                    if (busy) {
                        Text("抠图中…", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                        Spacer(Modifier.height(8.dp))
                    }
                    SolidButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth(), filled = source == null, enabled = !busy) {
                        Text(if (source == null) "选照片" else "换一张")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("全程手机本地处理,照片不上传", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                }
            }
        }
        item {
            if (cut != null) {
                GroupedCard {
                    CardPadding {
                        Text("背景", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(6.dp))
                        SegmentedPicker(MAT_BG.map { it.first }, bgIdx, { bgIdx = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        SolidButton(
                            onClick = {
                                val d = display ?: return@SolidButton
                                scope.launch {
                                    val transparent = MAT_BG[bgIdx].second == android.graphics.Color.TRANSPARENT
                                    val bytes = withContext(Dispatchers.Default) {
                                        ImageUtil.encode(d, if (transparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95)
                                    }
                                    val r = withContext(Dispatchers.IO) {
                                        ImageUtil.saveToPictures(
                                            context,
                                            "cutout_${System.currentTimeMillis()}.${if (transparent) "png" else "jpg"}",
                                            bytes, if (transparent) "image/png" else "image/jpeg"
                                        )
                                    }
                                    status = r.fold({ "已存到相册" }, { "保存失败:${it.message}" })
                                }
                            },
                            Modifier.fillMaxWidth()
                        ) { Text("保存") }
                        if (status.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("已存")) palette.green else palette.orange)
                        }
                    }
                }
            }
        }
    }
}
