package com.toolbox.nativetoolbox.ui.tools

import android.annotation.SuppressLint
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.PermissionGate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
private fun ParkingContent() {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("parking", android.content.Context.MODE_PRIVATE) }

    var lat by remember { mutableStateOf(prefs.getString("lat", null)) }
    var lon by remember { mutableStateOf(prefs.getString("lon", null)) }
    var savedAt by remember { mutableStateOf(prefs.getLong("at", 0L)) }
    var note by rememberSaveable { mutableStateOf(prefs.getString("note", "") ?: "") }
    var status by remember { mutableStateOf("") }

    val df = remember { SimpleDateFormat("M月d日 HH:mm", Locale.CHINESE) }

    fun mark() {
        status = ""
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        val loc = runCatching {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }.getOrNull()
        if (loc == null) { status = "拿不到位置。到开阔处等几秒,或先打开地图 App 定位一次再回来"; return }
        lat = "%.6f".format(loc.latitude)
        lon = "%.6f".format(loc.longitude)
        savedAt = System.currentTimeMillis()
        prefs.edit().putString("lat", lat).putString("lon", lon).putLong("at", savedAt).putString("note", note).apply()
        status = "停车位置已记下(精度 ±${loc.accuracy.toInt()} 米)"
    }

    fun navigate() {
        val la = lat ?: return; val lo = lon ?: return
        // geo: 通用协议,装了高德/百度/Google Maps 都能接
        val intents = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:$la,$lo?q=$la,$lo(我的车)")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://uri.amap.com/marker?position=$lo,$la&name=我的车")),
        )
        for (i in intents) {
            if (runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)) return
        }
        status = "没装地图 App,坐标已显示,可以复制"
    }

    GroupedCard {
        CardPadding {
            if (lat == null) {
                Text("停好车点一下,回头一键导航找车", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
            } else {
                Text("车停在这儿", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.label)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCell("记录时间", df.format(Date(savedAt)), Modifier.weight(1f))
                    StatCell("已停", run {
                        val m = (System.currentTimeMillis() - savedAt) / 60000
                        if (m >= 60) "${m / 60}小时${m % 60}分" else "$m 分钟"
                    }, Modifier.weight(1f))
                }
                if (note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("备注:$note", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                }
            }
            Spacer(Modifier.height(12.dp))
            SolidButton(onClick = { mark() }, Modifier.fillMaxWidth()) {
                Text(if (lat == null) "记下停车位置" else "重新记录(换了车位)")
            }
            if (lat != null) {
                Spacer(Modifier.height(8.dp))
                SolidButton(onClick = { navigate() }, Modifier.fillMaxWidth(), filled = false) { Text("导航去找车") }
            }
            if (status.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("停车位置")) palette.green else palette.orange)
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    GroupedCard {
        CardPadding {
            Text("楼层/车位号备注", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
            Spacer(Modifier.height(4.dp))
            IosTextField(note, {
                note = it
                prefs.edit().putString("note", it).apply()
            }, Modifier.fillMaxWidth(), placeholder = "比如 B2 层 F 区 233 号")
        }
    }
    if (lat != null && lon != null) {
        Spacer(Modifier.height(20.dp))
        GroupedCard {
            KeyValueRow("坐标", "$lat, $lon")
            RowDivider()
            KeyValueRow("适用场景", "露天/地面停车场。地下车库没信号,主要靠车位号备注", copyable = false)
        }
    }
}

@Composable
fun ParkingToolScreen(onBack: () -> Unit) {
    ToolScaffold {
        item {
            PermissionGate(android.Manifest.permission.ACCESS_FINE_LOCATION, "记停车位置需要定位。坐标只存在手机里") {
                ParkingContent()
            }
        }
    }
}
