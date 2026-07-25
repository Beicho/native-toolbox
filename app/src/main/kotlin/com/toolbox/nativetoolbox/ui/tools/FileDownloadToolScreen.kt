package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.concurrent.atomic.AtomicLong

private data class Probe(val size: Long, val acceptRanges: Boolean, val fileName: String)

private fun probe(url: String): Probe {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "HEAD"
    conn.connectTimeout = 8000
    conn.readTimeout = 10000
    conn.instanceFollowRedirects = true
    conn.setRequestProperty("User-Agent", "AstroKit")
    val code = conn.responseCode
    if (code !in 200..299) { conn.disconnect(); throw Exception("服务器返回 $code") }
    val size = conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
    val ranges = conn.getHeaderField("Accept-Ranges")?.contains("bytes") == true
    val cd = conn.getHeaderField("Content-Disposition") ?: ""
    val name = when {
        cd.contains("filename*=") -> URLDecoder.decode(cd.substringAfter("filename*=").substringAfter("''").trim('"', ';', ' '), "UTF-8")
        cd.contains("filename=") -> cd.substringAfter("filename=").trim('"', ';', ' ')
        else -> conn.url.path.substringAfterLast('/').ifBlank { "download.bin" }
    }
    conn.disconnect()
    return Probe(size, ranges, name.ifBlank { "download.bin" })
}

/** 多线程分段下载到临时文件,期间回调进度 */
private suspend fun multiDownload(
    url: String, size: Long, threads: Int, tmp: java.io.File,
    onProgress: (Long) -> Unit,
): Unit = coroutineScope {
    RandomAccessFile(tmp, "rw").use { it.setLength(size) }
    val done = AtomicLong(0)
    val chunk = size / threads
    (0 until threads).map { t ->
        async(Dispatchers.IO) {
            val start = t * chunk
            val end = if (t == threads - 1) size - 1 else (t + 1) * chunk - 1
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 30000
            conn.setRequestProperty("User-Agent", "AstroKit")
            conn.setRequestProperty("Range", "bytes=$start-$end")
            conn.inputStream.use { input ->
                RandomAccessFile(tmp, "rw").use { raf ->
                    raf.seek(start)
                    val buf = ByteArray(128 * 1024)
                    while (isActive) {
                        val r = input.read(buf)
                        if (r == -1) break
                        raf.write(buf, 0, r)
                        onProgress(done.addAndGet(r.toLong()))
                    }
                }
            }
            conn.disconnect()
        }
    }.awaitAll()
}

@Composable
fun FileDownloadToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var speedText by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var done by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    fun start() {
        var u = url.trim()
        if (u.isEmpty()) { status = "先贴下载链接"; return }
        if (!u.startsWith("http")) u = "https://$u"
        running = true; status = "连接中…"; progress = 0f; speedText = ""
        job = scope.launch {
            try {
                val p = withContext(Dispatchers.IO) { probe(u) }
                val tmp = java.io.File(context.cacheDir, "dl_${System.currentTimeMillis()}.part")
                val t0 = System.nanoTime()
                if (p.size > 0 && p.acceptRanges && p.size > 4 * 1024 * 1024) {
                    status = "多线程下载中(4 线程)…"
                    withContext(Dispatchers.IO) {
                        multiDownload(u, p.size, 4, tmp) { got ->
                            progress = got.toFloat() / p.size
                            val secs = (System.nanoTime() - t0) / 1e9
                            if (secs > 0.5) speedText = FileHelper.formatFileSize((got / secs).toLong()) + "/s"
                        }
                    }
                } else {
                    status = "下载中…"
                    withContext(Dispatchers.IO) {
                        val conn = URL(u).openConnection() as HttpURLConnection
                        conn.connectTimeout = 8000
                        conn.readTimeout = 30000
                        conn.setRequestProperty("User-Agent", "AstroKit")
                        conn.inputStream.use { input ->
                            tmp.outputStream().use { out ->
                                val buf = ByteArray(128 * 1024)
                                var got = 0L
                                while (isActive) {
                                    val r = input.read(buf)
                                    if (r == -1) break
                                    out.write(buf, 0, r)
                                    got += r
                                    if (p.size > 0) progress = got.toFloat() / p.size
                                    val secs = (System.nanoTime() - t0) / 1e9
                                    if (secs > 0.5) speedText = FileHelper.formatFileSize((got / secs).toLong()) + "/s"
                                }
                            }
                        }
                        conn.disconnect()
                    }
                }
                status = "保存中…"
                val bytes = withContext(Dispatchers.IO) { tmp.readBytes() }
                tmp.delete()
                val r = withContext(Dispatchers.IO) { FileHelper.saveToDownloads(context, p.fileName, bytes) }
                r.onSuccess {
                    status = ""
                    done = listOf(p.fileName to FileHelper.formatFileSize(bytes.size.toLong())) + done
                    progress = 0f
                }.onFailure { status = "保存失败:${it.message}" }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) status = "下载失败:${e.message ?: "网络错误"}"
            } finally {
                running = false
                speedText = ""
            }
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(url, { url = it }, Modifier.fillMaxWidth(), placeholder = "https://…/file.zip", mono = true)
                    Spacer(Modifier.height(12.dp))
                    if (running && progress > 0) {
                        Box(
                            Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(palette.sunkenBackground)
                        ) {
                            Box(
                                Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)).background(palette.accent)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${(progress * 100).toInt()}%  $speedText",
                            style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    SolidButton(
                        onClick = { if (running) { job?.cancel(); running = false; status = "已取消" } else start() },
                        Modifier.fillMaxWidth()
                    ) { Text(if (running) "取消" else "下载") }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.contains("失败") || status == "已取消") palette.red else palette.secondaryLabel)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("大文件自动 4 线程加速,存到系统下载目录的 AstroKit 文件夹", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                }
            }
        }
        item {
            if (done.isNotEmpty()) {
                GroupedCard {
                    done.forEachIndexed { i, (name, size) ->
                        KeyValueRow(name, size, copyable = false)
                        if (i != done.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
