package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs

/** 计算差异统计:逐像素比较(缩到同尺寸) */
private fun diffStats(a: Bitmap, b: Bitmap): Pair<Double, Bitmap> {
    val w = 480.coerceAtMost(a.width)
    val h = (w.toLong() * a.height / a.width).toInt().coerceAtLeast(1)
    val sa = Bitmap.createScaledBitmap(a, w, h, true)
    val sb = Bitmap.createScaledBitmap(b, w, h, true)
    val pa = IntArray(w * h); val pb = IntArray(w * h)
    sa.getPixels(pa, 0, w, 0, 0, w, h)
    sb.getPixels(pb, 0, w, 0, 0, w, h)
    val heat = IntArray(w * h)
    var diffCount = 0L
    for (i in pa.indices) {
        val d = (abs((pa[i] shr 16 and 0xFF) - (pb[i] shr 16 and 0xFF)) +
            abs((pa[i] shr 8 and 0xFF) - (pb[i] shr 8 and 0xFF)) +
            abs((pa[i] and 0xFF) - (pb[i] and 0xFF))) / 3
        if (d > 16) {
            diffCount++
            heat[i] = 0xFFFF3B30.toInt()
        } else {
            // 原图淡化
            val g = ((pa[i] shr 16 and 0xFF) + (pa[i] shr 8 and 0xFF) + (pa[i] and 0xFF)) / 3
            val gg = (g * 0.35 + 165).toInt()
            heat[i] = (0xFF shl 24) or (gg shl 16) or (gg shl 8) or gg
        }
    }
    val heatBmp = Bitmap.createBitmap(heat, w, h, Bitmap.Config.ARGB_8888)
    return (diffCount * 100.0 / pa.size) to heatBmp
}

@Composable
fun ImageCompareToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var imgA by remember { mutableStateOf<Bitmap?>(null) }
    var imgB by remember { mutableStateOf<Bitmap?>(null) }
    var mode by rememberSaveable { mutableStateOf(0) } // 0 滑动对比 1 差异高亮
    var split by remember { mutableFloatStateOf(0.5f) }
    var picking by remember { mutableStateOf(0) }
    var stats by remember { mutableStateOf<Pair<Double, Bitmap>?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bmp = ImageUtil.loadBitmap(context, uri, 1600) ?: return@rememberLauncherForActivityResult
        if (picking == 0) imgA = bmp else imgB = bmp
        stats = null
        val a = imgA; val b = imgB
        if (a != null && b != null && mode == 1) stats = diffStats(a, b)
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Row(Modifier.fillMaxWidth()) {
                        SolidButton(onClick = { picking = 0; picker.launch("image/*") }, Modifier.weight(1f), filled = imgA == null) {
                            Text(if (imgA == null) "选图 A" else "换图 A")
                        }
                        Spacer(Modifier.size(8.dp))
                        SolidButton(onClick = { picking = 1; picker.launch("image/*") }, Modifier.weight(1f), filled = imgB == null) {
                            Text(if (imgB == null) "选图 B" else "换图 B")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    SegmentedPicker(listOf("滑动对比", "差异高亮"), mode, {
                        mode = it
                        val a = imgA; val b = imgB
                        if (it == 1 && a != null && b != null && stats == null) stats = diffStats(a, b)
                    }, Modifier.fillMaxWidth())
                }
            }
        }
        item {
            val a = imgA; val b = imgB
            if (a != null && b != null) {
                if (mode == 0) {
                    GroupedCard {
                        val ratio = a.width.toFloat() / a.height
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio.coerceIn(0.4f, 2.4f))
                                .clip(RoundedCornerShape(12.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        split = (change.position.x / size.width).coerceIn(0.02f, 0.98f)
                                    }
                                }
                        ) {
                            Canvas(Modifier.fillMaxWidth().aspectRatio(ratio.coerceIn(0.4f, 2.4f))) {
                                val dst = Rect(0f, 0f, size.width, size.height)
                                drawIntoCanvas { c ->
                                    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
                                    // B 铺满
                                    c.nativeCanvas.drawBitmap(b, null, android.graphics.RectF(dst.left, dst.top, dst.right, dst.bottom), paint)
                                    // A 只画左半
                                    clipRect(0f, 0f, size.width * split, size.height) {
                                        drawIntoCanvas { c2 ->
                                            c2.nativeCanvas.drawBitmap(a, null, android.graphics.RectF(dst.left, dst.top, dst.right, dst.bottom), paint)
                                        }
                                    }
                                }
                                // 分割线
                                drawLine(
                                    Color.White,
                                    Offset(size.width * split, 0f),
                                    Offset(size.width * split, size.height),
                                    strokeWidth = 4f, cap = StrokeCap.Round
                                )
                                drawCircle(Color.White, 15f, Offset(size.width * split, size.height / 2))
                            }
                            Text("A", Modifier, color = Color.White, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                } else {
                    val s = stats
                    if (s != null) {
                        GroupedCard {
                            CardPadding {
                                androidx.compose.foundation.Image(
                                    s.second.asImageBitmap(), contentDescription = "差异图",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                                )
                            }
                        }
                        GroupedCard {
                            KeyValueRow("不同的区域", "%.1f%%(红色部分)".format(s.first), copyable = false)
                            RowDivider()
                            KeyValueRow("判定", if (s.first < 0.5) "基本是同一张图" else if (s.first < 8) "小改动" else "差别很大", copyable = false)
                        }
                    }
                }
            } else {
                GroupedCard {
                    CardPadding {
                        Text("选两张图:找茬、对比修图前后、验证是否同图都行", style = MaterialTheme.typography.bodyMedium, color = palette.tertiaryLabel)
                    }
                }
            }
        }
    }
}
