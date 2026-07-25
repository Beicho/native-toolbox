package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ratios = listOf(
    "原始" to 0f,
    "1:1" to 1f,
    "4:3" to 4f / 3,
    "3:4" to 3f / 4,
    "16:9" to 16f / 9,
    "9:16" to 9f / 16
)

/** 按目标比例居中裁剪 */
private fun cropToRatio(source: Bitmap, ratio: Float): Bitmap {
    if (ratio <= 0f) return source
    val currentRatio = source.width.toFloat() / source.height
    return if (currentRatio > ratio) {
        val newWidth = (source.height * ratio).toInt().coerceAtLeast(1)
        Bitmap.createBitmap(source, (source.width - newWidth) / 2, 0, newWidth, source.height)
    } else {
        val newHeight = (source.width / ratio).toInt().coerceAtLeast(1)
        Bitmap.createBitmap(source, 0, (source.height - newHeight) / 2, source.width, newHeight)
    }
}

private fun transform(source: Bitmap, rotation: Int, flipH: Boolean, flipV: Boolean): Bitmap {
    if (rotation == 0 && !flipH && !flipV) return source
    val matrix = Matrix().apply {
        if (rotation != 0) postRotate(rotation.toFloat())
        if (flipH || flipV) postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
    }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

@Composable
fun ImageCropToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var source by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<Bitmap?>(null) }
    var ratioIndex by rememberSaveable { mutableStateOf(0) }
    var rotation by rememberSaveable { mutableStateOf(0) }
    var flipH by rememberSaveable { mutableStateOf(false) }
    var flipV by rememberSaveable { mutableStateOf(false) }
    var working by rememberSaveable { mutableStateOf(false) }
    var saved by rememberSaveable { mutableStateOf("") }

    fun rebuild() {
        val src = source ?: return
        working = true
        saved = ""
        scope.launch {
            val output = withContext(Dispatchers.Default) {
                val rotated = transform(src, rotation, flipH, flipV)
                val cropped = cropToRatio(rotated, ratios[ratioIndex].second)
                cropped
            }
            result = output
            working = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            source = ImageUtil.loadBitmap(context, uri)
            rotation = 0
            flipH = false
            flipV = false
            ratioIndex = 0
            saved = ""
            rebuild()
        }
    }

    fun save() {
        val bmp = result ?: return
        val bytes = ImageUtil.encode(bmp, Bitmap.CompressFormat.JPEG, 95)
        val name = "crop_" + System.currentTimeMillis() + ".jpg"
        ImageUtil.saveToPictures(context, name, bytes, "image/jpeg")
            .onSuccess {
                saved = "已保存到 " + it
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
            }
            .onFailure { saved = "保存失败：" + (it.message ?: "") }
    }

    ToolScaffold {
        item { SectionHeader("预览") }
        item {
            GroupedCard {
                CardPadding {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.sunkenBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        val shown = result ?: source
                        if (shown != null) {
                            Image(
                                bitmap = shown.asImageBitmap(),
                                contentDescription = "预览",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth().height(300.dp)
                            )
                        } else {
                            Text(
                                "先选一张图",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.tertiaryLabel
                            )
                        }
                    }
                    SolidButton(onClick = { picker.launch("image/*") }, enabled = !working) {
                        Text(if (source == null) "从相册选图" else "换一张图")
                    }
                }
            }
        }
        if (source != null) {
            item { SectionHeader("裁剪比例") }
            item {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(
                            options = ratios.take(3).map { it.first },
                            selectedIndex = ratioIndex.coerceAtMost(2),
                            onSelected = {
                                ratioIndex = it
                                rebuild()
                            }
                        )
                        SegmentedPicker(
                            options = ratios.drop(3).map { it.first },
                            selectedIndex = (ratioIndex - 3).coerceAtLeast(0),
                            onSelected = {
                                ratioIndex = it + 3
                                rebuild()
                            }
                        )
                        Text(
                            "按选定比例从中心裁剪。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
            item { SectionHeader("旋转翻转") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SolidButton(
                                onClick = {
                                    rotation = (rotation + 270) % 360
                                    rebuild()
                                },
                                modifier = Modifier.weight(1f),
                                filled = false
                            ) { Text("左转 90°") }
                            SolidButton(
                                onClick = {
                                    rotation = (rotation + 90) % 360
                                    rebuild()
                                },
                                modifier = Modifier.weight(1f),
                                filled = false
                            ) { Text("右转 90°") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SolidButton(
                                onClick = {
                                    flipH = !flipH
                                    rebuild()
                                },
                                modifier = Modifier.weight(1f),
                                filled = flipH
                            ) { Text("左右翻转") }
                            SolidButton(
                                onClick = {
                                    flipV = !flipV
                                    rebuild()
                                },
                                modifier = Modifier.weight(1f),
                                filled = flipV
                            ) { Text("上下翻转") }
                        }
                    }
                }
            }
            item { SectionHeader("尺寸") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell(
                                "原图",
                                source?.let { it.width.toString() + "×" + it.height } ?: "—",
                                Modifier.weight(1f)
                            )
                            StatCell(
                                "结果",
                                result?.let { it.width.toString() + "×" + it.height } ?: "—",
                                Modifier.weight(1f)
                            )
                        }
                    }
                    KeyValueRow("旋转角度", rotation.toString() + "°", copyable = false)
                    RowDivider()
                    KeyValueRow(
                        "翻转",
                        listOfNotNull(
                            if (flipH) "左右" else null,
                            if (flipV) "上下" else null
                        ).joinToString("、").ifBlank { "无" },
                        copyable = false
                    )
                }
            }
            item { SectionHeader("保存") }
            item {
                GroupedCard {
                    CardPadding {
                        SolidButton(onClick = { save() }, enabled = result != null && !working) {
                            Text(if (working) "处理中…" else "保存到相册")
                        }
                        if (saved.isNotBlank()) {
                            Text(
                                saved,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (saved.startsWith("已保存")) palette.green else palette.red
                            )
                        }
                        Text(
                            "保存为 JPEG（质量 95），存到相册的 AstroKit 文件夹。原图不会被改动。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
    }
}
