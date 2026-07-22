package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlin.math.roundToInt

/** 平铺斜排文字水印 */
private fun applyWatermark(src: Bitmap, text: String, alphaPercent: Int, sizePercent: Int): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        alpha = (alphaPercent * 255 / 100).coerceIn(20, 255)
        textSize = out.width * sizePercent / 100f / 10f * 3f
        setShadowLayer(2f, 1f, 1f, android.graphics.Color.argb(120, 0, 0, 0))
    }
    val w = paint.measureText(text)
    val stepX = w + out.width * 0.15f
    val stepY = paint.textSize * 6
    canvas.save()
    canvas.rotate(-30f, out.width / 2f, out.height / 2f)
    var y = -out.height.toFloat()
    var row = 0
    while (y < out.height * 2f) {
        var x = -out.width.toFloat() + (row % 2) * stepX / 2f
        while (x < out.width * 2f) {
            canvas.drawText(text, x, y, paint)
            x += stepX
        }
        y += stepY
        row++
    }
    canvas.restore()
    return out
}

@Composable
fun WatermarkToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var text by rememberSaveable { mutableStateOf("仅供办理XX使用") }
    var alpha by rememberSaveable { mutableStateOf(45f) }
    var sizeIndex by rememberSaveable { mutableStateOf(1) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u: Uri? ->
        if (u != null) {
            source = ImageUtil.loadBitmap(context, u, maxDim = 2560)
            preview = null
        }
    }

    fun render() {
        val src = source ?: return
        if (text.isBlank()) return
        preview = applyWatermark(src, text, alpha.roundToInt(), listOf(6, 10, 16)[sizeIndex])
    }

    fun save() {
        val bmp = preview ?: return
        val bytes = ImageUtil.encode(bmp, Bitmap.CompressFormat.JPEG, 92)
        val result = ImageUtil.saveToPictures(context, "watermark_${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")
        Toast.makeText(context, if (result.isSuccess) "已保存到相册" else "保存失败", Toast.LENGTH_SHORT).show()
    }

    ToolScaffold {
        item { SectionHeader("水印设置(证件照防盗用)") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(value = text, onValueChange = { text = it }, placeholder = "水印文字")
                    SegmentedPicker(
                        options = listOf("小", "中", "大"),
                        selectedIndex = sizeIndex,
                        onSelected = { sizeIndex = it }
                    )
                    Text("不透明度 ${alpha.roundToInt()}%", color = palette.secondaryLabel)
                    Slider(
                        value = alpha,
                        onValueChange = { alpha = it },
                        valueRange = 15f..90f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = palette.accent,
                            inactiveTrackColor = palette.fill
                        )
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterHorizontally)
                    ) {
                        SolidButton(onClick = { picker.launch("image/*") }) {
                            Text("选图", color = Color.White)
                        }
                        SolidButton(onClick = { render() }, enabled = source != null) {
                            Text("加水印", color = Color.White)
                        }
                        SolidButton(onClick = { save() }, filled = false, enabled = preview != null) {
                            Text("保存", color = palette.accent)
                        }
                    }
                }
            }
        }
        item { SectionHeader("预览") }
        item {
            GroupedCard {
                CardPadding {
                    val bmp = preview ?: source
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "预览",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Text("先选一张图", color = palette.tertiaryLabel)
                    }
                }
            }
        }
    }
}
