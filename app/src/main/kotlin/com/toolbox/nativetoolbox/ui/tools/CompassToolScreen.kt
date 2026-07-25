package com.toolbox.nativetoolbox.ui.tools

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private fun directionName(degrees: Float): String {
    val d = ((degrees % 360) + 360) % 360
    return when {
        d < 22.5 || d >= 337.5 -> "北"
        d < 67.5 -> "东北"
        d < 112.5 -> "东"
        d < 157.5 -> "东南"
        d < 202.5 -> "南"
        d < 247.5 -> "西南"
        d < 292.5 -> "西"
        else -> "西北"
    }
}

@Composable
fun CompassToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }

    var heading by remember { mutableFloatStateOf(0f) }
    var accuracy by remember { mutableStateOf(-1) }
    var supported by remember { mutableStateOf(true) }
    var tiltWarning by remember { mutableStateOf(false) }

    DisposableEffect(manager) {
        val rotationSensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val magSensor = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val accelSensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (manager == null || (rotationSensor == null && (magSensor == null || accelSensor == null))) {
            supported = false
            onDispose { }
        } else {
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            var gravity: FloatArray? = null
            var geomagnetic: FloatArray? = null

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            heading = Math.toDegrees(orientation[0].toDouble()).toFloat()
                            // 俯仰或翻滚太大时磁力读数不可靠
                            tiltWarning = Math.abs(Math.toDegrees(orientation[1].toDouble())) > 35 ||
                                Math.abs(Math.toDegrees(orientation[2].toDouble())) > 35
                            accuracy = event.accuracy
                        }
                        Sensor.TYPE_ACCELEROMETER -> gravity = event.values.copyOf()
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            geomagnetic = event.values.copyOf()
                            accuracy = event.accuracy
                        }
                    }
                    if (rotationSensor == null) {
                        val g = gravity
                        val m = geomagnetic
                        if (g != null && m != null &&
                            SensorManager.getRotationMatrix(rotationMatrix, null, g, m)
                        ) {
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            heading = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, value: Int) {
                    accuracy = value
                }
            }

            if (rotationSensor != null) {
                manager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                manager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
                manager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose { manager.unregisterListener(listener) }
        }
    }

    val degrees = ((heading % 360) + 360) % 360

    ToolScaffold {
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!supported) {
                        Text(
                            "这台设备没有磁力传感器，用不了指南针",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.red
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.fillMaxWidth().height(220.dp)) {
                                val center = Offset(size.width / 2, size.height / 2)
                                val radius = minOf(size.width, size.height) / 2 - 20f
                                drawCircle(
                                    color = palette.separator,
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 2f)
                                )
                                // 刻度：每 30 度一根长线
                                (0 until 360 step 15).forEach { tick ->
                                    val angle = Math.toRadians((tick - degrees).toDouble() - 90)
                                    val inner = if (tick % 30 == 0) radius - 18f else radius - 9f
                                    drawLine(
                                        color = if (tick % 90 == 0) palette.label else palette.tertiaryLabel,
                                        start = Offset(
                                            center.x + (inner * cos(angle)).toFloat(),
                                            center.y + (inner * sin(angle)).toFloat()
                                        ),
                                        end = Offset(
                                            center.x + (radius * cos(angle)).toFloat(),
                                            center.y + (radius * sin(angle)).toFloat()
                                        ),
                                        strokeWidth = if (tick % 90 == 0) 3f else 1.5f
                                    )
                                }
                                // 指北针
                                val northAngle = Math.toRadians((-degrees).toDouble() - 90)
                                drawLine(
                                    color = palette.red,
                                    start = center,
                                    end = Offset(
                                        center.x + ((radius - 30f) * cos(northAngle)).toFloat(),
                                        center.y + ((radius - 30f) * sin(northAngle)).toFloat()
                                    ),
                                    strokeWidth = 6f
                                )
                                drawCircle(color = palette.label, radius = 6f, center = center)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    degrees.roundToInt().toString() + "°",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Light,
                                    color = palette.label
                                )
                                Text(
                                    directionName(degrees),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.accent
                                )
                            }
                        }
                        if (tiltWarning) {
                            Text(
                                "手机倾斜太多，读数会不准，尽量放平",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.orange
                            )
                        }
                    }
                }
            }
        }
        item { SectionHeader("读数") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("方位角", degrees.roundToInt().toString() + "°", Modifier.weight(1f))
                        StatCell("方向", directionName(degrees), Modifier.weight(1f))
                        StatCell(
                            "精度",
                            when (accuracy) {
                                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "高"
                                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "中"
                                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "低"
                                SensorManager.SENSOR_STATUS_UNRELIABLE -> "差"
                                else -> "—"
                            },
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        item { SectionHeader("八方位对照") }
        item {
            GroupedCard {
                val dirs = listOf(
                    "北" to "0°", "东北" to "45°", "东" to "90°", "东南" to "135°",
                    "南" to "180°", "西南" to "225°", "西" to "270°", "西北" to "315°"
                )
                dirs.forEachIndexed { index, (name, angle) ->
                    KeyValueRow(name, angle, copyable = false)
                    if (index != dirs.lastIndex) RowDivider()
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "指的是磁北，和地图上的真北有几度偏差。精度显示「低」或「差」时，把手机拿起来画几个 8 字可以校准。" +
                            "附近有磁铁、金属或强电流会明显干扰。全程离线，不需要定位权限。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
