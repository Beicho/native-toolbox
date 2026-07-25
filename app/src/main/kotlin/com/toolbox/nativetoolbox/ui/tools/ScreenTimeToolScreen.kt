package com.toolbox.nativetoolbox.ui.tools

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

private data class AppUsage(val label: String, val minutes: Long)

private fun hasUsageAccess(context: android.content.Context): Boolean {
    val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun queryUsage(context: android.content.Context, days: Int): List<AppUsage> {
    val usm = context.getSystemService(android.content.Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (days > 1) add(Calendar.DAY_OF_YEAR, -(days - 1))
    }
    val start = cal.timeInMillis
    val end = System.currentTimeMillis()
    val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end) ?: return emptyList()
    val pm = context.packageManager
    return stats
        .groupBy { it.packageName }
        .mapValues { (_, list) -> list.sumOf { it.totalTimeInForeground } }
        .filter { it.value >= 60_000 }
        .mapNotNull { (pkg, ms) ->
            val label = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrNull() ?: return@mapNotNull null
            AppUsage(label, ms / 60_000)
        }
        .sortedByDescending { it.minutes }
        .take(30)
}

private fun fmtMin(m: Long): String = if (m >= 60) "${m / 60} 小时 ${m % 60} 分" else "$m 分钟"

@Composable
fun ScreenTimeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var authorized by remember { mutableStateOf(hasUsageAccess(context)) }
    var range by rememberSaveable { mutableStateOf(0) } // 0 今天 1 近7天
    var data by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(range, authorized, refreshKey) {
        if (!authorized) return@LaunchedEffect
        loading = true
        data = withContext(Dispatchers.IO) { queryUsage(context, if (range == 0) 1 else 7) }
        loading = false
    }

    ToolScaffold {
        if (!authorized) {
            item {
                GroupedCard {
                    CardPadding {
                        Text("看看时间都去哪了", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.label)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "统计每个 App 的使用时长需要系统的「使用情况访问」授权。在打开的列表里找到 Astro Kit 打开开关,回来就能看。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryLabel
                        )
                        Spacer(Modifier.height(12.dp))
                        SolidButton(onClick = {
                            runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }, Modifier.fillMaxWidth()) { Text("去授权") }
                        Spacer(Modifier.height(8.dp))
                        SolidButton(onClick = { authorized = hasUsageAccess(context); refreshKey++ }, Modifier.fillMaxWidth(), filled = false) { Text("我开好了,刷新") }
                    }
                }
            }
        } else {
            item {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(listOf("今天", "近 7 天"), range, { range = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        val total = data.sumOf { it.minutes }
                        Text(
                            if (loading) "统计中…" else "共 ${fmtMin(total)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.label
                        )
                    }
                }
            }
            item {
                if (data.isNotEmpty()) {
                    val maxMin = data.first().minutes.coerceAtLeast(1)
                    GroupedCard {
                        Column(Modifier.padding(16.dp)) {
                            data.forEachIndexed { i, u ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        u.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.label,
                                        maxLines = 1,
                                        modifier = Modifier.width(110.dp)
                                    )
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(palette.sunkenBackground)
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth(u.minutes.toFloat() / maxMin)
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(if (i == 0) palette.red else if (i < 3) palette.orange else palette.accent)
                                        )
                                    }
                                    Text(
                                        fmtMin(u.minutes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryLabel
                                    )
                                }
                                if (i != data.lastIndex) Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                } else if (!loading) {
                    GroupedCard {
                        CardPadding {
                            Text("这个时间段没有超过 1 分钟的使用记录", style = MaterialTheme.typography.bodyMedium, color = palette.tertiaryLabel)
                        }
                    }
                }
            }
        }
    }
}
