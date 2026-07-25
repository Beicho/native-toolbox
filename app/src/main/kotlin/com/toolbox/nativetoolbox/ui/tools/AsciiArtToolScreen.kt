package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.theme.MonoStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 字符集按视觉密度从暗到亮排列 */
private val rampSets = listOf(
    "标准" to "@%#*+=-:. ",
    "细腻" to "$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^`'. ",
    "方块" to "█▓▒░ ",
    "数字" to "9876543210 ",
    "点阵" to "●◉◍◌○ "
)

private val widths = listOf(48, 64, 96, 128)

private fun loadBitmap(context: android.content.Context, uri: Uri): Bitmap? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
}.getOrNull()

/**
 * 转字符画：按目标列数等比缩放（字符高宽比约 2:1，所以行数要再减半），
 * 逐像素取灰度映射到字符集。
 */
private fun toAscii(bitmap: Bitmap, columns: Int, ramp: String, invert: Boolean): String {
    val aspect = bitmap.height.toFloat() / bitmap.width
    val rows = (columns * aspect * 0.5f).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(bitmap, columns, rows, true)
    val builder = StringBuilder()
    for (y in 0 until rows) {
        for (x in 0 until columns) {
            val pixel = scaled.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // 感知亮度权重
            val gray = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            val level = if (invert) 1.0 - gray else gray
            val index = (level * (ramp.length - 1)).toInt().coerceIn(0, ramp.length - 1)
            builder.append(ramp[index])
        }
        builder.append('\n')
    }
    if (scaled !== bitmap) scaled.recycle()
    return builder.toString().trimEnd()
}

@Composable
fun AsciiArtToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hScroll = rememberScrollState()

    var rampIndex by rememberSaveable { mutableStateOf(0) }
    var widthIndex by rememberSaveable { mutableStateOf(1) }
    var invert by rememberSaveable { mutableStateOf(false) }
    var art by rememberSaveable { mutableStateOf("") }
    var sourceInfo by rememberSaveable { mutableStateOf("") }
    var working by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    var lastUri by rememberSaveable { mutableStateOf<String?>(null) }

    fun convert(uriText: String) {
        working = true
        error = ""
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                val bitmap = loadBitmap(context, Uri.parse(uriText))
                if (bitmap == null) null
                else {
                    val info = bitmap.width.toString() + " × " + bitmap.height
                    val text = toAscii(bitmap, widths[widthIndex], rampSets[rampIndex].second, invert)
                    bitmap.recycle()
                    info to text
                }
            }
            if (result == null) {
                error = "读不出这张图，换一张试试"
                art = ""
            } else {
                sourceInfo = result.first
                art = result.second
            }
            working = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            lastUri = uri.toString()
            convert(uri.toString())
        }
    }

    ToolScaffold {
        item { SectionHeader("选一张图") }
        item {
            GroupedCard {
                CardPadding {
                    SolidButton(onClick = { picker.launch("image/*") }, enabled = !working) {
                        Text(if (working) "转换中…" else if (art.isBlank()) "从相册选图" else "换一张图")
                    }
                    if (sourceInfo.isNotBlank()) {
                        Text(
                            "原图尺寸 " + sourceInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    }
                    if (error.isNotBlank()) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = palette.red)
                    }
                    Text(
                        "对比度高、主体明确的图效果最好。人像建议先裁到只剩脸。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("参数") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = rampSets.map { it.first },
                        selectedIndex = rampIndex,
                        onSelected = {
                            rampIndex = it
                            lastUri?.let { uri -> convert(uri) }
                        }
                    )
                    SegmentedPicker(
                        options = widths.map { it.toString() + " 列" },
                        selectedIndex = widthIndex,
                        onSelected = {
                            widthIndex = it
                            lastUri?.let { uri -> convert(uri) }
                        }
                    )
                }
                ToggleRow(
                    "反色",
                    invert,
                    onCheckedChange = {
                        invert = it
                        lastUri?.let { uri -> convert(uri) }
                    },
                    subtitle = "深色背景下看着更顺眼"
                )
            }
        }
        if (art.isNotBlank()) {
            item { SectionHeader("预览") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(Modifier.horizontalScroll(hScroll)) {
                            Text(
                                art,
                                style = MonoStyle.copy(
                                    color = palette.label,
                                    fontSize = 5.sp,
                                    lineHeight = 5.sp
                                )
                            )
                        }
                        Text(
                            "这里字号缩得很小只为看整体效果，复制出去用等宽字体看才清楚。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
            item { SectionHeader("统计") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("列数", widths[widthIndex].toString(), Modifier.weight(1f))
                            StatCell("行数", art.lines().size.toString(), Modifier.weight(1f))
                            StatCell("字符数", art.length.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SectionHeader("复制") }
            item { GroupedCard { CardPadding { OutputCard(text = art, label = "字符画") } } }
        }
        item { SectionHeader("字符集说明") }
        item {
            GroupedCard {
                rampSets.forEachIndexed { index, (name, ramp) ->
                    KeyValueRow(name, ramp.take(20) + if (ramp.length > 20) "…" else "", copyable = false)
                    if (index != rampSets.lastIndex) RowDivider()
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "图片只在本机内存里处理，不保存、不上传。选图用的是系统相册选择器，不需要存储权限。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
