package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun ImageConvertToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sourceSize by remember { mutableStateOf(0L) }
    var formatIndex by rememberSaveable { mutableStateOf(0) }
    var savedInfo by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            sourceUri = uri
            sourceSize = FileHelper.getFileSize(context, uri)
            sourceBitmap = ImageUtil.loadBitmap(context, uri)
            savedInfo = ""
        }
    }

    fun convertAndSave() {
        val bmp = sourceBitmap ?: return
        val (format, ext, mime) = when (formatIndex) {
            0 -> Triple(Bitmap.CompressFormat.PNG, "png", "image/png")
            1 -> Triple(Bitmap.CompressFormat.JPEG, "jpg", "image/jpeg")
            else -> Triple(ImageUtil.webpFormat(), "webp", "image/webp")
        }
        val quality = if (formatIndex == 0) 100 else 95
        val bytes = ImageUtil.encode(bmp, format, quality)
        val result = ImageUtil.saveToPictures(context, "converted_${System.currentTimeMillis()}.$ext", bytes, mime)
        if (result.isSuccess) {
            savedInfo = "${result.getOrNull()}(${FileHelper.formatFileSize(bytes.size.toLong())})"
            Toast.makeText(context, "转换完成", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "保存失败:${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    ToolScaffold(title = "格式转换", onBack = onBack) {
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
                KeyValueRow("尺寸", sourceBitmap?.let { "${it.width} × ${it.height}" } ?: "", copyable = false)
            }
        }
        item { SectionHeader("目标格式") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("PNG", "JPEG", "WebP"),
                        selectedIndex = formatIndex,
                        onSelected = { formatIndex = it }
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        LiquidButton(
                            onClick = { convertAndSave() },
                            tint = palette.accent,
                            enabled = sourceBitmap != null
                        ) {
                            Text("转换并保存", color = Color.White)
                        }
                    }
                }
                KeyValueRow("输出", savedInfo, copyable = false)
            }
        }
    }
}
