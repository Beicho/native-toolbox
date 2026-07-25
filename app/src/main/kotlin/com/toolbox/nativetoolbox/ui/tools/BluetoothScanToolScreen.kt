package com.toolbox.nativetoolbox.ui.tools

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.PermissionGate
import kotlin.math.pow

private data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val lastSeen: Long,
)

/** RSSI 粗估距离(米):环境因子 2.2,只能看个大概 */
private fun estimateDistance(rssi: Int): Double = 10.0.pow((-59.0 - rssi) / (10 * 2.2))

@SuppressLint("MissingPermission")
@Composable
private fun ScannerContent() {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val devices = remember { mutableStateMapOf<String, BleDevice>() }
    var scanning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val adapter = remember { (context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter }
    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.device?.name ?: result.scanRecord?.deviceName
                devices[result.device.address] = BleDevice(
                    result.device.address, name, result.rssi, System.currentTimeMillis()
                )
            }

            override fun onScanFailed(errorCode: Int) {
                error = "扫描失败(代码 $errorCode),试试关开一次蓝牙"
                scanning = false
            }
        }
    }

    fun start() {
        if (adapter == null || !adapter.isEnabled) { error = "先在系统里打开蓝牙"; return }
        error = ""
        devices.clear()
        runCatching {
            adapter.bluetoothLeScanner?.startScan(
                null,
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                callback
            )
            scanning = true
        }.onFailure { error = "启动扫描失败:${it.message}" }
    }

    fun stop() {
        runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
        scanning = false
    }

    DisposableEffect(Unit) { onDispose { stop() } }

    val sorted = devices.values.sortedByDescending { it.rssi }

    GroupedCard {
        CardPadding {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("发现设备", "${devices.size}", Modifier.weight(1f))
                StatCell("状态", if (scanning) "扫描中" else "已停止", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            SolidButton(onClick = { if (scanning) stop() else start() }, Modifier.fillMaxWidth()) {
                Text(if (scanning) "停止扫描" else "开始扫描")
            }
            if (error.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(error, style = MaterialTheme.typography.bodyMedium, color = palette.red)
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    if (sorted.isNotEmpty()) {
        SectionHeader("附近的蓝牙设备(按信号强弱)")
        GroupedCard {
            sorted.forEachIndexed { i, d ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            d.name ?: "未知设备",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = palette.label,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${d.rssi} dBm", style = MaterialTheme.typography.bodyMedium, color = if (d.rssi > -70) palette.green else if (d.rssi > -85) palette.orange else palette.red)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${d.address} · 约 ${"%.1f".format(estimateDistance(d.rssi))} 米",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
                if (i != sorted.lastIndex) RowDivider()
            }
        }
    } else if (scanning) {
        Text(
            "正在搜索…耳机、手环、电视都会出现在这里",
            Modifier.fillMaxWidth().padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.tertiaryLabel,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun BluetoothScanToolScreen(onBack: () -> Unit) {
    // Android 12+ 用 BLUETOOTH_SCAN,11 及以下扫描要精确定位权限
    val permission = if (Build.VERSION.SDK_INT >= 31) android.Manifest.permission.BLUETOOTH_SCAN
    else android.Manifest.permission.ACCESS_FINE_LOCATION

    ToolScaffold {
        item {
            PermissionGate(permission, "扫描附近蓝牙设备需要这个权限,数据不出手机") {
                ScannerContent()
            }
        }
    }
}
