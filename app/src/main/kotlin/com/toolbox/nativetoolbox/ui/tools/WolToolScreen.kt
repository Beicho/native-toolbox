package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import androidx.compose.foundation.layout.width

private fun parseMac(text: String): ByteArray? {
    val clean = text.trim().replace("-", ":").replace(" ", ":")
    val parts = clean.split(":").filter { it.isNotEmpty() }
    val hex = if (parts.size == 6) parts else {
        val s = clean.replace(":", "")
        if (s.length != 12) return null
        (0 until 6).map { s.substring(it * 2, it * 2 + 2) }
    }
    return runCatching { hex.map { it.toInt(16).toByte() }.toByteArray() }.getOrNull()
}

/** 魔术包 = 6 个 0xFF + MAC × 16 */
private fun sendMagicPacket(mac: ByteArray, host: String, port: Int): String {
    return runCatching {
        val payload = ByteArray(6) { 0xFF.toByte() } + ByteArray(16 * 6) { mac[it % 6] }
        val addr = InetAddress.getByName(host)
        DatagramSocket().use { sock ->
            sock.broadcast = true
            sock.send(DatagramPacket(payload, payload.size, addr, port))
        }
        "OK"
    }.getOrElse { "发送失败:${it.message}" }
}


@Composable
fun WolToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mac by rememberSaveable { mutableStateOf("") }
    var host by rememberSaveable { mutableStateOf("255.255.255.255") }
    var port by rememberSaveable { mutableStateOf("9") }
    var status by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }

    val prefs = remember { context.getSharedPreferences("wol", android.content.Context.MODE_PRIVATE) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val raw = prefs.getString("saved", "") ?: ""
        saved = raw.split(";").filter { it.contains("|") }.map {
            val p = it.split("|"); Triple(p[0], p.getOrElse(1) { "255.255.255.255" }, p.getOrElse(2) { "9" })
        }
    }

    fun persist() {
        prefs.edit().putString("saved", saved.joinToString(";") { "${it.first}|${it.second}|${it.third}" }).apply()
    }

    fun wake(m: String, h: String, p: String) {
        val macBytes = parseMac(m)
        if (macBytes == null) { status = "MAC 地址格式不对,像 AA:BB:CC:DD:EE:FF 这样填"; return }
        val portNum = p.toIntOrNull() ?: 9
        scope.launch {
            val r = withContext(Dispatchers.IO) { sendMagicPacket(macBytes, h.ifBlank { "255.255.255.255" }, portNum) }
            status = if (r == "OK") "唤醒包已发出。电脑要在同一局域网、主板开了 WOL 才会醒" else r
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Text("电脑网卡 MAC 地址", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(4.dp))
                    IosTextField(mac, { mac = it }, Modifier.fillMaxWidth(), placeholder = "AA:BB:CC:DD:EE:FF", mono = true)
                    Spacer(Modifier.height(10.dp))
                    Text("广播地址(一般不用改)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(4.dp))
                    IosTextField(host, { host = it }, Modifier.fillMaxWidth(), placeholder = "255.255.255.255", mono = true)
                    Spacer(Modifier.height(10.dp))
                    Text("端口(9 或 7)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Spacer(Modifier.height(4.dp))
                    IosTextField(port, { port = it.filter { c -> c.isDigit() } }, Modifier.fillMaxWidth(), placeholder = "9")
                    Spacer(Modifier.height(12.dp))
                    SolidButton(onClick = { wake(mac, host, port) }, Modifier.fillMaxWidth(), enabled = mac.isNotBlank()) { Text("唤醒") }
                    Spacer(Modifier.height(8.dp))
                    SolidButton(
                        onClick = {
                            if (parseMac(mac) == null) { status = "MAC 格式不对,存不了"; return@SolidButton }
                            saved = (saved + Triple(mac.trim(), host.trim(), port.trim())).distinctBy { it.first }
                            persist()
                            status = "已保存,下次一键唤醒"
                        },
                        Modifier.fillMaxWidth(), filled = false, enabled = mac.isNotBlank()
                    ) { Text("保存这台设备") }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("唤醒包") || status.startsWith("已保存")) palette.green else palette.red)
                    }
                }
            }
        }
        item { if (saved.isNotEmpty()) SectionHeader("已保存的设备") }
        item {
            if (saved.isNotEmpty()) {
                GroupedCard {
                    saved.forEachIndexed { i, (m, h, p) ->
                        androidx.compose.foundation.layout.Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                                Text(m, style = MaterialTheme.typography.bodyMedium, color = palette.label)
                                Text("$h:$p", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                            }
                            SolidButton(onClick = { wake(m, h, p) }, Modifier.width(88.dp), height = 36.dp) { Text("唤醒") }
                            Spacer(Modifier.width(8.dp))
                            SolidButton(onClick = { saved = saved.filterIndexed { idx, _ -> idx != i }; persist() }, Modifier.width(64.dp), height = 36.dp, filled = false) { Text("删") }
                        }
                        if (i != saved.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
