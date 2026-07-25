package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
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

/** 在 bitmap 上把 (cx,cy,r) 圆内做块状马赛克(块大小 block px) */
private fun mosaicCircle(bmp: Bitmap, cx: Int, cy: Int, r: Int, block: Int) {
    val x0 = ((cx - r) / block * block).coerceAtLeast(0)
    val y0 = ((cy - r) / block * block).coerceAtLeast(0)
    val x1 = (cx + r).coerceAtMost(bmp.width - 1)
    val y1 = (cy + r).coerceAtMost(bmp.height - 1)
    var by = y0
    while (by <= y1) {
        var bx = x0
        while (bx <= x1) {
            val bw = block.coerceAtMost(bmp.width - bx)
            val bh = block.coerceAtMost(bmp.height - by)
            if (bw > 0 && bh > 0) {
                val ccx = bx + bw / 2; val ccy = by + bh / 2
                val dx = ccx - cx; val dy = ccy - cy
                if (dx * dx + dy * dy <= r * r) {
                    val c = bmp.getPixel(bx + bw / 2, by + bh / 2)
                    for (y in by until by + bh) for (x in bx until bx + bw) bmp.setPixel(x, y, c)
                }
            }
            bx += block
        }
        by += block
    }
}

@Composable
fun ImageMosaicToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf<Bitmap?>(null) }  // 直接在这张上涂
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var brush by remember { mutableFloatStateOf(36f) }
    var version by remember { mutableIntStateOf(0) }  // 触发重绘
    var status by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val b = ImageUtil.loadBitmap(context, uri, 1920)
            original = b
            working = b?.copy(Bitmap.Config.ARGB_8888, true)
            version++
            status = if (b == null) "图读不出来" else ""
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    val w = working
                    if (w == null) {
                        Text("选一张图,手指划过的地方打码", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        val ratio = w.width.toFloat() / w.height
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio.coerceIn(0.4f, 2.4f))
                                .clip(RoundedCornerShape(12.dp))
                                .pointerInput(w) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        // 视图坐标 → 图片坐标
                                        val scale = w.width.toFloat() / size.width
                                        val ix = (change.position.x * scale).toInt().coerceIn(0, w.width - 1)
                                        val iy = (change.position.y * (w.height.toFloat() / size.height)).toInt().coerceIn(0, w.height - 1)
                                        val r = (brush * scale).toInt().coerceAtLeast(8)
                                        val block = (r / 3).coerceAtLeast(8)
                                        mosaicCircle(w, ix, iy, r, block)
                                        version++
                                    }
                                }
                        ) {
                            @Suppress("UNUSED_EXPRESSION") version
                            Image(w.asImageBitmap(), contentDescription = "打码画布", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    SolidButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth(), filled = working == null) {
                        Text(if (working == null) "选图" else "换一张")
                    }
                }
            }
        }
        item {
            if (working != null) {
                GroupedCard {
                    CardPadding {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("笔刷", style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.size(width = 40.dp, height = 22.dp))
                            Slider(brush, { brush = it }, valueRange = 16f..90f, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth()) {
                            SolidButton(
                                onClick = {
                                    working = original?.copy(Bitmap.Config.ARGB_8888, true)
                                    version++
                                },
                                Modifier.weight(1f), filled = false
                            ) { Text("全部还原") }
                            Spacer(Modifier.size(8.dp))
                            SolidButton(
                                onClick = {
                                    val w = working ?: return@SolidButton
                                    scope.launch {
                                        val bytes = withContext(Dispatchers.Default) { ImageUtil.encode(w, Bitmap.CompressFormat.JPEG, 93) }
                                        val r = withContext(Dispatchers.IO) {
                                            ImageUtil.saveToPictures(context, "mosaic_${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")
                                        }
                                        status = r.fold({ "已存到相册" }, { "保存失败:${it.message}" })
                                    }
                                },
                                Modifier.weight(2f)
                            ) { Text("保存") }
                        }
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
