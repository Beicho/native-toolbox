package com.toolbox.nativetoolbox.util

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 局域网文件互传的迷你 HTTP 服务器:手机分享文件给电脑,电脑网页也能传文件回手机。
 * 只监听局域网,不经过任何外部服务器。
 */
class HttpShareServer(private val context: Context) {

    data class ShareFile(val name: String, val size: Long, val uri: Uri)

    @Volatile var shares: List<ShareFile> = emptyList()
    var onUpload: ((String) -> Unit)? = null

    private var server: ServerSocket? = null
    private val running = AtomicBoolean(false)
    private val pool = Executors.newFixedThreadPool(4)

    val port = 8462

    fun start(): Boolean {
        if (running.get()) return true
        return runCatching {
            val s = ServerSocket()
            s.reuseAddress = true
            s.bind(InetSocketAddress(port))
            server = s
            running.set(true)
            Thread {
                while (running.get()) {
                    val client = runCatching { s.accept() }.getOrNull() ?: break
                    pool.execute { runCatching { handle(client) }; runCatching { client.close() } }
                }
            }.apply { isDaemon = true }.start()
            true
        }.getOrDefault(false)
    }

    fun stop() {
        running.set(false)
        runCatching { server?.close() }
        server = null
    }

    private fun handle(sock: Socket) {
        sock.soTimeout = 60_000
        val input = BufferedInputStream(sock.getInputStream())
        val out = BufferedOutputStream(sock.getOutputStream())
        val requestLine = readLine(input) ?: return
        val parts = requestLine.split(" ")
        if (parts.size < 2) return
        val method = parts[0]
        val path = parts[1]

        // 头
        val headers = HashMap<String, String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val idx = line.indexOf(':')
            if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
        }

        when {
            method == "GET" && path == "/" -> respondHtml(out)
            method == "GET" && path.startsWith("/f/") -> {
                val idx = path.removePrefix("/f/").substringBefore('?').toIntOrNull()
                val f = idx?.let { shares.getOrNull(it) }
                if (f == null) respond(out, 404, "text/plain", "not found".toByteArray())
                else respondFile(out, f)
            }
            method == "POST" && path == "/upload" -> handleUpload(input, out, headers)
            else -> respond(out, 404, "text/plain", "not found".toByteArray())
        }
        out.flush()
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c == -1) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) break
            if (c != '\r'.code) sb.append(c.toChar())
            if (sb.length > 16384) break
        }
        return sb.toString()
    }

    private fun respond(out: OutputStream, code: Int, type: String, body: ByteArray) {
        val status = if (code == 200) "200 OK" else "$code Error"
        out.write("HTTP/1.1 $status\r\nContent-Type: $type; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(body)
    }

    private fun respondHtml(out: OutputStream) {
        val rows = shares.mapIndexed { i, f ->
            """<li><a href="/f/$i" download="${f.name.replace("\"", "")}">${f.name}</a><span>${FileHelper.formatFileSize(f.size)}</span></li>"""
        }.joinToString("")
        val html = """<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Astro Kit 手机网盘</title><style>
body{font-family:system-ui;max-width:560px;margin:24px auto;padding:0 16px;background:#f2f2f7;color:#111}
h2{font-size:20px} .card{background:#fff;border-radius:14px;padding:18px;margin-bottom:16px;box-shadow:0 1px 4px rgba(0,0,0,.06)}
ul{list-style:none;padding:0;margin:0} li{display:flex;justify-content:space-between;padding:10px 0;border-bottom:1px solid #eee}
li:last-child{border:none} a{color:#3478f6;text-decoration:none;word-break:break-all;margin-right:12px} span{color:#999;white-space:nowrap}
input[type=file]{margin:8px 0} button{background:#3478f6;color:#fff;border:0;border-radius:9px;padding:10px 18px;font-size:15px}
.empty{color:#999}</style></head><body>
<h2>手机上的文件</h2><div class="card">${if (rows.isEmpty()) "<p class=\"empty\">手机还没分享文件</p>" else "<ul>$rows</ul>"}</div>
<h2>传文件到手机</h2><div class="card"><form method="post" action="/upload" enctype="multipart/form-data">
<input type="file" name="file" multiple><br><button type="submit">上传</button></form></div>
</body></html>"""
        respond(out, 200, "text/html", html.toByteArray())
    }

    private fun respondFile(out: OutputStream, f: ShareFile) {
        val stream = runCatching { context.contentResolver.openInputStream(f.uri) }.getOrNull()
        if (stream == null) { respond(out, 404, "text/plain", "gone".toByteArray()); return }
        val encoded = URLEncoder.encode(f.name, "UTF-8").replace("+", "%20")
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\n" +
            "Content-Disposition: attachment; filename*=UTF-8''$encoded\r\n" +
            (if (f.size > 0) "Content-Length: ${f.size}\r\n" else "") +
            "Connection: close\r\n\r\n").toByteArray())
        stream.use { it.copyTo(out, 64 * 1024) }
    }

    /** multipart/form-data 解析:按 boundary 切文件段,流式写盘 */
    private fun handleUpload(input: InputStream, out: OutputStream, headers: Map<String, String>) {
        val ct = headers["content-type"] ?: ""
        val boundary = ct.substringAfter("boundary=", "").trim().removeSurrounding("\"")
        if (boundary.isEmpty()) { respond(out, 400, "text/plain", "bad request".toByteArray()); return }
        val len = headers["content-length"]?.toLongOrNull() ?: -1
        val body = if (len >= 0) input.readNBytesCompat(len) else input.readBytes()
        val marker = "--$boundary".toByteArray()
        var saved = 0
        var pos = indexOf(body, marker, 0)
        while (pos >= 0) {
            val headStart = pos + marker.size + 2 // \r\n
            if (headStart >= body.size) break
            val headEnd = indexOf(body, "\r\n\r\n".toByteArray(), headStart)
            if (headEnd < 0) break
            val head = String(body, headStart, headEnd - headStart)
            val next = indexOf(body, marker, headEnd + 4)
            val dataEnd = if (next >= 0) next - 2 else body.size // 去掉尾部 \r\n
            if (head.contains("filename=")) {
                var name = head.substringAfter("filename=\"").substringBefore("\"")
                name = URLDecoder.decode(name, "UTF-8").substringAfterLast('/').substringAfterLast('\\')
                if (name.isNotBlank() && dataEnd > headEnd + 4) {
                    val data = body.copyOfRange(headEnd + 4, dataEnd)
                    if (FileHelper.saveToDownloads(context, name, data).isSuccess) {
                        saved++
                        onUpload?.invoke(name)
                    }
                }
            }
            if (next < 0) break
            pos = next
        }
        val html = """<html><head><meta charset="utf-8"><meta http-equiv="refresh" content="1;url=/"></head>
<body style="font-family:system-ui;text-align:center;padding-top:60px">已收到 $saved 个文件,正在返回…</body></html>"""
        respond(out, 200, "text/html", html.toByteArray())
    }

    private fun InputStream.readNBytesCompat(n: Long): ByteArray {
        val cap = n.coerceAtMost(256L * 1024 * 1024).toInt() // 上限 256MB
        val buf = ByteArray(cap)
        var off = 0
        while (off < cap) {
            val r = read(buf, off, cap - off)
            if (r == -1) break
            off += r
        }
        return if (off == cap) buf else buf.copyOf(off)
    }

    private fun indexOf(data: ByteArray, pattern: ByteArray, from: Int): Int {
        outer@ for (i in from..data.size - pattern.size) {
            for (j in pattern.indices) {
                if (data[i + j] != pattern[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
