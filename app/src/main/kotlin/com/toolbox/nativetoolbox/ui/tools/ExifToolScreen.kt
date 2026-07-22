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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil

private val exifTags = listOf(
    "拍摄时间" to ExifInterface.TAG_DATETIME_ORIGINAL,
    "相机品牌" to ExifInterface.TAG_MAKE,
    "相机型号" to ExifInterface.TAG_MODEL,
    "光圈 F" to ExifInterface.TAG_F_NUMBER,
    "曝光时间" to ExifInterface.TAG_EXPOSURE_TIME,
    "ISO" to ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    "焦距" to ExifInterface.TAG_FOCAL_LENGTH,
    "软件" to ExifInterface.TAG_SOFTWARE
)

@Composable
fun ExifToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var tags by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var gps by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u != null) {
            uri = u
            runCatching {
                context.contentResolver.openInputStream(u)?.use { input ->
                    val exif = ExifInterface(input)
                    tags = exifTags.map { (name, tag) -> name to (exif.getAttribute(tag) ?: "") }
                    val latLong = exif.latLong
                    gps = if (latLong != null) "%.6f, %.6f".format(latLong[0], latLong[1]) else ""
                }
            }
        }
    }

    fun saveCleaned() {
        val u = uri ?: return
        // 重新编码为 JPEG 即丢弃全部元数据
        val bitmap = ImageUtil.loadBitmap(context, u) ?: return
        val bytes = ImageUtil.encode(bitmap, Bitmap.CompressFormat.JPEG, 95)
        val result = ImageUtil.saveToPictures(context, "clean_${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")
        Toast.makeText(
            context,
            if (result.isSuccess) "已保存无元数据副本到 ${result.getOrNull()}" else "保存失败",
            Toast.LENGTH_SHORT
        ).show()
    }

    ToolScaffold {
        item { SectionHeader("图片") }
        item {
            GroupedCard {
                CardPadding {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterHorizontally)
                    ) {
                        SolidButton(onClick = { picker.launch("image/*") }) {
                            Text("选择图片", color = Color.White)
                        }
                        SolidButton(onClick = { saveCleaned() }, filled = false, enabled = uri != null) {
                            Text("导出无元数据副本", color = palette.accent)
                        }
                    }
                }
            }
        }
        item { SectionHeader("位置信息(隐私重点)") }
        item {
            GroupedCard {
                KeyValueRow("GPS 坐标", gps.ifEmpty { if (uri != null) "无(安全)" else "" })
            }
        }
        item { SectionHeader("EXIF 元数据") }
        item {
            GroupedCard {
                if (tags.isEmpty()) {
                    KeyValueRow("选择图片后展示", "", copyable = false)
                } else {
                    tags.forEachIndexed { index, (k, v) ->
                        if (index > 0) RowDivider()
                        KeyValueRow(k, v)
                    }
                }
            }
        }
    }
}
