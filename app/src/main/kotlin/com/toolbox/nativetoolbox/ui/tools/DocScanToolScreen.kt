package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.toolbox.nativetoolbox.util.rememberPermission

/** 四角透视矫正:corners 为归一化坐标(左上/右上/右下/左下) */
private fun perspectiveCorrect(src: Bitmap, corners: List<Offset>): Bitmap {
    val srcPts = corners.flatMap { listOf(it.x * src.width, it.y * src.height) }.toFloatArray()
    // 目标尺寸:上下边平均宽 × 左右边平均高
    fun dist(a: Int, b: Int): Float {
        val dx = srcPts[a * 2] - srcPts[b * 2]; val dy = srcPts[a * 2 + 1] - srcPts[b * 2 + 1]
        return kotlin.math.hypot(dx, dy)
    }
    val w = ((dist(0, 1) + dist(3, 2)) / 2).toInt().coerceIn(200, 3000)
    val h = ((dist(0, 3) + dist(1, 2)) / 2).toInt().coerceIn(200, 4000)
    val dstPts = floatArrayOf(0f, 0f, w.toFloat(), 0f, w.toFloat(), h.toFloat(), 0f, h.toFloat())
    val m = Matrix()
    m.setPolyToPoly(srcPts, 0, dstPts, 0, 4)
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.eraseColor(android.graphics.Color.WHITE)
    Canvas(out).drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    return out
}

/** 文档增强:0 原色 1 增强(提对比) 2 黑白 */
private fun enhance(src: Bitmap, mode: Int): Bitmap {
    if (mode == 0) return src
    val cm = if (mode == 1) ColorMatrix(floatArrayOf(
        1.35f, 0f, 0f, 0f, -34f,
        0f, 1.35f, 0f, 0f, -34f,
        0f, 0f, 1.35f, 0f, -34f,
        0f, 0f, 0f, 1f, 0f,
    )) else ColorMatrix().apply {
        setSaturation(0f)
        postConcat(ColorMatrix(floatArrayOf(
            1.9f, 0f, 0f, 0f, -105f,
            0f, 1.9f, 0f, 0f, -105f,
            0f, 0f, 1.9f, 0f, -105f,
            0f, 0f, 0f, 1f, 0f,
        )))
    }
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
    return out
}

@Composable
fun DocScanToolScreen(onBack: () -> Unit) {
    val cameraOk = rememberPermission(android.Manifest.permission.CAMERA)
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Bitmap?>(null) }
    var corners by remember { mutableStateOf(listOf(Offset(0.08f, 0.08f), Offset(0.92f, 0.08f), Offset(0.92f, 0.92f), Offset(0.08f, 0.92f))) }
    var enhanceMode by rememberSaveable { mutableStateOf(1) }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            editing = ImageUtil.loadBitmap(context, uri, 2200)
            corners = listOf(Offset(0.08f, 0.08f), Offset(0.92f, 0.08f), Offset(0.92f, 0.92f), Offset(0.08f, 0.92f))
            status = if (editing == null) "图读不出来" else ""
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            editing = bmp
            corners = listOf(Offset(0.08f, 0.08f), Offset(0.92f, 0.08f), Offset(0.92f, 0.92f), Offset(0.08f, 0.92f))
        }
    }

    fun confirmPage() {
        val e = editing ?: return
        busy = true
        scope.launch {
            val page = withContext(Dispatchers.Default) {
                val corrected = perspectiveCorrect(e, corners)
                enhance(corrected, enhanceMode).also { if (it !== corrected) corrected.recycle() }
            }
            pages = pages + page
            editing = null
            status = "已收第 ${pages.size} 页,继续拍或直接导出"
            busy = false
        }
    }

    fun exportPdf() {
        if (pages.isEmpty()) return
        busy = true; status = "生成 PDF…"
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val doc = PdfDocument()
                    pages.forEachIndexed { i, p ->
                        val info = PdfDocument.PageInfo.Builder(p.width, p.height, i + 1).create()
                        val pg = doc.startPage(info)
                        pg.canvas.drawBitmap(p, 0f, 0f, null)
                        doc.finishPage(pg)
                    }
                    val buf = java.io.ByteArrayOutputStream()
                    doc.writeTo(buf)
                    doc.close()
                    FileHelper.saveToDownloads(context, "扫描_${System.currentTimeMillis()}.pdf", buf.toByteArray()).getOrThrow()
                }
            }
            status = r.fold({ "PDF 已存到 $it" }, { "导出失败:${it.message}" })
            if (r.isSuccess) { pages.forEach { it.recycle() }; pages = emptyList() }
            busy = false
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    val e = editing
                    if (e != null) {
                        val ratio = e.width.toFloat() / e.height
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio.coerceIn(0.5f, 2f))
                                .clip(RoundedCornerShape(12.dp))
                                .pointerInput(e) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                        val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                        // 动最近的角
                                        val idx = corners.indices.minByOrNull { i ->
                                            val dx = corners[i].x - nx; val dy = corners[i].y - ny
                                            dx * dx + dy * dy
                                        } ?: 0
                                        corners = corners.toMutableList().also { it[idx] = Offset(nx, ny) }
                                    }
                                }
                        ) {
                            Image(e.asImageBitmap(), contentDescription = "扫描原图", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                            ComposeCanvas(Modifier.fillMaxSize()) {
                                val pts = corners.map { Offset(it.x * size.width, it.y * size.height) }
                                for (i in pts.indices) {
                                    drawLine(Color(0xFF34C759), pts[i], pts[(i + 1) % 4], strokeWidth = 4f)
                                }
                                pts.forEach {
                                    drawCircle(Color.White, 20f, it)
                                    drawCircle(Color(0xFF34C759), 20f, it, style = Stroke(5f))
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("拖四个角对齐纸的边缘", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(10.dp))
                        SegmentedPicker(listOf("原色", "增强", "黑白"), enhanceMode, { enhanceMode = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth()) {
                            SolidButton(onClick = { editing = null }, Modifier.weight(1f), filled = false) { Text("弃用") }
                            Spacer(Modifier.width(8.dp))
                            SolidButton(onClick = { confirmPage() }, Modifier.weight(2f), enabled = !busy) { Text(if (busy) "矫正中…" else "矫正并收下这页") }
                        }
                    } else {
                        Text(
                            if (pages.isEmpty()) "把纸质文件拍成规整的 PDF" else "已收 ${pages.size} 页",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (pages.isEmpty()) palette.tertiaryLabel else palette.label
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth()) {
                            SolidButton(onClick = {
                                runCatching { camera.launch(null) }.onFailure { status = "相机打不开,先给 Astro Kit 相机权限,或直接选图" }
                            }, Modifier.weight(1f)) { Text("拍一页") }
                            Spacer(Modifier.width(8.dp))
                            SolidButton(onClick = { picker.launch("image/*") }, Modifier.weight(1f), filled = false) { Text("从相册选") }
                        }
                        if (pages.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            SolidButton(onClick = { exportPdf() }, Modifier.fillMaxWidth(), enabled = !busy) {
                                Text(if (busy) "导出中…" else "导出 PDF(${pages.size} 页)")
                            }
                            Spacer(Modifier.height(8.dp))
                            SolidButton(onClick = { pages.forEach { it.recycle() }; pages = emptyList(); status = "" }, Modifier.fillMaxWidth(), filled = false) { Text("清空重来") }
                        }
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.contains("失败") || status.contains("打不开")) palette.red else palette.green)
                    }
                }
            }
        }
        item {
            if (pages.isNotEmpty() && editing == null) {
                GroupedCard {
                    CardPadding {
                        Row(Modifier.fillMaxWidth()) {
                            pages.takeLast(4).forEach { p ->
                                Image(
                                    p.asImageBitmap(), contentDescription = null,
                                    modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
