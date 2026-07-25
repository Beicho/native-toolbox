package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
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

/** 经典梗图排版:图片上下加粗白字黑边;或者下方白底黑字字幕条 */
private fun renderMeme(src: Bitmap, top: String, bottom: String, layout: Int): Bitmap {
    val w = src.width
    return if (layout == 0) {
        // 白字黑边压在图上
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val size = w / 9f
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            style = Paint.Style.STROKE; strokeWidth = size / 8f; color = AColor.BLACK
        }
        val fill = Paint(stroke).apply { style = Paint.Style.FILL; color = AColor.WHITE }
        fun drawLine(text: String, y: Float) {
            if (text.isBlank()) return
            canvas.drawText(text, w / 2f, y, stroke)
            canvas.drawText(text, w / 2f, y, fill)
        }
        drawLine(top, size * 1.2f)
        drawLine(bottom, out.height - size * 0.5f)
        out
    } else {
        // 下方字幕条(可多行,黑字白底)
        val lines = (top + "\n" + bottom).split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        val size = w / 12f
        val lineH = size * 1.5f
        val barH = (lines.size * lineH + size * 0.6f).toInt()
        val out = Bitmap.createBitmap(w, src.height + barH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(AColor.WHITE)
        canvas.drawBitmap(src, 0f, 0f, null)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; textAlign = Paint.Align.CENTER; color = AColor.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        lines.forEachIndexed { i, line ->
            canvas.drawText(line, w / 2f, src.height + lineH * (i + 1) - size * 0.25f, p)
        }
        out
    }
}

@Composable
fun MemeMakerToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var top by rememberSaveable { mutableStateOf("") }
    var bottom by rememberSaveable { mutableStateOf("") }
    var style by rememberSaveable { mutableStateOf(0) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            source = ImageUtil.loadBitmap(context, uri, 1440)
            preview = null
            status = if (source == null) "图读不出来" else ""
        }
    }

    fun refresh() {
        val s = source ?: return
        scope.launch {
            preview = withContext(Dispatchers.Default) { renderMeme(s, top.trim(), bottom.trim(), style) }
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    val p = preview ?: source
                    if (p == null) {
                        Text("选一张图,配上你的梗", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Image(p.asImageBitmap(), contentDescription = "表情包预览", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                        Spacer(Modifier.height(12.dp))
                    }
                    SolidButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth(), filled = source == null) {
                        Text(if (source == null) "选图" else "换一张")
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("白字压图", "下方字幕"), style, { style = it; refresh() }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    IosTextField(top, { top = it }, Modifier.fillMaxWidth(), placeholder = if (style == 0) "上方文字" else "第一行字幕")
                    Spacer(Modifier.height(8.dp))
                    IosTextField(bottom, { bottom = it }, Modifier.fillMaxWidth(), placeholder = if (style == 0) "下方文字" else "第二行字幕(可空)")
                    Spacer(Modifier.height(10.dp))
                    SolidButton(onClick = { refresh() }, Modifier.fillMaxWidth(), filled = false, enabled = source != null) { Text("预览") }
                    Spacer(Modifier.height(8.dp))
                    SolidButton(
                        onClick = {
                            val s = source ?: return@SolidButton
                            scope.launch {
                                val final = withContext(Dispatchers.Default) { renderMeme(s, top.trim(), bottom.trim(), style) }
                                preview = final
                                val bytes = withContext(Dispatchers.Default) { ImageUtil.encode(final, Bitmap.CompressFormat.JPEG, 92) }
                                val r = withContext(Dispatchers.IO) {
                                    ImageUtil.saveToPictures(context, "meme_${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")
                                }
                                status = r.fold({ "已存到相册" }, { "保存失败:${it.message}" })
                            }
                        },
                        Modifier.fillMaxWidth(),
                        enabled = source != null && (top.isNotBlank() || bottom.isNotBlank())
                    ) { Text("保存") }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("已存")) palette.green else palette.red)
                    }
                }
            }
        }
    }
}
