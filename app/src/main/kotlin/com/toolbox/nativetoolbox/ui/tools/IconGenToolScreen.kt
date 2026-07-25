package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class IconSpec(val label: String, val px: Int)

private val ANDROID_SPECS = listOf(
    IconSpec("mdpi", 48), IconSpec("hdpi", 72), IconSpec("xhdpi", 96),
    IconSpec("xxhdpi", 144), IconSpec("xxxhdpi", 192), IconSpec("商店图 512", 512),
)
private val IOS_SPECS = listOf(
    IconSpec("40", 40), IconSpec("58", 58), IconSpec("60", 60), IconSpec("80", 80),
    IconSpec("87", 87), IconSpec("120", 120), IconSpec("180", 180), IconSpec("1024", 1024),
)

/** 方图 → 指定尺寸,shape: 0 原样 1 圆角(22%) 2 圆形 */
private fun renderIcon(src: Bitmap, px: Int, shape: Int): Bitmap {
    val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // 先裁成正方形再缩放
    val side = minOf(src.width, src.height)
    val sx = (src.width - side) / 2
    val sy = (src.height - side) / 2
    val square = Bitmap.createBitmap(src, sx, sy, side, side)
    val scaled = Bitmap.createScaledBitmap(square, px, px, true)
    when (shape) {
        1 -> {
            val r = px * 0.22f
            canvas.drawRoundRect(RectF(0f, 0f, px.toFloat(), px.toFloat()), r, r, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
        }
        2 -> {
            canvas.drawCircle(px / 2f, px / 2f, px / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
        }
        else -> canvas.drawBitmap(scaled, 0f, 0f, paint)
    }
    if (scaled !== out) scaled.recycle()
    if (square !== src && square !== scaled) square.recycle()
    return out
}

@Composable
fun IconGenToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var platform by rememberSaveable { mutableStateOf(0) } // 0 Android 1 iOS
    var shape by rememberSaveable { mutableStateOf(1) }
    var status by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            source = ImageUtil.loadBitmap(context, uri, 2048)
            status = if (source == null) "这张图读不出来,换一张试试" else ""
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    val bmp = source
                    if (bmp == null) {
                        Text("选一张图(最好是正方形、512 以上)", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            val preview = remember(bmp, shape) { renderIcon(bmp, 192, shape) }
                            Image(preview.asImageBitmap(), contentDescription = "图标预览", modifier = Modifier.size(84.dp))
                            Text("${bmp.width}×${bmp.height}\n导出会自动裁成正方形", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SolidButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth(), filled = source == null) {
                        Text(if (source == null) "选图" else "换一张")
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text("平台", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(6.dp))
                    SegmentedPicker(listOf("Android", "iOS"), platform, { platform = it }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Text("形状", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(6.dp))
                    SegmentedPicker(listOf("方形", "圆角", "圆形"), shape, { shape = it }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    SolidButton(
                        onClick = {
                            val bmp = source ?: return@SolidButton
                            saving = true; status = ""
                            scope.launch {
                                val specs = if (platform == 0) ANDROID_SPECS else IOS_SPECS
                                var ok = 0
                                withContext(Dispatchers.IO) {
                                    for (spec in specs) {
                                        val icon = renderIcon(bmp, spec.px, shape)
                                        val bytes = ImageUtil.encode(icon, Bitmap.CompressFormat.PNG, 100)
                                        icon.recycle()
                                        val name = "icon_${spec.px}" + (if (platform == 0 && spec.px != 512) "_${spec.label}" else "") + ".png"
                                        if (ImageUtil.saveToPictures(context, name, bytes, "image/png").isSuccess) ok++
                                    }
                                }
                                status = if (ok == specs.size) "已保存 $ok 张到相册 AstroKit 目录" else "保存了 $ok/${specs.size} 张,部分失败"
                                saving = false
                            }
                        },
                        Modifier.fillMaxWidth(),
                        enabled = source != null && !saving
                    ) { Text(if (saving) "导出中…" else "一键导出全套") }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("已保存")) palette.green else palette.red)
                    }
                }
            }
        }
        item { SectionHeader(if (platform == 0) "会导出这些尺寸" else "会导出这些尺寸(pt 对应像素)") }
        item {
            GroupedCard {
                val specs = if (platform == 0) ANDROID_SPECS else IOS_SPECS
                specs.forEachIndexed { i, s ->
                    KeyValueRow(s.label, "${s.px}×${s.px}", copyable = false)
                    if (i != specs.lastIndex) RowDivider()
                }
            }
        }
    }
}
