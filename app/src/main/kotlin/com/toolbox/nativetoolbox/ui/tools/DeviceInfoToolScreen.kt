package com.toolbox.nativetoolbox.ui.tools

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.util.FileHelper

@Composable
fun DeviceInfoToolScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val density = LocalDensity.current

    val info = remember {
        val dm = context.resources.displayMetrics
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        listOf(
            "设备" to listOf(
                "品牌" to Build.BRAND,
                "型号" to Build.MODEL,
                "设备名" to Build.DEVICE,
                "制造商" to Build.MANUFACTURER,
                "主板" to Build.BOARD,
                "SoC" to if (Build.VERSION.SDK_INT >= 31) {
                    Build.SOC_MODEL.takeIf { it != Build.UNKNOWN } ?: Build.HARDWARE
                } else Build.HARDWARE,
                "支持 ABI" to Build.SUPPORTED_ABIS.joinToString(", ")
            ),
            "系统" to listOf(
                "Android 版本" to Build.VERSION.RELEASE,
                "API Level" to Build.VERSION.SDK_INT.toString(),
                "安全补丁" to Build.VERSION.SECURITY_PATCH,
                "构建号" to Build.DISPLAY
            ),
            "屏幕" to listOf(
                "分辨率" to "${dm.widthPixels} × ${dm.heightPixels}",
                "密度" to "${dm.densityDpi} dpi (${dm.density}x)",
                "刷新率" to "${
                    if (Build.VERSION.SDK_INT >= 30) context.display?.refreshRate?.toInt() ?: 60
                    else @Suppress("DEPRECATION") (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.refreshRate.toInt()
                } Hz"
            ),
            "内存与电量" to listOf(
                "总内存" to FileHelper.formatFileSize(mem.totalMem),
                "可用内存" to FileHelper.formatFileSize(mem.availMem),
                "电量" to "$battery%"
            )
        )
    }

    ToolScaffold {
        info.forEach { (section, rows) ->
            item { SectionHeader(section) }
            item {
                GroupedCard {
                    rows.forEachIndexed { index, (k, v) ->
                        if (index > 0) RowDivider()
                        KeyValueRow(k, v)
                    }
                }
            }
        }
    }
}
