package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * 屏幕直尺 + 量角器:用系统报告的物理 DPI 画真实刻度。
 * 手机屏幕 DPI 标称值可能有 ±3% 偏差,量小东西够用。
 */
@Composable
fun RulerToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(0) } // 0 直尺 1 量角器

    val dm = context.resources.displayMetrics
    val pxPerMm = dm.ydpi / 25.4f

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("直尺", "量角器"), mode, { mode = it }, Modifier.fillMaxWidth())
                }
            }
        }
        item {
            if (mode == 0) {
                // 直尺:左缘刻度 + 可拖动的两条测量线
                var y1 by remember { mutableFloatStateOf(150f) }
                var y2 by remember { mutableFloatStateOf(500f) }
                var dragging by remember { mutableStateOf(0) }
                GroupedCard {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(560.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (palette.isDark) Color(0xFF1A1A1E) else Color(0xFFFDFBF3))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { pos ->
                                        dragging = if (abs(pos.y - y1) < abs(pos.y - y2)) 1 else 2
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        if (dragging == 1) y1 = (y1 + amount.y).coerceIn(0f, size.height.toFloat())
                                        else y2 = (y2 + amount.y).coerceIn(0f, size.height.toFloat())
                                    }
                                )
                            }
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val ink = if (palette.isDark) Color(0xFFE8E8EC) else Color(0xFF333333)
                            var mm = 0
                            while (true) {
                                val y = mm * pxPerMm
                                if (y > size.height) break
                                val len = when {
                                    mm % 10 == 0 -> 74f
                                    mm % 5 == 0 -> 48f
                                    else -> 28f
                                }
                                drawLine(ink, Offset(0f, y), Offset(len, y), strokeWidth = if (mm % 10 == 0) 3f else 1.6f)
                                mm++
                            }
                            // 测量线
                            drawLine(Color(0xFF3478F6), Offset(0f, y1), Offset(size.width, y1), strokeWidth = 3f)
                            drawLine(Color(0xFFFF9F0A), Offset(0f, y2), Offset(size.width, y2), strokeWidth = 3f)
                        }
                        // 厘米数字
                        Box(Modifier.fillMaxSize()) {
                            var cm = 1
                            while (cm * 10 * pxPerMm < 560.dp.value * dm.density) {
                                val yDp = (cm * 10 * pxPerMm / dm.density).dp
                                Text(
                                    "$cm",
                                    Modifier.padding(start = 82.dp / 2 + 8.dp, top = yDp - 9.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryLabel
                                )
                                cm++
                            }
                        }
                        Text(
                            "%.1f mm".format(abs(y2 - y1) / pxPerMm) + "  (%.2f cm)".format(abs(y2 - y1) / pxPerMm / 10),
                            Modifier.align(Alignment.TopEnd).padding(14.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.label
                        )
                        Text(
                            "拖动蓝线和橙线,把东西贴在屏幕上量",
                            Modifier.align(Alignment.BottomEnd).padding(14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            } else {
                // 量角器:中心点 + 两条可拖动射线
                var a1 by remember { mutableFloatStateOf(180f) }  // 角度(度)
                var a2 by remember { mutableFloatStateOf(60f) }
                GroupedCard {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (palette.isDark) Color(0xFF1A1A1E) else Color(0xFFFDFBF3))
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val cx = size.width / 2f
                                    val cy = size.height * 0.78f
                                    val ang = Math.toDegrees(
                                        atan2((cy - change.position.y).toDouble(), (change.position.x - cx).toDouble())
                                    ).toFloat().let { if (it < 0) it + 360 else it }
                                    // 拖到哪条线近就动哪条
                                    fun diff(a: Float, b: Float): Float {
                                        var d = abs(a - b) % 360
                                        if (d > 180) d = 360 - d
                                        return d
                                    }
                                    if (diff(ang, a1) < diff(ang, a2)) a1 = ang else a2 = ang
                                }
                            }
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val ink = if (palette.isDark) Color(0xFFE8E8EC) else Color(0xFF333333)
                            val cx = size.width / 2f
                            val cy = size.height * 0.78f
                            val r = size.width * 0.42f
                            // 半圆刻度
                            for (d in 0..180 step 5) {
                                val rad = Math.toRadians(d.toDouble())
                                val inner = if (d % 30 == 0) r - 40f else if (d % 10 == 0) r - 26f else r - 14f
                                drawLine(
                                    ink.copy(alpha = 0.8f),
                                    Offset(cx + (inner * kotlin.math.cos(rad)).toFloat(), cy - (inner * kotlin.math.sin(rad)).toFloat()),
                                    Offset(cx + (r * kotlin.math.cos(rad)).toFloat(), cy - (r * kotlin.math.sin(rad)).toFloat()),
                                    strokeWidth = if (d % 30 == 0) 3f else 1.5f
                                )
                            }
                            drawCircle(ink, radius = 7f, center = Offset(cx, cy))
                            // 两条射线
                            for ((ang, color) in listOf(a1 to Color(0xFF3478F6), a2 to Color(0xFFFF9F0A))) {
                                val rad = Math.toRadians(ang.toDouble())
                                drawLine(
                                    color,
                                    Offset(cx, cy),
                                    Offset(cx + (r * 1.06f * kotlin.math.cos(rad)).toFloat(), cy - (r * 1.06f * kotlin.math.sin(rad)).toFloat()),
                                    strokeWidth = 5f, cap = StrokeCap.Round
                                )
                            }
                            drawCircle(ink.copy(alpha = 0.25f), radius = r, center = Offset(cx, cy), style = Stroke(2f))
                        }
                        val angle = run {
                            var d = abs(a1 - a2) % 360
                            if (d > 180) d = 360 - d
                            d
                        }
                        Text(
                            "%.1f°".format(angle),
                            Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.label
                        )
                        Text(
                            "拖两条线对齐要量的边",
                            Modifier.align(Alignment.BottomCenter).padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
    }
}
