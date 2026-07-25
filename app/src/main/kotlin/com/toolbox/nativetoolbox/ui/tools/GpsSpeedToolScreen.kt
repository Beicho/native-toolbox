package com.toolbox.nativetoolbox.ui.tools

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.PermissionGate

@SuppressLint("MissingPermission")
@Composable
private fun SpeedContent() {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val lm = remember { context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager }

    var speedKmh by remember { mutableFloatStateOf(0f) }
    var maxSpeed by remember { mutableFloatStateOf(0f) }
    var altitude by remember { mutableDoubleStateOf(0.0) }
    var accuracy by remember { mutableFloatStateOf(0f) }
    var satellites by remember { mutableIntStateOf(0) }
    var satellitesUsed by remember { mutableIntStateOf(0) }
    var distanceM by remember { mutableDoubleStateOf(0.0) }
    var lastLoc by remember { mutableStateOf<Location?>(null) }
    var hasFix by remember { mutableStateOf(false) }
    var gpsOn by remember { mutableStateOf(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) }

    val listener = remember {
        LocationListener { loc ->
            hasFix = true
            val kmh = loc.speed * 3.6f
            speedKmh = if (kmh < 0.8f) 0f else kmh  // 静止抖动裁掉
            if (speedKmh > maxSpeed) maxSpeed = speedKmh
            altitude = loc.altitude
            accuracy = loc.accuracy
            lastLoc?.let { prev ->
                val d = prev.distanceTo(loc)
                if (d > accuracy / 2 && d < 200) distanceM += d  // 过滤漂移
            }
            lastLoc = loc
        }
    }

    val gnssCallback = remember {
        if (Build.VERSION.SDK_INT >= 30) {
            object : android.location.GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: android.location.GnssStatus) {
                    satellites = status.satelliteCount
                    var used = 0
                    for (i in 0 until status.satelliteCount) if (status.usedInFix(i)) used++
                    satellitesUsed = used
                }
            }
        } else null
    }

    DisposableEffect(Unit) {
        gpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (gpsOn) {
            runCatching {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, listener)
                if (Build.VERSION.SDK_INT >= 30 && gnssCallback != null) {
                    lm.registerGnssStatusCallback(context.mainExecutor, gnssCallback as android.location.GnssStatus.Callback)
                }
            }
        }
        onDispose {
            lm.removeUpdates(listener)
            if (Build.VERSION.SDK_INT >= 30 && gnssCallback != null) {
                lm.unregisterGnssStatusCallback(gnssCallback as android.location.GnssStatus.Callback)
            }
        }
    }

    GroupedCard {
        CardPadding {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%.0f".format(speedKmh),
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.label
                )
                Text("km/h", style = MaterialTheme.typography.titleMedium, color = palette.secondaryLabel)
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        !gpsOn -> "系统定位没开,去下拉快捷开关打开"
                        !hasFix -> "正在搜星…到空旷处更快"
                        else -> "精度 ±${"%.0f".format(accuracy)} 米"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasFix) palette.green else palette.orange
                )
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    GroupedCard {
        CardPadding {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("最高", "%.0f km/h".format(maxSpeed), Modifier.weight(1f))
                StatCell("里程", if (distanceM >= 1000) "%.2f km".format(distanceM / 1000) else "%.0f m".format(distanceM), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("海拔", "%.0f m".format(altitude), Modifier.weight(1f))
                StatCell("卫星", "$satellitesUsed/$satellites", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            SolidButton(onClick = { maxSpeed = 0f; distanceM = 0.0; lastLoc = null }, Modifier.fillMaxWidth(), filled = false) {
                Text("清零重计")
            }
        }
    }
}

@Composable
fun GpsSpeedToolScreen(onBack: () -> Unit) {
    ToolScaffold {
        item {
            PermissionGate(android.Manifest.permission.ACCESS_FINE_LOCATION, "测速需要 GPS 定位。位置只在本机算速度,不上传") {
                SpeedContent()
            }
        }
    }
}
