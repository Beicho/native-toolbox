package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PDF 工具箱:合并 / 拆分 / 抽页 / 转图片。
 * 系统 PdfRenderer 渲染成位图重排版,加密 PDF 打不开;文字型 PDF 处理后会变成图片型(体积可能增大)。
 */
private fun copyToCache(context: android.content.Context, uri: Uri, name: String): File? = runCatching {
    val f = File(context.cacheDir, name)
    context.contentResolver.openInputStream(uri)?.use { ins -> f.outputStream().use { ins.copyTo(it) } } ?: return null
    f
}.getOrNull()

private fun pageCount(f: File): Int = runCatching {
    ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { it.pageCount }
    }
}.getOrDefault(0)

/** 渲染指定页为 Bitmap(150dpi 近似:宽 1240) */
private fun renderPage(renderer: PdfRenderer, index: Int): Bitmap {
    val page = renderer.openPage(index)
    val scale = 1240f / page.width
    val bmp = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
    bmp.eraseColor(android.graphics.Color.WHITE)
    page.render(bmp, null, android.graphics.Matrix().apply { setScale(scale, scale) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    page.close()
    return bmp
}

/** 把多个 PDF 的选定页写成一个新 PDF */
private fun writePdf(sources: List<Pair<File, List<Int>>>, out: java.io.OutputStream) {
    val doc = PdfDocument()
    var pageNo = 1
    for ((file, pages) in sources) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (p in pages) {
                    if (p !in 0 until renderer.pageCount) continue
                    val bmp = renderPage(renderer, p)
                    val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, pageNo++).create()
                    val page = doc.startPage(info)
                    page.canvas.drawBitmap(bmp, 0f, 0f, null)
                    doc.finishPage(page)
                    bmp.recycle()
                }
            }
        }
    }
    doc.writeTo(out)
    doc.close()
}

/** "1-3,5,7" → [0,1,2,4,6] */
private fun parsePages(text: String, total: Int): List<Int> {
    if (text.isBlank()) return (0 until total).toList()
    val out = sortedSetOf<Int>()
    for (part in text.split(',', '，', ' ')) {
        val p = part.trim()
        if (p.isEmpty()) continue
        if (p.contains('-')) {
            val a = p.substringBefore('-').trim().toIntOrNull() ?: continue
            val b = p.substringAfter('-').trim().toIntOrNull() ?: continue
            for (i in a..b) if (i in 1..total) out.add(i - 1)
        } else {
            p.toIntOrNull()?.let { if (it in 1..total) out.add(it - 1) }
        }
    }
    return out.toList()
}

@Composable
fun PdfToolsToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf(0) } // 0 合并 1 抽页/拆分 2 转图片
    var files by remember { mutableStateOf<List<Pair<File, Int>>>(emptyList()) } // 文件+页数
    var pageSpec by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                uris.mapIndexedNotNull { i, u ->
                    val f = copyToCache(context, u, "pdf_${System.currentTimeMillis()}_$i.pdf") ?: return@mapIndexedNotNull null
                    val n = pageCount(f)
                    if (n <= 0) { f.delete(); null } else f to n
                }
            }
            files = if (mode == 0) files + loaded else loaded.take(1)
            status = if (loaded.isEmpty()) "打不开:可能是加密 PDF 或文件损坏" else ""
        }
    }

    fun run() {
        if (files.isEmpty()) { status = "先选 PDF"; return }
        busy = true; status = "处理中…"
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    when (mode) {
                        0 -> { // 合并全部
                            val buf = java.io.ByteArrayOutputStream()
                            writePdf(files.map { it.first to (0 until it.second).toList() }, buf)
                            FileHelper.saveToDownloads(context, "合并_${System.currentTimeMillis()}.pdf", buf.toByteArray()).getOrThrow()
                        }
                        1 -> { // 抽页
                            val (f, n) = files.first()
                            val pages = parsePages(pageSpec, n)
                            if (pages.isEmpty()) throw Exception("页码没写对,像「1-3,5」这样填")
                            val buf = java.io.ByteArrayOutputStream()
                            writePdf(listOf(f to pages), buf)
                            FileHelper.saveToDownloads(context, "抽页_${System.currentTimeMillis()}.pdf", buf.toByteArray()).getOrThrow()
                        }
                        else -> { // 转图片
                            val (f, n) = files.first()
                            val pages = parsePages(pageSpec, n)
                            var saved = 0
                            ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                                PdfRenderer(pfd).use { renderer ->
                                    for (p in pages.take(50)) {
                                        val bmp = renderPage(renderer, p)
                                        val bytes = com.toolbox.nativetoolbox.util.ImageUtil.encode(bmp, Bitmap.CompressFormat.JPEG, 92)
                                        bmp.recycle()
                                        if (com.toolbox.nativetoolbox.util.ImageUtil.saveToPictures(context, "pdf_p${p + 1}_${System.currentTimeMillis()}.jpg", bytes, "image/jpeg").isSuccess) saved++
                                    }
                                }
                            }
                            "相册(共 $saved 张)"
                        }
                    }
                }
            }
            status = r.fold({ "完成,已存到 $it" }, { "失败:${it.message}" })
            busy = false
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("合并", "抽页", "转图片"), mode, { mode = it; files = emptyList(); status = "" }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when (mode) {
                            0 -> "选多个 PDF,按顺序拼成一个"
                            1 -> "从一个 PDF 里抽出想要的页,或反过来当拆分用"
                            else -> "把 PDF 每页导出成图片"
                        },
                        style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel
                    )
                    Spacer(Modifier.height(10.dp))
                    SolidButton(onClick = { picker.launch("application/pdf") }, Modifier.fillMaxWidth(), filled = files.isEmpty()) {
                        Text(if (files.isEmpty()) "选 PDF" else if (mode == 0) "继续加" else "换一个")
                    }
                    if (mode != 0 && files.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("页码(共 ${files.first().second} 页,空 = 全部)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(4.dp))
                        IosTextField(pageSpec, { pageSpec = it }, Modifier.fillMaxWidth(), placeholder = "1-3,5")
                    }
                    if (files.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        SolidButton(onClick = { run() }, Modifier.fillMaxWidth(), enabled = !busy) {
                            Text(if (busy) "处理中…" else listOf("合并", "抽页", "导出图片")[mode])
                        }
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("失败") || status.startsWith("打不开")) palette.red else palette.green)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("处理后是图片型 PDF,文字不能再选中;加密 PDF 不支持", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                }
            }
        }
        item {
            if (files.isNotEmpty()) {
                GroupedCard {
                    files.forEachIndexed { i, (f, n) ->
                        KeyValueRow("第 ${i + 1} 个", "$n 页 · ${FileHelper.formatFileSize(f.length())}", copyable = false)
                        if (i != files.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
