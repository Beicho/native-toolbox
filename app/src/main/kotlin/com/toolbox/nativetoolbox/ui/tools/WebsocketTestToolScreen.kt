package com.toolbox.nativetoolbox.ui.tools

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.Socket
import java.net.URI
import java.security.MessageDigest
import javax.net.ssl.SSLSocketFactory
import kotlin.random.Random

/**
 * 用裸 Socket 实现 RFC 6455 客户端握手与文本帧收发。
 * 只做连通性与消息往返验证，够用来排查「服务端到底通不通、返回什么」。
 */
private class WsSession(
    val socket: Socket,
    val output: OutputStream,
    val input: BufferedInputStream
)

private fun buildKey(): String {
    val bytes = ByteArray(16).also { Random.nextBytes(it) }
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}

private fun expectedAccept(key: String): String {
    val magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    val digest = MessageDigest.getInstance("SHA-1").digest((key + magic).toByteArray())
    return android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP)
}

private fun writeTextFrame(output: OutputStream, text: String) {
    val payload = text.toByteArray(Charsets.UTF_8)
    val mask = ByteArray(4).also { Random.nextBytes(it) }
    output.write(0x81) // FIN + text opcode
    when {
        payload.size < 126 -> output.write(0x80 or payload.size)
        payload.size <= 0xFFFF -> {
            output.write(0x80 or 126)
            output.write((payload.size shr 8) and 0xFF)
            output.write(payload.size and 0xFF)
        }
        else -> {
            output.write(0x80 or 127)
            repeat(4) { output.write(0) }
            output.write((payload.size shr 24) and 0xFF)
            output.write((payload.size shr 16) and 0xFF)
            output.write((payload.size shr 8) and 0xFF)
            output.write(payload.size and 0xFF)
        }
    }
    output.write(mask)
    output.write(ByteArray(payload.size) { (payload[it].toInt() xor mask[it % 4].toInt()).toByte() })
    output.flush()
}

private fun readFrame(input: BufferedInputStream): Pair<Int, String>? {
    val first = input.read()
    if (first < 0) return null
    val opcode = first and 0x0F
    val second = input.read()
    if (second < 0) return null
    var length = second and 0x7F
    if (length == 126) {
        length = (input.read() shl 8) or input.read()
    } else if (length == 127) {
        length = 0
        repeat(8) { length = (length shl 8) or input.read() }
    }
    val payload = ByteArray(length)
    var read = 0
    while (read < length) {
        val r = input.read(payload, read, length - read)
        if (r < 0) break
        read += r
    }
    return opcode to String(payload, 0, read, Charsets.UTF_8)
}

private suspend fun connectWs(urlText: String): Result<WsSession> = withContext(Dispatchers.IO) {
    runCatching {
        val uri = URI(urlText)
        val secure = uri.scheme.equals("wss", true)
        val port = if (uri.port > 0) uri.port else if (secure) 443 else 80
        val path = (uri.rawPath.ifBlank { "/" }) + (uri.rawQuery?.let { "?" + it } ?: "")
        val socket = if (secure) {
            SSLSocketFactory.getDefault().createSocket(uri.host, port)
        } else {
            Socket(uri.host, port)
        }
        socket.soTimeout = 15000
        val key = buildKey()
        val request = buildString {
            append("GET ").append(path).append(" HTTP/1.1\r\n")
            append("Host: ").append(uri.host).append("\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("Sec-WebSocket-Key: ").append(key).append("\r\n")
            append("Sec-WebSocket-Version: 13\r\n")
            append("\r\n")
        }
        val output = socket.getOutputStream()
        output.write(request.toByteArray())
        output.flush()

        val input = BufferedInputStream(socket.getInputStream())
        val header = StringBuilder()
        while (!header.endsWith("\r\n\r\n")) {
            val b = input.read()
            if (b < 0) throw IllegalStateException("服务端在握手阶段就断开了")
            header.append(b.toChar())
        }
        val headerText = header.toString()
        if (!headerText.contains("101")) {
            val firstLine = headerText.lineSequence().firstOrNull() ?: ""
            throw IllegalStateException("服务端拒绝升级：" + firstLine)
        }
        val accept = headerText.lineSequence()
            .firstOrNull { it.startsWith("Sec-WebSocket-Accept", true) }
            ?.substringAfter(':')?.trim()
        if (accept != null && accept != expectedAccept(key)) {
            throw IllegalStateException("握手校验值不对，可能不是标准 WebSocket 服务")
        }
        WsSession(socket, output, input)
    }
}

@Composable
fun WebsocketTestToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var url by rememberSaveable { mutableStateOf("wss://echo.websocket.org") }
    var message by rememberSaveable { mutableStateOf("hello") }
    var connected by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var sentCount by rememberSaveable { mutableStateOf(0) }
    var recvCount by rememberSaveable { mutableStateOf(0) }
    val logs = remember { mutableListOf<String>().toMutableStateList() }
    var session by remember { mutableStateOf<WsSession?>(null) }

    fun log(line: String) {
        logs.add(0, line)
        if (logs.size > 100) logs.removeAt(logs.lastIndex)
    }

    fun disconnect() {
        runCatching { session?.socket?.close() }
        session = null
        connected = false
    }

    DisposableEffect(Unit) { onDispose { runCatching { session?.socket?.close() } } }

    fun connect() {
        busy = true
        status = ""
        logs.clear()
        sentCount = 0
        recvCount = 0
        scope.launch {
            connectWs(url.trim())
                .onSuccess { s ->
                    session = s
                    connected = true
                    log("已连接 " + url.trim())
                    // 后台持续读帧
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            while (true) {
                                val frame = readFrame(s.input) ?: break
                                val (opcode, text) = frame
                                when (opcode) {
                                    0x1 -> {
                                        recvCount += 1
                                        log("收到：" + text)
                                    }
                                    0x8 -> {
                                        log("服务端关闭了连接")
                                        break
                                    }
                                    0x9 -> log("收到 ping")
                                    0xA -> log("收到 pong")
                                }
                            }
                        }
                        connected = false
                    }
                }
                .onFailure { e ->
                    status = e.message ?: "连接失败"
                    connected = false
                }
            busy = false
        }
    }

    fun send() {
        val s = session ?: return
        val text = message
        scope.launch(Dispatchers.IO) {
            runCatching { writeTextFrame(s.output, text) }
                .onSuccess {
                    sentCount += 1
                    log("发送：" + text)
                }
                .onFailure { status = "发送失败：" + (it.message ?: "") }
        }
    }

    ToolScaffold {
        item { SectionHeader("连接") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = "ws:// 或 wss:// 地址",
                        mono = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = { if (connected) disconnect() else connect() },
                            modifier = Modifier.weight(1f),
                            enabled = !busy
                        ) { Text(if (busy) "连接中…" else if (connected) "断开" else "连接") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("状态", if (connected) "已连接" else "未连接", Modifier.weight(1f))
                        StatCell("已发", sentCount.toString(), Modifier.weight(1f))
                        StatCell("已收", recvCount.toString(), Modifier.weight(1f))
                    }
                    if (status.isNotBlank()) {
                        Text(status, style = MaterialTheme.typography.bodySmall, color = palette.red)
                    }
                    Text(
                        "直接从这台手机发起标准 WebSocket 握手，不经过中转。需要联网。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("发送消息") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = "要发送的文本",
                        minHeight = 90.dp,
                        mono = true
                    )
                    SolidButton(onClick = { send() }, enabled = connected) { Text("发送") }
                }
            }
        }
        item { SectionHeader("消息记录（" + logs.size + "）") }
        item {
            GroupedCard {
                CardPadding {
                    if (logs.isEmpty()) {
                        Text(
                            "连接后收发的消息会显示在这里，最新的在最上面。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    } else {
                        OutputCard(text = logs.joinToString("\n"), label = "日志")
                    }
                }
            }
        }
    }
}
