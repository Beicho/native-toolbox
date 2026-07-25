package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * 测速走 Cloudflare 公共测速端点(全球任播,国内也能连):
 * 下行 GET speed.cloudflare.com/__down?bytes=N,上行 POST /__up。
 */
private const val DOWN_URL = "https://speed.cloudflare.com/__down?bytes=100000000"
private const val UP_URL = "https://speed.cloudflare.com/__up"

private fun pingMs(host: String, port: Int = 443): Long? = runCatching {
    val start = System.nanoTime()
    Socket().use { it.connect(InetSocketAddress(host, port), 3000) }
    (System.nanoTime() - start) / 1_000_000
}.getOrNull()

@Composable
fun SpeedTestToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("") }
    var ping by remember { mutableStateOf<Long?>(null) }
    var jitter by remember { mutableStateOf<Long?>(null) }
    var downMbps by remember { mutableStateOf(0.0) }
    var upMbps by remember { mutableStateOf(0.0) }
    var liveMbps by remember { mutableStateOf(0.0) }
    var error by remember { mutableStateOf("") }
    var job by remember { mutableStateOf<Job?>(null) }

    fun start() {
        running = true; error = ""; ping = null; jitter = null; downMbps = 0.0; upMbps = 0.0; liveMbps = 0.0
        job = scope.launch {
            try {
                // 1. 延迟:5 次 TCP 握手取中位数,极差当抖动
                phase = "测延迟…"
                val pings = withContext(Dispatchers.IO) {
                    (1..5).mapNotNull { pingMs("speed.cloudflare.com") }
                }
                if (pings.isEmpty()) { error = "连不上测速服务,检查一下网络"; running = false; return@launch }
                ping = pings.sorted()[pings.size / 2]
                jitter = (pings.max() - pings.min())

                // 2. 下行:10 秒内能拉多少
                phase = "测下载…"
                val downBytes = withContext(Dispatchers.IO) {
                    var total = 0L
                    val deadline = System.nanoTime() + 10_000_000_000L
                    runCatching {
                        val conn = URL(DOWN_URL).openConnection() as HttpURLConnection
                        conn.connectTimeout = 5000
                        conn.readTimeout = 15000
                        conn.inputStream.use { input ->
                            val buf = ByteArray(64 * 1024)
                            val t0 = System.nanoTime()
                            while (isActive && System.nanoTime() < deadline) {
                                val r = input.read(buf)
                                if (r == -1) break
                                total += r
                                val secs = (System.nanoTime() - t0) / 1e9
                                if (secs > 0.5) liveMbps = total * 8 / 1e6 / secs
                            }
                        }
                        conn.disconnect()
                    }
                    total
                }
                val downSecs = 10.0.coerceAtMost(10.0)
                downMbps = downBytes * 8 / 1e6 / downSecs
                if (downBytes == 0L) { error = "下载测速失败,网络可能不稳定"; running = false; return@launch }

                // 3. 上行:8 秒内能推多少
                phase = "测上传…"
                liveMbps = 0.0
                val (upBytes, upSecs) = withContext(Dispatchers.IO) {
                    var total = 0L
                    val t0 = System.nanoTime()
                    val deadline = t0 + 8_000_000_000L
                    runCatching {
                        val conn = URL(UP_URL).openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.doOutput = true
                        conn.setChunkedStreamingMode(64 * 1024)
                        conn.connectTimeout = 5000
                        conn.setRequestProperty("Content-Type", "application/octet-stream")
                        conn.outputStream.use { out ->
                            val buf = ByteArray(64 * 1024)
                            while (isActive && System.nanoTime() < deadline) {
                                out.write(buf)
                                total += buf.size
                                val secs = (System.nanoTime() - t0) / 1e9
                                if (secs > 0.5) liveMbps = total * 8 / 1e6 / secs
                            }
                        }
                        conn.responseCode
                        conn.disconnect()
                    }
                    total to ((System.nanoTime() - t0) / 1e9)
                }
                if (upBytes > 0 && upSecs > 0.3) upMbps = upBytes * 8 / 1e6 / upSecs
                phase = "完成"
            } finally {
                running = false
                liveMbps = 0.0
            }
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (running) {
                            Text(phase, style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (liveMbps > 0) "%.1f".format(liveMbps) else "…",
                                fontSize = 64.sp, fontWeight = FontWeight.Bold, color = palette.accent
                            )
                            Text("Mbps", style = MaterialTheme.typography.titleMedium, color = palette.secondaryLabel)
                        } else if (downMbps > 0) {
                            Text("下载", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                            Text("%.1f".format(downMbps), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = palette.label)
                            Text("Mbps", style = MaterialTheme.typography.titleMedium, color = palette.secondaryLabel)
                        } else {
                            Text("测一下当前网络的真实速度", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                            Spacer(Modifier.height(4.dp))
                            Text("会消耗 100~300MB 流量,连 WiFi 时随便测", style = MaterialTheme.typography.bodySmall, color = palette.orange)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    SolidButton(
                        onClick = { if (running) { job?.cancel(); running = false } else start() },
                        Modifier.fillMaxWidth()
                    ) { Text(if (running) "停止" else if (downMbps > 0) "再测一次" else "开始测速") }
                    if (error.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                    }
                }
            }
        }
        item {
            if (downMbps > 0 || ping != null) {
                GroupedCard {
                    CardPadding {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("延迟", ping?.let { "$it ms" } ?: "—", Modifier.weight(1f))
                            StatCell("抖动", jitter?.let { "$it ms" } ?: "—", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("下载", if (downMbps > 0) "%.1f Mbps".format(downMbps) else "—", Modifier.weight(1f))
                            StatCell("上传", if (upMbps > 0) "%.1f Mbps".format(upMbps) else "—", Modifier.weight(1f))
                        }
                        if (downMbps > 0) {
                            Spacer(Modifier.height(10.dp))
                            val verdict = when {
                                downMbps >= 90 -> "很快:4K 视频、大型下载都轻松"
                                downMbps >= 30 -> "够用:高清视频、视频通话没问题"
                                downMbps >= 8 -> "一般:刷视频可以,大文件要等"
                                else -> "偏慢:适合聊天刷网页,看视频会转圈"
                            }
                            Text(verdict, style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                        }
                    }
                }
            }
        }
    }
}
