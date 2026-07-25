package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BG_COLORS = listOf(
    "白" to AColor.WHITE, "黑" to AColor.BLACK, "米" to AColor.rgb(245, 240, 228),
    "灰" to AColor.rgb(240, 240, 244), "粉" to AColor.rgb(252, 228, 236), "透明" to AColor.TRANSPARENT,
)

/** 圆角 + 内边距背景 + 阴影 */
private fun renderBorder(src: Bitmap, cornerPct: Float, padPct: Float, bg: Int, shadow: Boolean): Bitmap {
    val pad = (src.width * padPct / 100).toInt()
    val w = src.width + pad * 2
    val h = src.height + pad * 2
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    if (bg != AColor.TRANSPARENT) canvas.drawColor(bg)
    val radius = src.width * cornerPct / 100
    val rect = RectF(pad.toFloat(), pad.toFloat(), (pad + src.width).toFloat(), (pad + src.height).toFloat())
    if (shadow && pad > 4) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.argb(70, 0, 0, 0)
            maskFilter = android.graphics.BlurMaskFilter(pad * 0.45f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(RectF(rect).apply { offset(0f, pad * 0.18f) }, radius, radius, shadowPaint)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(android.graphics.Matrix().apply { setTranslate(pad.toFloat(), pad.toFloat()) })
        }
    }
    canvas.drawRoundRect(rect, radius, radius, paint)
    return out
}

@Composable
fun ImageBorderToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var corner by rememberSaveable { mutableFloatStateOf(6f) }
    var padding by rememberSaveable { mutableFloatStateOf(6f) }
    var bgIdx by rememberSaveable { mutableStateOf(0) }
    var shadow by rememberSaveable { mutableStateOf(true) }
    var status by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            source = ImageUtil.loadBitmap(context, uri, 2400)
            status = if (source == null) "图读不出来" else ""
        }
    }

    val previewSrc = remember(source) {
        source?.let { if (it.width > 800) Bitmap.createScaledBitmap(it, 800, (800L * it.height / it.width).toInt(), true) else it }
    }
    val preview = remember(previewSrc, corner, padding, bgIdx, shadow) {
        previewSrc?.let { renderBorder(it, corner, padding, BG_COLORS[bgIdx].second, shadow) }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    if (preview == null) {
                        Text("给截图和照片加圆角、留白和投影,发出去更精致", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Image(preview.asImageBitmap(), contentDescription = "边框预览", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                        Spacer(Modifier.height(12.dp))
                    }
                    SolidButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth(), filled = source == null) {
                        Text(if (source == null) "选图" else "换一张")
                    }
                }
            }
        }
        item {
            if (source != null) {
                GroupedCard {
                    CardPadding {
                        Text("背景色", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(6.dp))
                        SegmentedPicker(BG_COLORS.map { it.first }, bgIdx, { bgIdx = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("圆角", style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.size(width = 40.dp, height = 22.dp))
                            Slider(corner, { corner = it }, valueRange = 0f..20f, modifier = Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("留白", style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.size(width = 40.dp, height = 22.dp))
                            Slider(padding, { padding = it }, valueRange = 0f..18f, modifier = Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("投影", style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.weight(1f))
                            androidx.compose.material3.Switch(checked = shadow, onCheckedChange = { shadow = it })
                        }
                        Spacer(Modifier.height(10.dp))
                        SolidButton(
                            onClick = {
                                val s = source ?: return@SolidButton
                                scope.launch {
                                    val bytes = withContext(Dispatchers.Default) {
                                        val full = renderBorder(s, corner, padding, BG_COLORS[bgIdx].second, shadow)
                                        val png = bgIdx == BG_COLORS.lastIndex || corner > 0
                                        val b = ImageUtil.encode(full, if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95)
                                        full.recycle(); b
                                    }
                                    val png = bgIdx == BG_COLORS.lastIndex || corner > 0
                                    val r = withContext(Dispatchers.IO) {
                                        ImageUtil.saveToPictures(context, "border_${System.currentTimeMillis()}.${if (png) "png" else "jpg"}", bytes, if (png) "image/png" else "image/jpeg")
                                    }
                                    status = r.fold({ "已存到相册" }, { "保存失败:${it.message}" })
                                }
                            },
                            Modifier.fillMaxWidth()
                        ) { Text("保存") }
                        if (status.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("已存")) palette.green else palette.red)
                        }
                    }
                }
            }
        }
    }
}
