package com.toolbox.nativetoolbox.ui.tools

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.sqrt

private class SensorReading(val values: FloatArray, val accuracy: Int)

private val watchedSensors = listOf(
    Triple(Sensor.TYPE_ACCELEROMETER, "加速度计", "m/s²"),
    Triple(Sensor.TYPE_GYROSCOPE, "陀螺仪", "rad/s"),
    Triple(Sensor.TYPE_MAGNETIC_FIELD, "磁力计", "μT"),
    Triple(Sensor.TYPE_LIGHT, "光线", "lx"),
    Triple(Sensor.TYPE_PROXIMITY, "距离", "cm"),
    Triple(Sensor.TYPE_PRESSURE, "气压", "hPa"),
    Triple(Sensor.TYPE_AMBIENT_TEMPERATURE, "环境温度", "℃"),
    Triple(Sensor.TYPE_RELATIVE_HUMIDITY, "相对湿度", "%"),
    Triple(Sensor.TYPE_STEP_COUNTER, "计步器", "步"),
    Triple(Sensor.TYPE_ROTATION_VECTOR, "旋转矢量", "")
)

private fun accuracyText(accuracy: Int): String = when (accuracy) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "高"
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "中"
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "低"
    SensorManager.SENSOR_STATUS_UNRELIABLE -> "不可靠"
    else -> "未知"
}

private fun formatValues(values: FloatArray, unit: String): String {
    if (values.isEmpty()) return "—"
    val text = values.take(3).joinToString("  ") { String.format("%.2f", it) }
    return if (unit.isBlank()) text else text + " " + unit
}

@Composable
fun SensorDashToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }

    val readings = remember { mutableStateOf(mapOf<Int, SensorReading>()) }
    var available by remember { mutableStateOf(listOf<Triple<Int, String, String>>()) }
    var missing by remember { mutableStateOf(listOf<String>()) }

    DisposableEffect(manager) {
        if (manager == null) {
            onDispose { }
        } else {
            val present = ArrayList<Triple<Int, String, String>>()
            val absent = ArrayList<String>()
            watchedSensors.forEach { entry ->
                if (manager.getDefaultSensor(entry.first) != null) present.add(entry) else absent.add(entry.second)
            }
            available = present
            missing = absent

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    readings.value = readings.value.toMutableMap().also {
                        it[event.sensor.type] = SensorReading(event.values.copyOf(), event.accuracy)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
            }
            present.forEach { (type, _, _) ->
                manager.getDefaultSensor(type)?.let {
                    manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
                }
            }
            onDispose { manager.unregisterListener(listener) }
        }
    }

    val accel = readings.value[Sensor.TYPE_ACCELEROMETER]?.values
    // 加速度模长减去重力，能大致看出手机是不是在动
    val motion = accel?.let { sqrt(it[0] * it[0] + it[1] * it[1] + it[2] * it[2]) - 9.81f }
    val light = readings.value[Sensor.TYPE_LIGHT]?.values?.firstOrNull()
    val steps = readings.value[Sensor.TYPE_STEP_COUNTER]?.values?.firstOrNull()

    ToolScaffold {
        item { SectionHeader("概览") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell(
                            "运动强度",
                            motion?.let { String.format("%.1f", Math.abs(it)) } ?: "—",
                            Modifier.weight(1f)
                        )
                        StatCell(
                            "环境光",
                            light?.let { Math.round(it).toString() + " lx" } ?: "—",
                            Modifier.weight(1f)
                        )
                        StatCell(
                            "可用传感器",
                            available.size.toString(),
                            Modifier.weight(1f)
                        )
                    }
                    if (light != null) {
                        Text(
                            when {
                                light < 10 -> "很暗，接近全黑"
                                light < 100 -> "室内偏暗"
                                light < 1000 -> "正常室内照明"
                                light < 10000 -> "明亮，接近窗边"
                                else -> "户外强光"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    }
                }
            }
        }
        item { SectionHeader("实时读数") }
        item {
            GroupedCard {
                if (available.isEmpty()) {
                    CardPadding {
                        Text(
                            "读不到传感器数据",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    }
                } else {
                    available.forEachIndexed { index, (type, name, unit) ->
                        val reading = readings.value[type]
                        KeyValueRow(
                            name,
                            reading?.let { formatValues(it.values, unit) } ?: "等待数据…",
                            copyable = false
                        )
                        if (index != available.lastIndex) RowDivider()
                    }
                }
            }
        }
        item { SectionHeader("精度") }
        item {
            GroupedCard {
                val withAccuracy = available.filter { readings.value.containsKey(it.first) }
                if (withAccuracy.isEmpty()) {
                    CardPadding {
                        Text("等待数据…", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                    }
                } else {
                    withAccuracy.forEachIndexed { index, (type, name, _) ->
                        KeyValueRow(
                            name,
                            accuracyText(readings.value[type]?.accuracy ?: -1),
                            copyable = false
                        )
                        if (index != withAccuracy.lastIndex) RowDivider()
                    }
                }
            }
        }
        if (missing.isNotEmpty()) {
            item { SectionHeader("这台设备没有的传感器") }
            item {
                GroupedCard {
                    CardPadding {
                        Text(
                            missing.joinToString("、"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryLabel
                        )
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "数据直接来自系统传感器接口，不需要任何权限（计步器在部分系统上需要身体活动权限才有数值）。" +
                            "全部实时显示，不记录不上传。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
