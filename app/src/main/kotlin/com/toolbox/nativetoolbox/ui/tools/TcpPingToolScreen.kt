package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

private val commonPorts = listOf(
    80 to "HTTP",
    443 to "HTTPS",
    22 to "SSH",
    3306 to "MySQL",
    5432 to "PostgreSQL",
    6379 to "Redis",
    27017 to "MongoDB",
    8080 to "备用 HTTP"
)

/** TCP 握手计时：连上就算通，比 ICMP ping 更能反映实际能不能用 */
private suspend fun tcpProbe(host: String, port: Int, timeoutMs: Int): Long? =
    withContext(Dispatchers.IO) {
        runCatching {
            val started = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            (System.nanoTime() - started) / 1_000_000
        }.getOrNull()
    }

@Composable
fun TcpPingToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var host by rememberSaveable { mutableStateOf("") }
    var portText by rememberSaveable { mutableStateOf("443") }
    var countText by rememberSaveable { mutableStateOf("5") }
    var results by remember { mutableStateOf<List<Long?>>(emptyList()) }
    var running by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf("") }
    var scanResults by remember { mutableStateOf<List<Pair<Int, Long?>>>(emptyList()) }
    var scanning by rememberSaveable { mutableStateOf(false) }

    val port = portText.trim().toIntOrNull()?.coerceIn(1, 65535) ?: 443
    val count = countText.trim().toIntOrNull()?.coerceIn(1, 20) ?: 5

    fun cleanHost(): String = host.trim()
        .removePrefix("https://").removePrefix("http://")
        .trimEnd('/').substringBefore('/').substringBefore(':')

    fun run() {
        val target = cleanHost()
        if (target.isBlank()) {
            status = "先输入域名或 IP"
            return
        }
        running = true
        status = ""
        results = emptyList()
        scope.launch {
            val collected = ArrayList<Long?>()
            repeat(count) {
                val r = tcpProbe(target, port, 3000)
                collected.add(r)
                results = collected.toList()
            }
            running = false
            if (collected.all { it == null }) {
                status = "全部超时。可能是端口没开、被防火墙拦了，或者域名不对。"
            }
        }
    }

    fun scan() {
        val target = cleanHost()
        if (target.isBlank()) {
            status = "先输入域名或 IP"
            return
        }
        scanning = true
        status = ""
        scanResults = emptyList()
        scope.launch {
            val collected = ArrayList<Pair<Int, Long?>>()
            commonPorts.forEach { (p, _) ->
                collected.add(p to tcpProbe(target, p, 2000))
                scanResults = collected.toList()
            }
            scanning = false
        }
    }

    val ok = results.filterNotNull()
    val lossRate = if (results.isEmpty()) 0.0 else (results.size - ok.size) * 100.0 / results.size
    val avg = if (ok.isEmpty()) Double.NaN else ok.average()
    val jitter = if (ok.size < 2) Double.NaN else {
        val mean = ok.average()
        Math.sqrt(ok.sumOf { (it - mean) * (it - mean) } / ok.size)
    }

    ToolScaffold {
        item { SectionHeader("目标") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = host,
                        onValueChange = { host = it },
                        placeholder = "域名或 IP，例如 github.com",
                        mono = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = portText,
                            onValueChange = { portText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "端口",
                            mono = true
                        )
                        IosTextField(
                            value = countText,
                            onValueChange = { countText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "测几次",
                            mono = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = { run() },
                            modifier = Modifier.weight(1f),
                            enabled = !running && !scanning
                        ) { Text(if (running) "测试中…" else "开始测试") }
                        SolidButton(
                            onClick = { scan() },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = !running && !scanning
                        ) { Text(if (scanning) "扫描中…" else "扫常用端口") }
                    }
                    Text(
                        "用 TCP 握手测连通性，比传统 ping 更能反映端口是不是真的能用。需要联网。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                    if (status.isNotBlank()) {
                        Text(status, style = MaterialTheme.typography.bodySmall, color = palette.orange)
                    }
                }
            }
        }
        if (results.isNotEmpty()) {
            item { SectionHeader("结果") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell(
                                "平均延迟",
                                if (avg.isNaN()) "超时" else Math.round(avg).toString() + " ms",
                                Modifier.weight(1f)
                            )
                            StatCell("丢包率", String.format("%.0f%%", lossRate), Modifier.weight(1f))
                            StatCell(
                                "抖动",
                                if (jitter.isNaN()) "—" else Math.round(jitter).toString() + " ms",
                                Modifier.weight(1f)
                            )
                        }
                        if (ok.isNotEmpty()) {
                            Text(
                                "最快 " + ok.min() + " ms，最慢 " + ok.max() + " ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryLabel
                            )
                        }
                    }
                }
            }
            item { SectionHeader("每次结果") }
            item {
                GroupedCard {
                    results.forEachIndexed { index, value ->
                        KeyValueRow(
                            "第 " + (index + 1) + " 次",
                            value?.let { it.toString() + " ms" } ?: "超时",
                            copyable = false
                        )
                        if (index != results.lastIndex) RowDivider()
                    }
                }
            }
        }
        if (scanResults.isNotEmpty()) {
            item { SectionHeader("常用端口扫描") }
            item {
                GroupedCard {
                    scanResults.forEachIndexed { index, (p, latency) ->
                        val name = commonPorts.firstOrNull { it.first == p }?.second ?: ""
                        KeyValueRow(
                            p.toString() + "　" + name,
                            latency?.let { "开放 " + it + " ms" } ?: "关闭或过滤",
                            copyable = false
                        )
                        if (index != scanResults.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
