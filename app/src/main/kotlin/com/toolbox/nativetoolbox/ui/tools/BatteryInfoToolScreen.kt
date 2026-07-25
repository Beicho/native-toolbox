package com.toolbox.nativetoolbox.ui.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private class BatterySnapshot(
    val level: Int,
    val voltageMv: Int,
    val temperatureTenth: Int,
    val status: Int,
    val plugged: Int,
    val health: Int,
    val technology: String,
    val currentUa: Int,
    val capacityUah: Int
)

private fun readBattery(context: Context): BatterySnapshot {
    val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    return BatterySnapshot(
        level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1,
        voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1,
        temperatureTenth = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1,
        status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1,
        plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1,
        health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1,
        technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "",
        currentUa = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: Int.MIN_VALUE,
        capacityUah = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: Int.MIN_VALUE
    )
}

private fun statusText(status: Int): String = when (status) {
    BatteryManager.BATTERY_STATUS_CHARGING -> "正在充电"
    BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
    BatteryManager.BATTERY_STATUS_FULL -> "已充满"
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
    else -> "未知"
}

private fun pluggedText(plugged: Int): String = when (plugged) {
    BatteryManager.BATTERY_PLUGGED_AC -> "充电器"
    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线充电"
    0 -> "未连接"
    else -> "未知"
}

private fun healthText(health: Int): String = when (health) {
    BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
    BatteryManager.BATTERY_HEALTH_DEAD -> "已损坏"
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "电压过高"
    BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "异常"
    else -> "未知"
}

@Composable
fun BatteryInfoToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var snapshot by remember { mutableStateOf(readBattery(context)) }

    DisposableEffect(Unit) {
        val job = scope.launch {
            while (isActive) {
                snapshot = readBattery(context)
                delay(2000)
            }
        }
        onDispose { job.cancel() }
    }

    val temperature = if (snapshot.temperatureTenth > 0) snapshot.temperatureTenth / 10.0 else Double.NaN
    val voltage = if (snapshot.voltageMv > 0) snapshot.voltageMv / 1000.0 else Double.NaN
    val currentMa = if (snapshot.currentUa != Int.MIN_VALUE) snapshot.currentUa / 1000.0 else Double.NaN
    // 电流 × 电压 = 瞬时功率
    val powerW = if (!currentMa.isNaN() && !voltage.isNaN()) Math.abs(currentMa / 1000.0 * voltage) else Double.NaN
    val capacityMah = if (snapshot.capacityUah != Int.MIN_VALUE) snapshot.capacityUah / 1000 else -1

    val tempColor = when {
        temperature.isNaN() -> palette.secondaryLabel
        temperature >= 45 -> palette.red
        temperature >= 40 -> palette.orange
        else -> palette.green
    }

    ToolScaffold {
        item { SectionHeader("当前状态") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell(
                            "电量",
                            if (snapshot.level >= 0) snapshot.level.toString() + "%" else "—",
                            Modifier.weight(1f)
                        )
                        StatCell(
                            "温度",
                            if (temperature.isNaN()) "—" else String.format("%.1f℃", temperature),
                            Modifier.weight(1f)
                        )
                        StatCell(
                            "功率",
                            if (powerW.isNaN()) "—" else String.format("%.1fW", powerW),
                            Modifier.weight(1f)
                        )
                    }
                    Text(
                        statusText(snapshot.status) + "　" + pluggedText(snapshot.plugged),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (snapshot.status == BatteryManager.BATTERY_STATUS_CHARGING) palette.green else palette.secondaryLabel
                    )
                    if (!temperature.isNaN() && temperature >= 40) {
                        Text(
                            "电池有点热了，充电时别玩重度游戏，也别盖着东西。",
                            style = MaterialTheme.typography.bodySmall,
                            color = tempColor
                        )
                    }
                }
            }
        }
        item { SectionHeader("详细数据") }
        item {
            GroupedCard {
                KeyValueRow("电压", if (voltage.isNaN()) "" else String.format("%.3f V", voltage), copyable = false)
                RowDivider()
                KeyValueRow(
                    "电流",
                    if (currentMa.isNaN()) "" else String.format("%.0f mA", currentMa) +
                        if (currentMa > 0) "（充入）" else if (currentMa < 0) "（输出）" else "",
                    copyable = false
                )
                RowDivider()
                KeyValueRow("剩余容量", if (capacityMah > 0) capacityMah.toString() + " mAh" else "系统未提供", copyable = false)
                RowDivider()
                KeyValueRow("健康状况", healthText(snapshot.health), copyable = false)
                RowDivider()
                KeyValueRow("电池类型", snapshot.technology.ifBlank { "未知" }, copyable = false)
            }
        }
        item { SectionHeader("说明") }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "数据每两秒刷新一次，全部来自系统电池接口，不需要任何权限。\n\n" +
                            "部分厂商的系统不上报电流和容量，显示「系统未提供」是正常的。" +
                            "电流为正表示正在充入，为负表示正在耗电。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader("保养建议") }
        item {
            GroupedCard {
                val tips = listOf(
                    "日常电量" to "保持在 20% 到 80% 之间最养电池",
                    "避免高温" to "40℃ 以上会明显加速老化",
                    "快充" to "偶尔用没问题，长期慢充更温和",
                    "长期存放" to "留 50% 左右电量，别放空",
                    "边充边玩" to "发热叠加，尽量避免"
                )
                tips.forEachIndexed { index, (k, v) ->
                    KeyValueRow(k, v, copyable = false)
                    if (index != tips.lastIndex) RowDivider()
                }
            }
        }
    }
}
