package com.toolbox.nativetoolbox.ui.tools

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import android.hardware.Sensor
import android.hardware.SensorManager

private class HardwareItem(val name: String, val present: Boolean, val note: String)

private fun collectHardware(context: Context): List<HardwareItem> {
    val pm = context.packageManager
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    fun feature(name: String) = pm.hasSystemFeature(name)
    fun sensor(type: Int) = sensorManager?.getDefaultSensor(type) != null

    return listOf(
        HardwareItem("后置摄像头", feature(PackageManager.FEATURE_CAMERA_ANY), "拍照、扫码、放大镜"),
        HardwareItem("前置摄像头", feature(PackageManager.FEATURE_CAMERA_FRONT), "自拍、镜子"),
        HardwareItem("自动对焦", feature(PackageManager.FEATURE_CAMERA_AUTOFOCUS), "拍近物清晰度"),
        HardwareItem("闪光灯", feature(PackageManager.FEATURE_CAMERA_FLASH), "手电筒、补光"),
        HardwareItem("指纹识别", feature(PackageManager.FEATURE_FINGERPRINT), "解锁与支付"),
        HardwareItem("NFC", feature(PackageManager.FEATURE_NFC), "刷卡、门禁、公交"),
        HardwareItem("蓝牙", feature(PackageManager.FEATURE_BLUETOOTH), "耳机、手表"),
        HardwareItem("低功耗蓝牙", feature(PackageManager.FEATURE_BLUETOOTH_LE), "小米手环这类设备"),
        HardwareItem("Wi-Fi", feature(PackageManager.FEATURE_WIFI), "无线上网"),
        HardwareItem("Wi-Fi 直连", feature(PackageManager.FEATURE_WIFI_DIRECT), "设备间直传"),
        HardwareItem("GPS 定位", feature(PackageManager.FEATURE_LOCATION_GPS), "导航、轨迹"),
        HardwareItem("电话功能", feature(PackageManager.FEATURE_TELEPHONY), "打电话、插 SIM"),
        HardwareItem("麦克风", feature(PackageManager.FEATURE_MICROPHONE), "录音、语音、分贝仪"),
        HardwareItem("加速度计", sensor(Sensor.TYPE_ACCELEROMETER), "计步、横竖屏、水平仪"),
        HardwareItem("陀螺仪", sensor(Sensor.TYPE_GYROSCOPE), "游戏体感、防抖"),
        HardwareItem("磁力计", sensor(Sensor.TYPE_MAGNETIC_FIELD), "指南针"),
        HardwareItem("光线传感器", sensor(Sensor.TYPE_LIGHT), "自动亮度"),
        HardwareItem("距离传感器", sensor(Sensor.TYPE_PROXIMITY), "接电话时息屏"),
        HardwareItem("气压计", sensor(Sensor.TYPE_PRESSURE), "海拔估算"),
        HardwareItem("环境温度计", sensor(Sensor.TYPE_AMBIENT_TEMPERATURE), "很少有手机带"),
        HardwareItem("湿度计", sensor(Sensor.TYPE_RELATIVE_HUMIDITY), "很少有手机带"),
        HardwareItem("计步器", sensor(Sensor.TYPE_STEP_COUNTER), "系统级计步"),
        HardwareItem("心率传感器", sensor(Sensor.TYPE_HEART_RATE), "一般只有手表有")
    )
}

@Composable
fun HardwareTestToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current

    val items = remember { collectHardware(context) }
    var filter by rememberSaveable { mutableStateOf(0) }

    val present = items.count { it.present }
    val shown = when (filter) {
        1 -> items.filter { it.present }
        2 -> items.filter { !it.present }
        else -> items
    }

    ToolScaffold {
        item { SectionHeader("硬件概览") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("检出", present.toString(), Modifier.weight(1f))
                        StatCell("缺失", (items.size - present).toString(), Modifier.weight(1f))
                        StatCell("检测项", items.size.toString(), Modifier.weight(1f))
                    }
                    Text(
                        "全部通过系统的硬件能力查询接口判断，不需要权限，也不会真的去启动硬件。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("设备信息") }
        item {
            GroupedCard {
                KeyValueRow("品牌", Build.BRAND)
                RowDivider()
                KeyValueRow("型号", Build.MODEL)
                RowDivider()
                KeyValueRow("主板", Build.BOARD)
                RowDivider()
                KeyValueRow("硬件平台", Build.HARDWARE)
                RowDivider()
                KeyValueRow("系统版本", "Android " + Build.VERSION.RELEASE)
                RowDivider()
                KeyValueRow("API 等级", Build.VERSION.SDK_INT.toString())
                RowDivider()
                KeyValueRow("架构", Build.SUPPORTED_ABIS.joinToString(", "))
                RowDivider()
                KeyValueRow("安全补丁", Build.VERSION.SECURITY_PATCH.ifBlank { "未知" })
            }
        }
        item { SectionHeader("筛选") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("全部", "有", "没有"),
                        selectedIndex = filter,
                        onSelected = { filter = it }
                    )
                }
            }
        }
        item { SectionHeader("检测明细（" + shown.size + "）") }
        item {
            GroupedCard {
                shown.forEachIndexed { index, item ->
                    KeyValueRow(
                        item.name,
                        (if (item.present) "有　" else "没有　") + item.note,
                        copyable = false
                    )
                    if (index != shown.lastIndex) RowDivider()
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "显示「没有」不一定是坏了，多数是这个型号本身就没配。环境温度计和湿度计绝大部分手机都没有。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
