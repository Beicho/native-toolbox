package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** 一条标注:kind 0箭头 1方框 2圆圈 3文字(文字只用起点) */
private data class Anno(val kind: Int, val x1: Float, val y1: Float, val x2: Float, val y2: Float, val text: String, val color: Int)

private val ANNO_COLORS = listOf(
    "红" to AColor.rgb(255, 59, 48), "黄" to AColor.rgb(255, 204, 0),
    "绿" to AColor.rgb(52, 199, 89), "蓝" to AColor.rgb(0, 122, 255),
)

private fun renderAnnos(src: Bitmap, annos: List<Anno>): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)
    val strokeW = src.width / 90f
    for (a in annos) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = a.color; style = Paint.Style.STROKE; strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        when (a.kind) {
            0 -> { // 箭头
                canvas.drawLine(a.x1, a.y1, a.x2, a.y2, paint)
                val ang = atan2((a.y2 - a.y1).toDouble(), (a.x2 - a.x1).toDouble())
                val headLen = strokeW * 4.5f
                for (side in listOf(-1, 1)) {
                    val wing = ang + Math.PI + side * 0.5
                    canvas.drawLine(
                        a.x2, a.y2,
                        a.x2 + (headLen * cos(wing)).toFloat(), a.y2 + (headLen * sin(wing)).toFloat(),
                        paint
                    )
                }
            }
            1 -> canvas.drawRoundRect(
                minOf(a.x1, a.x2), minOf(a.y1, a.y2), maxOf(a.x1, a.x2), maxOf(a.y1, a.y2),
                strokeW * 2, strokeW * 2, paint
            )
            2 -> canvas.drawOval(minOf(a.x1, a.x2), minOf(a.y1, a.y2), maxOf(a.x1, a.x2), maxOf(a.y1, a.y2), paint)
            3 -> {
                val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = a.color; textSize = src.width / 16f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    setShadowLayer(4f, 0f, 2f, AColor.argb(140, 0, 0, 0))
                }
                canvas.drawText(a.text.ifBlank { "标注" }, a.x1, a.y1, tp)
            }
        }
    }
    return out
}

@Composable
fun ImageAnnotateToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var annos by remember { mutableStateOf<List<Anno>>(emptyList()) }
    var kind by rememberSaveable { mutableStateOf(0) }
    var colorIdx by rememberSaveable { mutableStateOf(0) }
    var text by rememberSaveable { mutableStateOf("") }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCur by remember { mutableStateOf<Offset?>(null) }
    var status by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            source = ImageUtil.loadBitmap(context, uri, 1920)
            annos = emptyList()
            status = if (source == null) "图读不出来" else ""
        }
    }

    val preview = remember(source, annos) { source?.let { renderAnnos(it, annos) } }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    val p = preview
                    if (p == null) {
                        Text("选一张图,拖出箭头、框和文字", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        val src = source!!
                        val ratio = src.width.toFloat() / src.height
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio.coerceIn(0.4f, 2.4f))
                                .clip(RoundedCornerShape(12.dp))
                                .pointerInput(src, kind, colorIdx, text) {
                                    detectDragGestures(
                                        onDragStart = { pos -> dragStart = pos; dragCur = pos },
                                        onDrag = { change, _ -> change.consume(); dragCur = change.position },
                                        onDragEnd = {
                                            val s = dragStart; val c = dragCur
                                            if (s != null && c != null) {
                                                val sx = src.width.toFloat() / size.width
                                                val sy = src.height.toFloat() / size.height
                                                annos = annos + Anno(
                                                    kind,
                                                    s.x * sx, s.y * sy, c.x * sx, c.y * sy,
                                                    text, ANNO_COLORS[colorIdx].second
                                                )
                                            }
                                            dragStart = null; dragCur = null
                                        }
                                    )
                                }
                        ) {
                            Image(p.asImageBitmap(), contentDescription = "标注画布", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    SolidButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth(), filled = source == null) {
                        Text(if (source == null) "选图" else "换一张")
                    }
                }
            }
        }
        item {
            if (source != null) {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(listOf("箭头", "方框", "圆圈", "文字"), kind, { kind = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        SegmentedPicker(ANNO_COLORS.map { it.first }, colorIdx, { colorIdx = it }, Modifier.fillMaxWidth())
                        if (kind == 3) {
                            Spacer(Modifier.height(10.dp))
                            IosTextField(text, { text = it }, Modifier.fillMaxWidth(), placeholder = "要写的字(拖一下放位置)")
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth()) {
                            SolidButton(onClick = { annos = annos.dropLast(1) }, Modifier.weight(1f), filled = false, enabled = annos.isNotEmpty()) { Text("撤销") }
                            Spacer(Modifier.size(8.dp))
                            SolidButton(
                                onClick = {
                                    val s = source ?: return@SolidButton
                                    scope.launch {
                                        val bytes = withContext(Dispatchers.Default) {
                                            val full = renderAnnos(s, annos)
                                            val b = ImageUtil.encode(full, Bitmap.CompressFormat.JPEG, 93)
                                            full.recycle(); b
                                        }
                                        val r = withContext(Dispatchers.IO) {
                                            ImageUtil.saveToPictures(context, "annotate_${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")
                                        }
                                        status = r.fold({ "已存到相册" }, { "保存失败:${it.message}" })
                                    }
                                },
                                Modifier.weight(2f), enabled = annos.isNotEmpty()
                            ) { Text("保存") }
                        }
                        if (status.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("已存")) palette.green else palette.red)
                        }
                    }
                }
            }
        }
    }
}
