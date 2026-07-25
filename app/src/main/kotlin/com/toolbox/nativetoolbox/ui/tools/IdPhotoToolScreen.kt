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
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import com.toolbox.nativetoolbox.util.Matting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 规格:名称 + 宽高(mm)+ 300dpi 像素 */
private data class IdSpec(val name: String, val wMm: Int, val hMm: Int) {
    val wPx get() = wMm * 300 / 25 // 约 300dpi(25.4 取 25 简化,误差 1.6% 内)
    val hPx get() = hMm * 300 / 25
}

private val ID_SPECS = listOf(
    IdSpec("一寸", 25, 35),
    IdSpec("二寸", 35, 49),
    IdSpec("小二寸", 35, 45),
    IdSpec("大一寸", 33, 48),
)

private val ID_BG = listOf(
    "白底" to android.graphics.Color.WHITE,
    "红底" to android.graphics.Color.rgb(219, 34, 42),
    "蓝底" to android.graphics.Color.rgb(67, 142, 219),
)

/** 抠图 + 底色 + 按规格裁剪(顶部留 8% 头顶空间,人物居中) */
private fun renderIdPhoto(cut: Bitmap, spec: IdSpec, bg: Int): Bitmap {
    // 找人像包围盒(alpha > 0)
    val w = cut.width; val h = cut.height
    val px = IntArray(w * h)
    cut.getPixels(px, 0, w, 0, 0, w, h)
    var top = h; var bottom = 0; var left = w; var right = 0
    for (y in 0 until h) {
        for (x in 0 until w) {
            if ((px[y * w + x] ushr 24) > 30) {
                if (y < top) top = y
                if (y > bottom) bottom = y
                if (x < left) left = x
                if (x > right) right = x
            }
        }
    }
    if (top >= bottom || left >= right) { top = 0; bottom = h - 1; left = 0; right = w - 1 }

    val personH = bottom - top
    val targetRatio = spec.wPx.toFloat() / spec.hPx
    // 人像高度占画面 82%,头顶留白 8%
    val frameH = (personH / 0.82f)
    val frameW = frameH * targetRatio
    val cx = (left + right) / 2f
    val frameTop = top - frameH * 0.08f
    val frameLeft = cx - frameW / 2

    val out = Bitmap.createBitmap(spec.wPx, spec.hPx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    canvas.drawColor(bg)
    val srcRect = android.graphics.RectF(frameLeft, frameTop, frameLeft + frameW, frameTop + frameH)
    val m = android.graphics.Matrix()
    m.setRectToRect(srcRect, android.graphics.RectF(0f, 0f, spec.wPx.toFloat(), spec.hPx.toFloat()), android.graphics.Matrix.ScaleToFit.FILL)
    canvas.drawBitmap(cut, m, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
    return out
}

@Composable
fun IdPhotoToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cut by remember { mutableStateOf<Bitmap?>(null) }
    var specIdx by rememberSaveable { mutableStateOf(0) }
    var bgIdx by rememberSaveable { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val src = ImageUtil.loadBitmap(context, uri, 1600)
        if (src == null) { status = "图读不出来"; return@rememberLauncherForActivityResult }
        busy = true; status = ""
        scope.launch {
            val r = withContext(Dispatchers.Default) { Matting.cutout(src, threshold = 0.5f) }
            cut = r
            status = if (r == null) "没识别到人。正面、光线均匀的半身照效果最好" else ""
            busy = false
        }
    }

    val preview = remember(cut, specIdx, bgIdx) {
        cut?.let { renderIdPhoto(it, ID_SPECS[specIdx], ID_BG[bgIdx].second) }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    if (preview == null) {
                        Text("拍一张正面半身照,自动做成证件照", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(4.dp))
                        Text("要求:纯色墙前、光线均匀、露出双肩", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Image(
                            preview.asImageBitmap(), contentDescription = "证件照预览",
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    if (busy) {
                        Text("处理中…", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                        Spacer(Modifier.height(8.dp))
                    }
                    SolidButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth(), filled = cut == null, enabled = !busy) {
                        Text(if (cut == null) "选照片" else "换一张")
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.orange)
                    }
                }
            }
        }
        item {
            if (cut != null) {
                GroupedCard {
                    CardPadding {
                        Text("规格", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(6.dp))
                        SegmentedPicker(ID_SPECS.map { it.name }, specIdx, { specIdx = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Text("底色", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(6.dp))
                        SegmentedPicker(ID_BG.map { it.first }, bgIdx, { bgIdx = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        SolidButton(
                            onClick = {
                                val p = preview ?: return@SolidButton
                                scope.launch {
                                    val bytes = withContext(Dispatchers.Default) { ImageUtil.encode(p, Bitmap.CompressFormat.JPEG, 96) }
                                    val r = withContext(Dispatchers.IO) {
                                        ImageUtil.saveToPictures(context, "idphoto_${ID_SPECS[specIdx].name}_${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")
                                    }
                                    status = r.fold({ "已存到相册,冲印尺寸 ${ID_SPECS[specIdx].wMm}×${ID_SPECS[specIdx].hMm}mm" }, { "保存失败:${it.message}" })
                                }
                            },
                            Modifier.fillMaxWidth()
                        ) { Text("保存") }
                    }
                }
            }
        }
        item {
            if (cut != null) {
                GroupedCard {
                    val s = ID_SPECS[specIdx]
                    KeyValueRow("尺寸", "${s.wMm}×${s.hMm} mm(${s.wPx}×${s.hPx}px)", copyable = false)
                    RowDivider()
                    KeyValueRow("适用", listOf("简历、社保、驾照等", "护照、签证常用", "部分考试报名", "港澳通行证等")[specIdx], copyable = false)
                    RowDivider()
                    KeyValueRow("提示", "严肃场合(护照/签证)对头部比例有硬性要求,建议照相馆", copyable = false)
                }
            }
        }
    }
}
