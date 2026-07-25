package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 设备硬件分类路由(17 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.deviceToolsGraph(back: () -> Unit) {
    composable("tool/deviceinfo") { DeviceInfoToolScreen(back) }
    composable("tool/level") { LevelToolScreen(back) }
    composable("tool/screentest") { ScreenTestToolScreen(back) }
    composable("tool/sensor_dash") { SensorDashToolScreen(back) }
    composable("tool/compass") { CompassToolScreen(back) }
    composable("tool/battery_info") { BatteryInfoToolScreen(back) }
    composable("tool/performance") { PerformanceToolScreen(back) }
    composable("tool/hardware_test") { HardwareTestToolScreen(back) }
    composable("tool/storage_clean") { StorageCleanToolScreen(back) }
    composable("tool/wifi_analyze") { WifiAnalyzeToolScreen(back) }
    composable("tool/bluetooth_scan") { BluetoothScanToolScreen(back) }
    composable("tool/nfc_tool") { NfcToolScreen(back) }
    composable("tool/gps_speed") { GpsSpeedToolScreen(back) }
    composable("tool/decibel_meter") { DecibelMeterToolScreen(back) }
    composable("tool/flashlight") { FlashlightToolScreen(back) }
    composable("tool/screen_on") { ScreenOnToolScreen(back) }
    composable("tool/screen_time") { ScreenTimeToolScreen(back) }
}
