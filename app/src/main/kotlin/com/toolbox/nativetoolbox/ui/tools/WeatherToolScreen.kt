package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.net.AstroApi
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 常用城市经纬度，省得用户自己查 */
private val presetCities = listOf(
    Triple("北京", 39.9042, 116.4074),
    Triple("上海", 31.2304, 121.4737),
    Triple("广州", 23.1291, 113.2644),
    Triple("深圳", 22.5431, 114.0579),
    Triple("成都", 30.5728, 104.0668),
    Triple("杭州", 30.2741, 120.1551),
    Triple("武汉", 30.5928, 114.3055),
    Triple("西安", 34.3416, 108.9398)
)

/** WMO 天气代码 → 人话 */
private fun weatherText(code: Int): String = when (code) {
    0 -> "晴"
    1 -> "晴间多云"
    2 -> "多云"
    3 -> "阴"
    45, 48 -> "有雾"
    51, 53, 55 -> "小雨"
    56, 57 -> "冻雨"
    61 -> "小雨"
    63 -> "中雨"
    65 -> "大雨"
    66, 67 -> "冻雨"
    71 -> "小雪"
    73 -> "中雪"
    75 -> "大雪"
    77 -> "雪粒"
    80, 81, 82 -> "阵雨"
    85, 86 -> "阵雪"
    95 -> "雷阵雨"
    96, 99 -> "雷阵雨伴冰雹"
    else -> "未知"
}

/** 空气质量指数分级（欧洲 AQI 口径，后端用的 open-meteo） */
private fun aqiLevel(aqi: Int): String = when {
    aqi < 0 -> "—"
    aqi <= 20 -> "优"
    aqi <= 40 -> "良"
    aqi <= 60 -> "中等"
    aqi <= 80 -> "较差"
    aqi <= 100 -> "差"
    else -> "极差"
}

private fun temp(value: Double): String =
    if (value.isNaN()) "—" else Math.round(value).toString() + "°"

@Composable
fun WeatherToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var lat by rememberSaveable { mutableStateOf("39.9042") }
    var lon by rememberSaveable { mutableStateOf("116.4074") }
    var cityName by rememberSaveable { mutableStateOf("北京") }
    var raw by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }

    fun load() {
        val latValue = lat.trim().toDoubleOrNull()
        val lonValue = lon.trim().toDoubleOrNull()
        if (latValue == null || lonValue == null || latValue < -90 || latValue > 90 || lonValue < -180 || lonValue > 180) {
            status = "经纬度填得不对，纬度 -90~90，经度 -180~180"
            return
        }
        loading = true
        status = ""
        scope.launch {
            AstroApi.get("/weather", mapOf("lat" to latValue.toString(), "lon" to lonValue.toString()))
                .onSuccess { res ->
                    raw = res.data.toString()
                    status = cachedHint(res.cachedAt)
                }
                .onFailure { e ->
                    raw = null
                    status = e.message ?: "获取失败，请检查网络"
                }
            loading = false
        }
    }

    val json = raw?.let { runCatching { JSONObject(it) }.getOrNull() }
    val current = json?.optJSONObject("current") ?: json?.optJSONObject("current_weather")
    val air = json?.optJSONObject("air") ?: json?.optJSONObject("air_quality")
    val daily = json?.optJSONObject("daily")

    ToolScaffold {
        item { SectionHeader("位置") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = lat,
                            onValueChange = { lat = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "纬度",
                            mono = true
                        )
                        IosTextField(
                            value = lon,
                            onValueChange = { lon = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "经度",
                            mono = true
                        )
                    }
                    presetCities.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (name, cityLat, cityLon) ->
                                SolidButton(
                                    onClick = {
                                        lat = cityLat.toString()
                                        lon = cityLon.toString()
                                        cityName = name
                                    },
                                    modifier = Modifier.weight(1f),
                                    filled = cityName == name,
                                    height = 38.dp
                                ) { Text(name, style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                    SolidButton(onClick = { load() }, enabled = !loading) {
                        Text(if (loading) "获取中…" else "查天气")
                    }
                    Text(
                        "这个功能需要联网。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        if (status.isNotBlank()) {
            item {
                GroupedCard {
                    CardPadding {
                        Text(status, style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    }
                }
            }
        }
        if (current != null) {
            item { SectionHeader("实况") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell(
                                "温度",
                                temp(current.optDouble("temperature_2m", current.optDouble("temperature", Double.NaN))),
                                Modifier.weight(1f)
                            )
                            StatCell(
                                "天气",
                                weatherText(current.optInt("weather_code", current.optInt("weathercode", -1))),
                                Modifier.weight(1f)
                            )
                            StatCell(
                                "湿度",
                                current.optInt("relative_humidity_2m", -1).let { if (it < 0) "—" else it.toString() + "%" },
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            item { SectionHeader("详细") }
            item {
                GroupedCard {
                    KeyValueRow(
                        "体感温度",
                        current.optDouble("apparent_temperature", Double.NaN).let { if (it.isNaN()) "" else temp(it) },
                        copyable = false
                    )
                    RowDivider()
                    KeyValueRow(
                        "风速",
                        current.optDouble("wind_speed_10m", current.optDouble("windspeed", Double.NaN))
                            .let { if (it.isNaN()) "" else Math.round(it).toString() + " km/h" },
                        copyable = false
                    )
                    RowDivider()
                    KeyValueRow(
                        "降水",
                        current.optDouble("precipitation", Double.NaN)
                            .let { if (it.isNaN()) "" else it.toString() + " mm" },
                        copyable = false
                    )
                    if (daily != null) {
                        RowDivider()
                        val maxArr = daily.optJSONArray("temperature_2m_max")
                        val minArr = daily.optJSONArray("temperature_2m_min")
                        val high = maxArr?.optDouble(0, Double.NaN) ?: Double.NaN
                        val low = minArr?.optDouble(0, Double.NaN) ?: Double.NaN
                        KeyValueRow(
                            "今日温差",
                            if (high.isNaN() || low.isNaN()) "" else temp(low) + " ~ " + temp(high),
                            copyable = false
                        )
                    }
                }
            }
        }
        if (air != null) {
            item { SectionHeader("空气质量") }
            item {
                GroupedCard {
                    val aqi = air.optInt("european_aqi", air.optInt("aqi", -1))
                    KeyValueRow("指数", if (aqi < 0) "" else aqi.toString() + "（" + aqiLevel(aqi) + "）", copyable = false)
                    RowDivider()
                    KeyValueRow(
                        "PM2.5",
                        air.optDouble("pm2_5", Double.NaN).let { if (it.isNaN()) "" else Math.round(it).toString() + " μg/m³" },
                        copyable = false
                    )
                    RowDivider()
                    KeyValueRow(
                        "PM10",
                        air.optDouble("pm10", Double.NaN).let { if (it.isNaN()) "" else Math.round(it).toString() + " μg/m³" },
                        copyable = false
                    )
                }
            }
        }

        // 未来7天预报
        if (daily != null) {
            item { SectionHeader("未来7天预报") }
            item {
                val timeArr = daily.optJSONArray("time")
                val maxArr = daily.optJSONArray("temperature_2m_max")
                val minArr = daily.optJSONArray("temperature_2m_min")
                val codeArr = daily.optJSONArray("weather_code")
                val sunriseArr = daily.optJSONArray("sunrise")
                val sunsetArr = daily.optJSONArray("sunset")
                val uvArr = daily.optJSONArray("uv_index_max")

                if (timeArr != null && maxArr != null && minArr != null) {
                    GroupedCard {
                        val days = minOf(7, timeArr.length())
                        for (i in 0 until days) {
                            val date = timeArr.optString(i, "")
                            val dayLabel = when (i) {
                                0 -> "今天"
                                1 -> "明天"
                                else -> date.substring(5).replace("-", "/")
                            }
                            val high = maxArr.optDouble(i, Double.NaN)
                            val low = minArr.optDouble(i, Double.NaN)
                            val code = codeArr?.optInt(i, -1) ?: -1
                            val weather = if (code >= 0) weatherText(code) else "—"

                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(dayLabel, style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.width(56.dp))
                                Text(weather, style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel, modifier = Modifier.width(72.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(temp(low), style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                                    Text("~", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                                    Text(temp(high), style = MaterialTheme.typography.bodyMedium, color = palette.label)
                                }
                            }
                            if (i != days - 1) RowDivider()
                        }
                    }
                }

                // 日出日落 + 紫外线
                if (sunriseArr != null || uvArr != null) {
                    Spacer(Modifier.height(12.dp))
                    GroupedCard {
                        if (sunriseArr != null && sunriseArr.length() > 0) {
                            val sunrise = sunriseArr.optString(0, "").let {
                                if (it.length >= 16) it.substring(11, 16) else "—"
                            }
                            val sunset = sunsetArr?.optString(0, "")?.let {
                                if (it.length >= 16) it.substring(11, 16) else "—"
                            } ?: "—"
                            KeyValueRow("日出", sunrise, copyable = false)
                            RowDivider()
                            KeyValueRow("日落", sunset, copyable = false)
                        }
                        if (uvArr != null && uvArr.length() > 0) {
                            val uv = uvArr.optDouble(0, Double.NaN)
                            if (!uv.isNaN()) {
                                if (sunriseArr != null && sunriseArr.length() > 0) RowDivider()
                                val uvLevel = when {
                                    uv < 3 -> "低"
                                    uv < 6 -> "中等"
                                    uv < 8 -> "高"
                                    uv < 11 -> "很高"
                                    else -> "极高"
                                }
                                KeyValueRow("紫外线指数", "%.1f (%s)".format(uv, uvLevel), copyable = false)
                            }
                        }
                    }
                }
            }
        }
    }
}
