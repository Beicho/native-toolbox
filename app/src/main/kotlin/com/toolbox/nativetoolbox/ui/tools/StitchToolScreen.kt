package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import com.toolbox.nativetoolbox.util.ImageUtil

/** 统一宽度后竖向拼接,限高防 OOM */
private fun stitch(bitmaps: List<Bitmap>): Bitmap? {
    if (bitmaps.isEmpty()) return null
    val width = 1080
    val scaled = bitmaps.map {
        val h = (it.height.toFloat() * width / it.width).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(it, width, h, true)
    }
    val totalH = scaled.sumOf { it.height }
    if (totalH > 20000) return null // 防超长 OOM
    val out = Bitmap.createBitmap(width, totalH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    var y = 0f
    scaled.forEach {
        canvas.drawBitmap(it, 0f, y, null)
        y += it.height
    }
    return out
}

@Composable
fun StitchToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var count by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf<Bitmap?>(null) }
    var tooLong by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            tooLong = false
            count = uris.size
            val bitmaps = uris.mapNotNull { ImageUtil.loadBitmap(context, it, maxDim = 2048) }
            result = stitch(bitmaps)
            if (result == null && bitmaps.isNotEmpty()) tooLong = true
        }
    }

    fun save() {
        val bmp = result ?: return
        val bytes = ImageUtil.encode(bmp, Bitmap.CompressFormat.JPEG, 90)
        val r = ImageUtil.saveToPictures(context, "stitch_${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")
        Toast.makeText(context, if (r.isSuccess) "已保存长图(${FileHelper.formatFileSize(bytes.size.toLong())})" else "保存失败", Toast.LENGTH_SHORT).show()
    }

    ToolScaffold {
        item { SectionHeader("长截图拼接(按选择顺序从上到下)") }
        item {
            GroupedCard {
                CardPadding {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        SolidButton(onClick = { picker.launch("image/*") }) {
                            Text("多选图片", color = Color.White)
                        }
                        SolidButton(onClick = { save() }, filled = false, enabled = result != null) {
                            Text("保存长图", color = palette.accent)
                        }
                    }
                    if (tooLong) {
                        Text("拼接后过长(超 2 万像素),减少几张试试", color = palette.red)
                    }
                }
                KeyValueRow("已选", if (count > 0) "$count 张" else "", copyable = false)
            }
        }
        item { SectionHeader("预览") }
        item {
            GroupedCard {
                CardPadding {
                    val bmp = result
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "拼接预览",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 600.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Text("选 2 张以上截图自动拼接", color = palette.tertiaryLabel)
                    }
                }
            }
        }
    }
}
