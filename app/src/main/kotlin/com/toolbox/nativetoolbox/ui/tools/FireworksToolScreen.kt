package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---- 粒子模型(普通类,不进 Compose 状态,由帧循环驱动) ----

private class Spark(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var life: Float,          // 1 → 0
    val decay: Float,
    val color: Color,
    val trail: Boolean,       // 柳条效果:长拖影
)

private class Rocket(
    var x: Float, var y: Float,
    var vy: Float,
    val targetY: Float,
    val color: Color,
)

private val FIREWORK_COLORS = listOf(
    Color(0xFFFF5252), Color(0xFFFFB300), Color(0xFFFFF176), Color(0xFF69F0AE),
    Color(0xFF40C4FF), Color(0xFFB388FF), Color(0xFFFF80AB), Color(0xFFFFD180),
    Color(0xFF80D8FF), Color(0xFFCCFF90),
)

private fun explode(sparks: MutableList<Spark>, x: Float, y: Float, rnd: Random) {
    val kind = rnd.nextInt(4)
    val c1 = FIREWORK_COLORS[rnd.nextInt(FIREWORK_COLORS.size)]
    val c2 = FIREWORK_COLORS[rnd.nextInt(FIREWORK_COLORS.size)]
    when (kind) {
        0 -> { // 球形
            repeat(110) {
                val a = rnd.nextFloat() * 2f * PI.toFloat()
                val sp = 2f + rnd.nextFloat() * 9f
                sparks.add(Spark(x, y, cos(a) * sp, sin(a) * sp, 1f, 0.012f + rnd.nextFloat() * 0.010f, c1, false))
            }
        }
        1 -> { // 双色双环
            repeat(60) {
                val a = it / 60f * 2f * PI.toFloat()
                sparks.add(Spark(x, y, cos(a) * 7.5f, sin(a) * 7.5f, 1f, 0.014f, c1, false))
            }
            repeat(42) {
                val a = it / 42f * 2f * PI.toFloat()
                sparks.add(Spark(x, y, cos(a) * 4.2f, sin(a) * 4.2f, 1f, 0.013f, c2, false))
            }
        }
        2 -> { // 柳条垂落
            repeat(80) {
                val a = rnd.nextFloat() * 2f * PI.toFloat()
                val sp = 1.5f + rnd.nextFloat() * 5.5f
                sparks.add(Spark(x, y, cos(a) * sp, sin(a) * sp - 1f, 1f, 0.006f + rnd.nextFloat() * 0.004f, Color(0xFFFFE082), true))
            }
        }
        else -> { // 蜂群乱窜
            repeat(90) {
                val a = rnd.nextFloat() * 2f * PI.toFloat()
                val sp = rnd.nextFloat() * 10f
                sparks.add(Spark(x, y, cos(a) * sp, sin(a) * sp, 1f, 0.016f + rnd.nextFloat() * 0.012f, if (rnd.nextBoolean()) c1 else c2, false))
            }
        }
    }
}

@Composable
fun FireworksToolScreen(onBack: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    var auto by remember { mutableStateOf(true) }
    var frame by remember { mutableLongStateOf(0L) }

    val rockets = remember { mutableListOf<Rocket>() }
    val sparks = remember { mutableListOf<Spark>() }
    val rnd = remember { Random(System.nanoTime()) }
    val canvasSize = remember { FloatArray(2) } // 非 state:draw 阶段写入不触发重组
    var lastAuto = remember { longArrayOf(0L) }

    // 帧循环:驱动物理并强制重绘(dt 归一化为 60fps 帧数)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            var boom = false
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 16_666_667.0).toFloat().coerceAtMost(3f)
                    val itR = rockets.iterator()
                    while (itR.hasNext()) {
                        val r = itR.next()
                        r.y += r.vy * dt
                        if (r.y <= r.targetY) {
                            explode(sparks, r.x, r.y, rnd)
                            boom = true
                            itR.remove()
                        }
                    }
                    val itS = sparks.iterator()
                    while (itS.hasNext()) {
                        val s = itS.next()
                        s.x += s.vx * dt
                        s.y += s.vy * dt
                        s.vy += 0.11f * dt          // 重力
                        s.vx *= (1f - 0.012f * dt)  // 阻力
                        s.vy *= (1f - 0.006f * dt)
                        s.life -= s.decay * dt
                        if (s.life <= 0f) itS.remove()
                    }
                    if (auto && canvasSize[0] > 0 && now - lastAuto[0] > 900_000_000L + rnd.nextLong(700_000_000L)) {
                        lastAuto[0] = now
                        val x = canvasSize[0] * (0.15f + rnd.nextFloat() * 0.7f)
                        val ty = canvasSize[1] * (0.12f + rnd.nextFloat() * 0.35f)
                        rockets.add(Rocket(x, canvasSize[1], -(9f + rnd.nextFloat() * 4f), ty, Color.White))
                    }
                }
                last = now
                frame = now
            }
            if (boom) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    ToolScaffold {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF060915))
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            rockets.add(Rocket(pos.x, size.height.toFloat(), -(10f + rnd.nextFloat() * 4f), pos.y, Color.White))
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_EXPRESSION") frame // 读一次,让每帧都重绘
                    canvasSize[0] = size.width; canvasSize[1] = size.height
                    // 星空底
                    val starRnd = Random(42)
                    repeat(46) {
                        drawCircle(
                            Color.White.copy(alpha = 0.10f + starRnd.nextFloat() * 0.16f),
                            radius = 1f + starRnd.nextFloat() * 1.6f,
                            center = Offset(starRnd.nextFloat() * size.width, starRnd.nextFloat() * size.height)
                        )
                    }
                    for (r in rockets) {
                        drawLine(Color.White.copy(alpha = 0.85f), Offset(r.x, r.y + 26f), Offset(r.x, r.y), strokeWidth = 3f, cap = StrokeCap.Round)
                    }
                    for (s in sparks) {
                        val a = s.life.coerceIn(0f, 1f)
                        if (s.trail) {
                            drawLine(
                                s.color.copy(alpha = a * 0.9f),
                                Offset(s.x - s.vx * 3.4f, s.y - s.vy * 3.4f),
                                Offset(s.x, s.y),
                                strokeWidth = 2.4f, cap = StrokeCap.Round
                            )
                        } else {
                            drawCircle(s.color.copy(alpha = a), radius = 2.2f + a * 1.8f, center = Offset(s.x, s.y))
                        }
                    }
                }
                Text(
                    "点夜空放一朵",
                    Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.45f)
                )
                Row(
                    Modifier.align(Alignment.BottomStart).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("自动放", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = auto, onCheckedChange = { auto = it })
                }
            }
        }
    }
}
