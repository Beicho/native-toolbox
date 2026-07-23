package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 设备硬件分类路由(17 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.deviceToolsGraph(back: () -> Unit) {
    composable("tool/deviceinfo") { DeviceInfoToolScreen(back) }
    composable("tool/level") { LevelToolScreen(back) }
    composable("tool/screentest") { ScreenTestToolScreen(back) }
    composable("tool/sensor_dash") { PlaceholderToolScreen("传感器仪表盘", back) }
    composable("tool/compass") { PlaceholderToolScreen("指南针", back) }
    composable("tool/battery_info") { PlaceholderToolScreen("电池信息", back) }
    composable("tool/performance") { PlaceholderToolScreen("性能监控", back) }
    composable("tool/hardware_test") { PlaceholderToolScreen("硬件体检", back) }
    composable("tool/storage_clean") { PlaceholderToolScreen("存储清理", back) }
    composable("tool/wifi_analyze") { PlaceholderToolScreen("WiFi 分析", back) }
    composable("tool/bluetooth_scan") { PlaceholderToolScreen("蓝牙雷达", back) }
    composable("tool/nfc_tool") { PlaceholderToolScreen("NFC 读写", back) }
    composable("tool/gps_speed") { PlaceholderToolScreen("GPS 速度表", back) }
    composable("tool/decibel_meter") { PlaceholderToolScreen("分贝仪", back) }
    composable("tool/flashlight") { PlaceholderToolScreen("手电筒", back) }
    composable("tool/screen_on") { PlaceholderToolScreen("屏幕常亮", back) }
    composable("tool/screen_time") { PlaceholderToolScreen("屏幕时间", back) }
}
