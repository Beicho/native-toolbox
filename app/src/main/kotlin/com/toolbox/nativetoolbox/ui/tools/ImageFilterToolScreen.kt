package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class FilterPreset(val name: String, val matrix: FloatArray)

private val PRESETS = listOf(
    FilterPreset("原图", floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)),
    FilterPreset("黑白", floatArrayOf(0.299f, 0.587f, 0.114f, 0f, 0f, 0.299f, 0.587f, 0.114f, 0f, 0f, 0.299f, 0.587f, 0.114f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)),
    FilterPreset("怀旧", floatArrayOf(0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f, 0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)),
    FilterPreset("胶片", floatArrayOf(1.1f, 0f, 0f, 0f, -12f, 0f, 1.05f, 0f, 0f, -6f, 0f, 0f, 0.92f, 0f, 8f, 0f, 0f, 0f, 1f, 0f)),
    FilterPreset("冷调", floatArrayOf(0.92f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1.12f, 0f, 6f, 0f, 0f, 0f, 1f, 0f)),
    FilterPreset("暖阳", floatArrayOf(1.12f, 0f, 0f, 0f, 8f, 0f, 1.02f, 0f, 0f, 2f, 0f, 0f, 0.88f, 0f, -6f, 0f, 0f, 0f, 1f, 0f)),
    FilterPreset("清新", floatArrayOf(1.05f, 0f, 0f, 0f, 6f, 0f, 1.08f, 0f, 0f, 6f, 0f, 0f, 1.05f, 0f, 6f, 0f, 0f, 0f, 1f, 0f)),
    FilterPreset("反色", floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f)),
)

/** 组合:预设 → 亮度/对比/饱和/色温 */
private fun buildMatrix(preset: FloatArray, brightness: Float, contrast: Float, saturation: Float, warmth: Float): ColorMatrix {
    val cm = ColorMatrix(preset)
    // 饱和度
    cm.postConcat(ColorMatrix().apply { setSaturation(saturation) })
    // 对比度 + 亮度:scale 后平移
    val t = (1 - contrast) * 127.5f + brightness
    cm.postConcat(ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, t,
        0f, contrast, 0f, 0f, t,
        0f, 0f, contrast, 0f, t,
        0f, 0f, 0f, 1f, 0f,
    )))
    // 色温:红蓝对拉
    cm.postConcat(ColorMatrix(floatArrayOf(
        1f, 0f, 0f, 0f, warmth,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, -warmth,
        0f, 0f, 0f, 1f, 0f,
    )))
    return cm
}

private fun applyMatrix(src: Bitmap, cm: ColorMatrix): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
    return out
}

@Composable
fun ImageFilterToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var previewBase by remember { mutableStateOf<Bitmap?>(null) } // 缩小版,滑杆实时预览用
    var preset by rememberSaveable { mutableStateOf(0) }
    var brightness by rememberSaveable { mutableFloatStateOf(0f) }   // -80..80
    var contrast by rememberSaveable { mutableFloatStateOf(1f) }     // 0.5..1.6
    var saturation by rememberSaveable { mutableFloatStateOf(1f) }   // 0..2
    var warmth by rememberSaveable { mutableFloatStateOf(0f) }       // -40..40
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            source = ImageUtil.loadBitmap(context, uri, 2400)
            previewBase = source?.let {
                val w = 900.coerceAtMost(it.width)
                Bitmap.createScaledBitmap(it, w, (w.toLong() * it.height / it.width).toInt(), true)
            }
            status = if (source == null) "图读不出来" else ""
        }
    }

    val previewBitmap = remember(previewBase, preset, brightness, contrast, saturation, warmth) {
        previewBase?.let { applyMatrix(it, buildMatrix(PRESETS[preset].matrix, brightness, contrast, saturation, warmth)) }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    if (previewBitmap == null) {
                        Text("选一张图开始调色", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Image(previewBitmap.asImageBitmap(), contentDescription = "滤镜预览", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                        Spacer(Modifier.height(12.dp))
                    }
                    SolidButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth(), filled = source == null) {
                        Text(if (source == null) "选图" else "换一张")
                    }
                }
            }
        }
        item {
            if (previewBase != null) {
                GroupedCard {
                    CardPadding {
                        Text("滤镜", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PRESETS.forEachIndexed { i, f ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val thumb = remember(previewBase) {
                                        previewBase?.let { b ->
                                            val s = Bitmap.createScaledBitmap(b, 96, (96L * b.height / b.width).toInt().coerceAtLeast(1), true)
                                            applyMatrix(s, ColorMatrix(f.matrix))
                                        }
                                    }
                                    if (thumb != null) {
                                        Image(
                                            thumb.asImageBitmap(), contentDescription = f.name,
                                            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(8.dp))
                                                .clickable { preset = i },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(f.name, style = MaterialTheme.typography.bodySmall, color = if (preset == i) palette.accent else palette.secondaryLabel)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            if (previewBase != null) {
                GroupedCard {
                    CardPadding {
                        FilterSlider("亮度", brightness, -80f..80f, { brightness = it }, palette)
                        FilterSlider("对比", contrast, 0.5f..1.6f, { contrast = it }, palette)
                        FilterSlider("饱和", saturation, 0f..2f, { saturation = it }, palette)
                        FilterSlider("色温", warmth, -40f..40f, { warmth = it }, palette)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SolidButton(onClick = { brightness = 0f; contrast = 1f; saturation = 1f; warmth = 0f; preset = 0 }, Modifier.weight(1f), filled = false) { Text("重置") }
                            SolidButton(
                                onClick = {
                                    val s = source ?: return@SolidButton
                                    busy = true
                                    scope.launch {
                                        val bytes = withContext(Dispatchers.Default) {
                                            val full = applyMatrix(s, buildMatrix(PRESETS[preset].matrix, brightness, contrast, saturation, warmth))
                                            val b = ImageUtil.encode(full, Bitmap.CompressFormat.JPEG, 93)
                                            full.recycle(); b
                                        }
                                        val r = withContext(Dispatchers.IO) {
                                            ImageUtil.saveToPictures(context, "filter_${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")
                                        }
                                        status = r.fold({ "已存到相册" }, { "保存失败:${it.message}" })
                                        busy = false
                                    }
                                },
                                Modifier.weight(2f), enabled = !busy
                            ) { Text(if (busy) "导出中…" else "保存") }
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

@Composable
private fun FilterSlider(name: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, palette: com.toolbox.nativetoolbox.ui.theme.IosPalette) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.size(width = 40.dp, height = 22.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
    }
}
