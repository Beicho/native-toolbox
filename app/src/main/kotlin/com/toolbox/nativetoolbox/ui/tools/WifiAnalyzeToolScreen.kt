package com.toolbox.nativetoolbox.ui.tools

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
import java.net.Inet4Address
import java.net.NetworkInterface

private class NetSnapshot(
    val type: String,
    val downstreamKbps: Int,
    val upstreamKbps: Int,
    val rssi: Int,
    val linkSpeedMbps: Int,
    val frequencyMhz: Int,
    val metered: Boolean,
    val validated: Boolean,
    val vpn: Boolean,
    val localIps: List<Pair<String, String>>
)

private fun bandOf(freq: Int): String = when {
    freq <= 0 -> "未知"
    freq < 2500 -> "2.4 GHz"
    freq < 5900 -> "5 GHz"
    else -> "6 GHz"
}

private fun channelOf(freq: Int): String = when {
    freq in 2412..2484 -> (((freq - 2412) / 5) + 1).toString()
    freq in 5170..5825 -> (((freq - 5000) / 5)).toString()
    freq > 5900 -> (((freq - 5950) / 5) + 1).toString()
    else -> "—"
}

private fun signalQuality(rssi: Int): String = when {
    rssi == 0 -> "—"
    rssi >= -50 -> "极好"
    rssi >= -60 -> "好"
    rssi >= -70 -> "一般"
    rssi >= -80 -> "偏弱"
    else -> "很弱"
}

private fun localAddresses(): List<Pair<String, String>> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { iface ->
            iface.inetAddresses.toList()
                .filterIsInstance<Inet4Address>()
                .map { iface.name to (it.hostAddress ?: "") }
        }
        .filter { it.second.isNotBlank() }
}.getOrDefault(emptyList())

private fun readNet(context: Context): NetSnapshot {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork
    val caps = network?.let { cm.getNetworkCapabilities(it) }
    val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    val type = when {
        caps == null -> "未连接"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动数据"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "有线"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        else -> "其他"
    }

    @Suppress("DEPRECATION")
    val info = runCatching { wifi?.connectionInfo }.getOrNull()

    return NetSnapshot(
        type = type,
        downstreamKbps = caps?.linkDownstreamBandwidthKbps ?: 0,
        upstreamKbps = caps?.linkUpstreamBandwidthKbps ?: 0,
        rssi = info?.rssi ?: 0,
        linkSpeedMbps = info?.linkSpeed ?: 0,
        frequencyMhz = info?.frequency ?: 0,
        metered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false,
        validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
        vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
        localIps = localAddresses()
    )
}

@Composable
fun WifiAnalyzeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var data by remember { mutableStateOf(readNet(context)) }

    DisposableEffect(Unit) {
        val job = scope.launch {
            while (isActive) {
                data = readNet(context)
                delay(2000)
            }
        }
        onDispose { job.cancel() }
    }

    // RSSI 从 -100 到 -40 映射成 0 到 1
    val signalFraction = if (data.rssi == 0) 0f
    else ((data.rssi + 100).coerceIn(0, 60) / 60f)

    ToolScaffold {
        item { SectionHeader("当前网络") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("类型", data.type, Modifier.weight(1f))
                        StatCell(
                            "信号",
                            if (data.rssi == 0) "—" else data.rssi.toString() + " dBm",
                            Modifier.weight(1f)
                        )
                        StatCell("质量", signalQuality(data.rssi), Modifier.weight(1f))
                    }
                    if (data.rssi != 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.sunkenBackground)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(signalFraction)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            signalFraction > 0.6f -> palette.green
                                            signalFraction > 0.35f -> palette.orange
                                            else -> palette.red
                                        }
                                    )
                            )
                        }
                    }
                    if (!data.validated && data.type != "未连接") {
                        Text(
                            "系统还没验证这个网络能上外网，可能需要网页认证或者根本不通。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.orange
                        )
                    }
                    if (data.vpn) {
                        Text(
                            "当前流量走 VPN。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.accent
                        )
                    }
                }
            }
        }
        item { SectionHeader("链路参数") }
        item {
            GroupedCard {
                KeyValueRow(
                    "协商速率",
                    if (data.linkSpeedMbps > 0) data.linkSpeedMbps.toString() + " Mbps" else "系统未提供",
                    copyable = false
                )
                RowDivider()
                KeyValueRow(
                    "频段",
                    if (data.frequencyMhz > 0) bandOf(data.frequencyMhz) + "（" + data.frequencyMhz + " MHz）" else "系统未提供",
                    copyable = false
                )
                RowDivider()
                KeyValueRow(
                    "信道",
                    if (data.frequencyMhz > 0) channelOf(data.frequencyMhz) else "系统未提供",
                    copyable = false
                )
                RowDivider()
                KeyValueRow(
                    "系统估算下行",
                    if (data.downstreamKbps > 0) String.format("%.1f Mbps", data.downstreamKbps / 1000.0) else "—",
                    copyable = false
                )
                RowDivider()
                KeyValueRow(
                    "系统估算上行",
                    if (data.upstreamKbps > 0) String.format("%.1f Mbps", data.upstreamKbps / 1000.0) else "—",
                    copyable = false
                )
                RowDivider()
                KeyValueRow("按流量计费", if (data.metered) "是" else "否", copyable = false)
            }
        }
        item { SectionHeader("本机地址") }
        item {
            GroupedCard {
                if (data.localIps.isEmpty()) {
                    CardPadding {
                        Text(
                            "读不到本机地址",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    }
                } else {
                    data.localIps.forEachIndexed { index, (iface, ip) ->
                        KeyValueRow(iface, ip)
                        if (index != data.localIps.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
