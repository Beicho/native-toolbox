package com.toolbox.nativetoolbox.ui.tools

import android.annotation.SuppressLint
import android.location.LocationManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * 日出日落:NOAA 简化算法(民用精度 ±2 分钟);月相:朔望月周期推算。全离线。
 */
private object AstroCalc {
    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)

    /** 返回 (日出, 日落) 当地时间的"当日分钟数",极昼/极夜返回 null */
    fun sunTimes(lat: Double, lon: Double, cal: Calendar): Pair<Int, Int>? {
        val doy = cal.get(Calendar.DAY_OF_YEAR)
        val tzOffsetH = cal.timeZone.getOffset(cal.timeInMillis) / 3600000.0

        fun calc(rising: Boolean): Double? {
            val lngHour = lon / 15.0
            val t = doy + ((if (rising) 6.0 else 18.0) - lngHour) / 24.0
            val m = 0.9856 * t - 3.289
            var l = m + 1.916 * sin(rad(m)) + 0.020 * sin(rad(2 * m)) + 282.634
            l = (l + 360.0) % 360.0
            var ra = deg(kotlin.math.atan(0.91764 * tan(rad(l))))
            ra = (ra + 360.0) % 360.0
            ra += (floor(l / 90.0) * 90.0) - (floor(ra / 90.0) * 90.0)
            ra /= 15.0
            val sinDec = 0.39782 * sin(rad(l))
            val cosDec = cos(asin(sinDec))
            val cosH = (cos(rad(90.833)) - sinDec * sin(rad(lat))) / (cosDec * cos(rad(lat)))
            if (cosH > 1 || cosH < -1) return null
            var h = if (rising) 360.0 - deg(acos(cosH)) else deg(acos(cosH))
            h /= 15.0
            val tt = h + ra - 0.06571 * t - 6.622
            var ut = tt - lngHour
            ut = (ut + 24.0) % 24.0
            return (ut + tzOffsetH + 24.0) % 24.0
        }

        val rise = calc(true) ?: return null
        val set = calc(false) ?: return null
        return (rise * 60).toInt() to (set * 60).toInt()
    }

    /** 月龄(0~29.53),0=新月 */
    fun moonAge(cal: Calendar): Double {
        // 参考朔:2000-01-06 18:14 UTC
        val ref = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(2000, 0, 6, 18, 14, 0)
        }.timeInMillis
        val synodic = 29.530588853
        val days = (cal.timeInMillis - ref) / 86400000.0
        return ((days % synodic) + synodic) % synodic
    }

    fun moonPhaseName(age: Double): Pair<String, String> = when {
        age < 1.0 -> "新月" to "🌑"
        age < 6.4 -> "娥眉月" to "🌒"
        age < 8.4 -> "上弦月" to "🌓"
        age < 13.8 -> "盈凸月" to "🌔"
        age < 15.8 -> "满月" to "🌕"
        age < 21.1 -> "亏凸月" to "🌖"
        age < 23.1 -> "下弦月" to "🌗"
        age < 28.5 -> "残月" to "🌘"
        else -> "新月" to "🌑"
    }

    /** 月亮照亮比例 */
    fun illumination(age: Double): Int {
        val phase = age / 29.530588853 * 2 * Math.PI
        return ((1 - cos(phase)) / 2 * 100).toInt()
    }
}

private fun fmtMin(minOfDay: Int): String = "%02d:%02d".format(minOfDay / 60, minOfDay % 60)

@SuppressLint("MissingPermission")
@Composable
fun AstronomyToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var latText by rememberSaveable { mutableStateOf("39.90") }
    var lonText by rememberSaveable { mutableStateOf("116.40") }
    var located by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    // 有定位权限就静默取一次,没有就用默认(北京),不弹权限
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            runCatching {
                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    latText = "%.2f".format(loc.latitude)
                    lonText = "%.2f".format(loc.longitude)
                    located = true
                }
            }
        }
    }

    val lat = latText.toDoubleOrNull()
    val lon = lonText.toDoubleOrNull()
    val cal = remember { Calendar.getInstance() }
    val sun = if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0)
        AstroCalc.sunTimes(lat, lon, cal) else null
    val age = AstroCalc.moonAge(cal)
    val (phaseName, phaseEmoji) = AstroCalc.moonPhaseName(age)

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(phaseEmoji, fontSize = 64.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("今晚 · $phaseName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.label)
                        Text("月龄 ${"%.1f".format(age)} 天 · 亮面 ${AstroCalc.illumination(age)}%", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    if (sun != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("日出", fmtMin(sun.first), Modifier.weight(1f))
                            StatCell("日落", fmtMin(sun.second), Modifier.weight(1f))
                            StatCell("昼长", run {
                                val d = sun.second - sun.first
                                "${d / 60}h${d % 60}m"
                            }, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        val goldenEve = sun.second - 60
                        Text(
                            "拍照黄金时刻:日出后一小时内(${fmtMin(sun.first)}~${fmtMin(sun.first + 60)})和日落前一小时(${fmtMin(goldenEve)}~${fmtMin(sun.second)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    } else {
                        Text("这个位置今天太阳不升/不落(极昼极夜),或坐标没填对", style = MaterialTheme.typography.bodyMedium, color = palette.orange)
                    }
                }
            }
        }
        item { SectionHeader(if (located) "位置(已自动定位)" else "位置(默认北京,可改)") }
        item {
            GroupedCard {
                CardPadding {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("纬度", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                            Spacer(Modifier.height(4.dp))
                            IosTextField(latText, { latText = it.filter { c -> c.isDigit() || c == '.' || c == '-' } }, Modifier.fillMaxWidth(), placeholder = "39.90")
                        }
                        Column(Modifier.weight(1f)) {
                            Text("经度", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                            Spacer(Modifier.height(4.dp))
                            IosTextField(lonText, { lonText = it.filter { c -> c.isDigit() || c == '.' || c == '-' } }, Modifier.fillMaxWidth(), placeholder = "116.40")
                        }
                    }
                }
            }
        }
        item { SectionHeader("接下来的月相") }
        item {
            GroupedCard {
                val synodic = 29.530588853
                val next = listOf(
                    "满月" to 14.77, "新月" to 0.0, "上弦月" to 7.38, "下弦月" to 22.15,
                ).map { (name, target) ->
                    var d = target - age
                    if (d <= 0) d += synodic
                    name to d
                }.sortedBy { it.second }
                next.forEachIndexed { i, (name, days) ->
                    val c = Calendar.getInstance().apply { add(Calendar.MINUTE, (days * 1440).toInt()) }
                    KeyValueRow(name, "${c.get(Calendar.MONTH) + 1}月${c.get(Calendar.DAY_OF_MONTH)}日(${days.toInt()} 天后)", copyable = false)
                    if (i != next.lastIndex) RowDivider()
                }
            }
        }
    }
}
