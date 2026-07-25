package com.toolbox.nativetoolbox.ui.tools

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.location.Geocoder
import android.location.LocationManager
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
import com.toolbox.nativetoolbox.net.AstroApi
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import com.toolbox.nativetoolbox.util.PermissionGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 左下角打卡水印:大号时间 + 日期星期 + 地点 + 天气,半透明黑条压底 */
private fun renderWatermark(src: Bitmap, time: String, date: String, place: String, weather: String): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)
    val w = out.width.toFloat()
    val base = w / 22f

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = base * 2.2f; color = AColor.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(base / 6f, 0f, base / 12f, AColor.argb(160, 0, 0, 0))
    }
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = base * 0.9f; color = AColor.WHITE
        setShadowLayer(base / 8f, 0f, base / 16f, AColor.argb(160, 0, 0, 0))
    }
    val lines = listOfNotNull(
        date.takeIf { it.isNotBlank() },
        place.takeIf { it.isNotBlank() },
        weather.takeIf { it.isNotBlank() },
    )
    val margin = base
    val lineH = base * 1.35f
    val blockH = base * 2.6f + lines.size * lineH + margin
    var y = out.height - blockH + base * 2.2f

    // 左侧竖色条,打卡相机的经典标识
    canvas.drawRoundRect(
        RectF(margin, y - base * 1.9f, margin + base * 0.28f, y + lines.size * lineH + base * 0.2f),
        base / 8f, base / 8f,
        Paint().apply { color = AColor.rgb(255, 204, 0) }
    )
    val textX = margin + base * 0.9f
    canvas.drawText(time, textX, y, timePaint)
    y += base * 0.6f
    for (line in lines) {
        y += lineH
        canvas.drawText(line, textX, y, subPaint)
    }
    return out
}

@SuppressLint("MissingPermission")
@Composable
private fun WatermarkContent() {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var place by rememberSaveable { mutableStateOf("") }
    var weather by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }

    val timeStr = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    val dateStr = remember {
        SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE).format(Date())
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            source = ImageUtil.loadBitmap(context, uri, 1920)
            preview = null
            status = if (source == null) "图读不出来" else ""
        }
    }

    fun locate() {
        locating = true; status = ""
        scope.launch {
            val loc = withContext(Dispatchers.IO) {
                runCatching {
                    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
                    lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }.getOrNull()
            }
            if (loc == null) {
                status = "拿不到位置,可以手动填地点"
                locating = false
                return@launch
            }
            // 地名:Geocoder 逆解析
            val name = withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    Geocoder(context, Locale.CHINESE).getFromLocation(loc.latitude, loc.longitude, 1)
                        ?.firstOrNull()?.let { a ->
                            listOfNotNull(a.locality ?: a.adminArea, a.subLocality, a.thoroughfare)
                                .joinToString("")
                        }
                }.getOrNull()
            }
            if (!name.isNullOrBlank()) place = name
            // 天气:自家服务
            val w = AstroApi.get("/weather", mapOf("lat" to "%.4f".format(loc.latitude), "lon" to "%.4f".format(loc.longitude)))
            w.onSuccess { res ->
                runCatching {
                    val cur = res.data.getJSONObject("weather").getJSONObject("current")
                    val t = cur.optDouble("temperature_2m", Double.NaN)
                    val code = cur.optInt("weather_code", -1)
                    val desc = when (code) {
                        0 -> "晴"; 1, 2 -> "多云"; 3 -> "阴"; 45, 48 -> "雾"
                        in 51..57 -> "毛毛雨"; in 61..67 -> "雨"; in 71..77 -> "雪"
                        in 80..82 -> "阵雨"; 85, 86 -> "阵雪"; in 95..99 -> "雷雨"
                        else -> ""
                    }
                    if (!t.isNaN()) weather = "${t.toInt()}°C" + (if (desc.isNotEmpty()) " $desc" else "")
                }
            }
            if (name.isNullOrBlank() && weather.isBlank()) status = "定位到了,但地名和天气都没拿到,手动填吧"
            locating = false
        }
    }

    fun render() {
        val s = source ?: return
        scope.launch {
            preview = withContext(Dispatchers.Default) {
                renderWatermark(s, timeStr, dateStr, place.trim(), weather.trim())
            }
        }
    }

    GroupedCard {
        CardPadding {
            val p = preview ?: source
            if (p == null) {
                Text("选一张照片,盖上时间地点章", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                Spacer(Modifier.height(12.dp))
            } else {
                Image(p.asImageBitmap(), contentDescription = "水印预览", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                Spacer(Modifier.height(12.dp))
            }
            SolidButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth(), filled = source == null) {
                Text(if (source == null) "选照片" else "换一张")
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    GroupedCard {
        CardPadding {
            Text("地点", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
            Spacer(Modifier.height(4.dp))
            IosTextField(place, { place = it }, Modifier.fillMaxWidth(), placeholder = "点下面自动定位,或手动填")
            Spacer(Modifier.height(10.dp))
            Text("天气", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
            Spacer(Modifier.height(4.dp))
            IosTextField(weather, { weather = it }, Modifier.fillMaxWidth(), placeholder = "比如 26°C 多云")
            Spacer(Modifier.height(10.dp))
            SolidButton(onClick = { locate() }, Modifier.fillMaxWidth(), filled = false, enabled = !locating) {
                Text(if (locating) "定位中…" else "自动填地点和天气(联网)")
            }
            Spacer(Modifier.height(8.dp))
            SolidButton(onClick = { render() }, Modifier.fillMaxWidth(), filled = false, enabled = source != null) { Text("预览水印") }
            Spacer(Modifier.height(8.dp))
            SolidButton(
                onClick = {
                    val s = source ?: return@SolidButton
                    scope.launch {
                        val final = withContext(Dispatchers.Default) {
                            renderWatermark(s, timeStr, dateStr, place.trim(), weather.trim())
                        }
                        preview = final
                        val bytes = withContext(Dispatchers.Default) { ImageUtil.encode(final, Bitmap.CompressFormat.JPEG, 93) }
                        val r = withContext(Dispatchers.IO) {
                            ImageUtil.saveToPictures(context, "checkin_${System.currentTimeMillis()}.jpg", bytes, "image/jpeg")
                        }
                        status = r.fold({ "已存到相册" }, { "保存失败:${it.message}" })
                    }
                },
                Modifier.fillMaxWidth(),
                enabled = source != null
            ) { Text("盖章保存") }
            if (status.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("已存")) palette.green else palette.orange)
            }
        }
    }
}

@Composable
fun CheckinWatermarkToolScreen(onBack: () -> Unit) {
    ToolScaffold {
        item {
            PermissionGate(android.Manifest.permission.ACCESS_FINE_LOCATION, "自动填写打卡地点需要定位。也可以拒绝后手动填地点") {
                WatermarkContent()
            }
        }
    }
}
