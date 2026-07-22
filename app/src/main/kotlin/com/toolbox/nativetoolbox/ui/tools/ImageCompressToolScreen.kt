package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.liquid.LiquidButton
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlin.math.roundToInt

@Composable
fun ImageCompressToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sourceSize by remember { mutableStateOf(0L) }
    var quality by rememberSaveable { mutableStateOf(80f) }
    var formatIndex by rememberSaveable { mutableStateOf(0) }
    var resultBytes by remember { mutableStateOf<ByteArray?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            sourceUri = uri
            sourceSize = FileHelper.getFileSize(context, uri)
            sourceBitmap = ImageUtil.loadBitmap(context, uri)
            resultBytes = null
        }
    }

    fun compress() {
        val bmp = sourceBitmap ?: return
        val format = if (formatIndex == 0) Bitmap.CompressFormat.JPEG else ImageUtil.webpFormat()
        resultBytes = ImageUtil.encode(bmp, format, quality.roundToInt())
    }

    fun save() {
        val bytes = resultBytes ?: return
        val ext = if (formatIndex == 0) "jpg" else "webp"
        val mime = if (formatIndex == 0) "image/jpeg" else "image/webp"
        val result = ImageUtil.saveToPictures(context, "compressed_${System.currentTimeMillis()}.$ext", bytes, mime)
        Toast.makeText(
            context,
            if (result.isSuccess) "已保存到 ${result.getOrNull()}" else "保存失败:${result.exceptionOrNull()?.message}",
            Toast.LENGTH_SHORT
        ).show()
    }

    ToolScaffold(title = "图片压缩", onBack = onBack) {
        item { SectionHeader("源图片") }
        item {
            GroupedCard {
                CardPadding {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        LiquidButton(onClick = { picker.launch("image/*") }, tint = palette.accent) {
                            Text("选择图片", color = Color.White)
                        }
                    }
                }
                KeyValueRow("原始大小", if (sourceSize > 0) FileHelper.formatFileSize(sourceSize) else "", copyable = false)
                RowDivider()
                KeyValueRow(
                    "尺寸",
                    sourceBitmap?.let { "${it.width} × ${it.height}" } ?: "",
                    copyable = false
                )
            }
        }
        item { SectionHeader("压缩设置 · 质量 ${quality.roundToInt()}%") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("JPEG", "WebP"),
                        selectedIndex = formatIndex,
                        onSelected = { formatIndex = it; resultBytes = null }
                    )
                    Slider(
                        value = quality,
                        onValueChange = { quality = it; },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = palette.accent,
                            inactiveTrackColor = palette.fill
                        )
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        LiquidButton(onClick = { compress() }, tint = palette.accent, enabled = sourceBitmap != null) {
                            Text("压缩", color = Color.White)
                        }
                        LiquidButton(onClick = { save() }, enabled = resultBytes != null) {
                            Text("保存", color = palette.accent)
                        }
                    }
                }
            }
        }
        item { SectionHeader("结果") }
        item {
            GroupedCard {
                KeyValueRow(
                    "压缩后大小",
                    resultBytes?.let { FileHelper.formatFileSize(it.size.toLong()) } ?: "",
                    copyable = false
                )
                RowDivider()
                KeyValueRow(
                    "压缩率",
                    if (resultBytes != null && sourceSize > 0) {
                        "${(100 - resultBytes!!.size * 100L / sourceSize)}%"
                    } else "",
                    copyable = false
                )
            }
        }
    }
}
