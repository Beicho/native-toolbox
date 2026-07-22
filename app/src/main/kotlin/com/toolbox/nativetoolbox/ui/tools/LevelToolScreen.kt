package com.toolbox.nativetoolbox.ui.tools

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.abs
import kotlin.math.atan2

/** 水平仪:加速度计驱动,玻璃感气泡(径向渐变高光模拟玻璃珠) */
@Composable
fun LevelToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var ax by remember { mutableFloatStateOf(0f) }
    var ay by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // 低通滤波,气泡稳一点
                ax = ax * 0.8f + event.values[0] * 0.2f
                ay = ay * 0.8f + event.values[1] * 0.2f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    val g = 9.81f
    val tiltX = (ax / g).coerceIn(-1f, 1f)
    val tiltY = (ay / g).coerceIn(-1f, 1f)
    val angleX = Math.toDegrees(atan2(ax, g).toDouble())
    val angleY = Math.toDegrees(atan2(ay, g).toDouble())
    val level = abs(angleX) < 1.0 && abs(angleY) < 1.0

    val bubbleColor = if (level) palette.green else palette.accent

    ToolScaffold {
        item { SectionHeader("把手机平放在物体表面") }
        item {
            GroupedCard {
                CardPadding {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(if (level) palette.green.copy(alpha = 0.15f) else palette.sunkenBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        // 十字准线
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(palette.separator.copy(alpha = 0.35f))
                        )
                        Box(
                            Modifier
                                .width1()
                                .background(palette.separator.copy(alpha = 0.35f))
                        )
                        // 中心目标圈
                        Box(
                            Modifier
                                .size(64.dp)
                                .border(2.dp, bubbleColor.copy(alpha = 0.6f), CircleShape)
                        )
                        // 玻璃气泡
                        val bubble by animateOffsetAsState(
                            targetValue = Offset(-tiltX * 130f, tiltY * 130f),
                            animationSpec = spring(0.7f, 200f),
                            label = "bubble"
                        )
                        Box(
                            Modifier
                                .offset { IntOffset(bubble.x.dp.roundToPx(), bubble.y.dp.roundToPx()) }
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.9f),
                                            bubbleColor.copy(alpha = 0.55f),
                                            bubbleColor.copy(alpha = 0.8f)
                                        ),
                                        center = Offset(38f, 32f),
                                        radius = 90f
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                        )
                    }
                    Text(
                        if (level) "水平了!" else "还差一点…",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (level) palette.green else palette.secondaryLabel,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
        item { SectionHeader("角度") }
        item {
            GroupedCard {
                KeyValueRow("左右倾角", "%.1f°".format(angleX), copyable = false)
                RowDivider()
                KeyValueRow("前后倾角", "%.1f°".format(angleY), copyable = false)
            }
        }
    }
}

private fun Modifier.width1(): Modifier =
    this.then(Modifier.size(width = 1.dp, height = 10000.dp))
