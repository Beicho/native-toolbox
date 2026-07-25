package com.toolbox.nativetoolbox.ui.tools

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private class PerfSnapshot(
    val totalRamMb: Long,
    val availRamMb: Long,
    val appHeapUsedMb: Long,
    val appHeapMaxMb: Long,
    val appPssMb: Long,
    val cpuCores: Int,
    val cpuMaxMhz: Int,
    val lowMemory: Boolean,
    val threshold: Long
)

private fun readCpuMaxMhz(): Int = runCatching {
    val file = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
    if (!file.canRead()) return 0
    file.readText().trim().toLong().toInt() / 1000
}.getOrDefault(0)

private fun snapshot(context: Context): PerfSnapshot {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }
    val runtime = Runtime.getRuntime()
    val memoryInfo = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
    return PerfSnapshot(
        totalRamMb = info.totalMem / 1024 / 1024,
        availRamMb = info.availMem / 1024 / 1024,
        appHeapUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
        appHeapMaxMb = runtime.maxMemory() / 1024 / 1024,
        appPssMb = memoryInfo.totalPss.toLong() / 1024,
        cpuCores = runtime.availableProcessors(),
        cpuMaxMhz = readCpuMaxMhz(),
        lowMemory = info.lowMemory,
        threshold = info.threshold / 1024 / 1024
    )
}

@Composable
fun PerformanceToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var data by remember { mutableStateOf(snapshot(context)) }
    var history by remember { mutableStateOf(listOf<Long>()) }

    DisposableEffect(Unit) {
        val job = scope.launch {
            while (isActive) {
                val s = snapshot(context)
                data = s
                history = (history + s.availRamMb).takeLast(40)
                delay(1500)
            }
        }
        onDispose { job.cancel() }
    }

    val usedRam = data.totalRamMb - data.availRamMb
    val ramFraction = if (data.totalRamMb == 0L) 0f else usedRam.toFloat() / data.totalRamMb
    val heapFraction = if (data.appHeapMaxMb == 0L) 0f else data.appHeapUsedMb.toFloat() / data.appHeapMaxMb

    ToolScaffold {
        item { SectionHeader("内存") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("已用", usedRam.toString() + " MB", Modifier.weight(1f))
                        StatCell("可用", data.availRamMb.toString() + " MB", Modifier.weight(1f))
                        StatCell("占用率", String.format("%.0f%%", ramFraction * 100), Modifier.weight(1f))
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.sunkenBackground)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(ramFraction.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        ramFraction > 0.9f -> palette.red
                                        ramFraction > 0.75f -> palette.orange
                                        else -> palette.green
                                    }
                                )
                        )
                    }
                    if (data.lowMemory) {
                        Text(
                            "系统已进入低内存状态，后台应用会被大量清理。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    }
                }
            }
        }
        item { SectionHeader("系统内存详情") }
        item {
            GroupedCard {
                KeyValueRow("物理内存总量", data.totalRamMb.toString() + " MB", copyable = false)
                RowDivider()
                KeyValueRow("低内存阈值", data.threshold.toString() + " MB", copyable = false)
                RowDivider()
                KeyValueRow(
                    "距离阈值",
                    (data.availRamMb - data.threshold).toString() + " MB",
                    copyable = false
                )
            }
        }
        item { SectionHeader("本应用占用") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("堆已用", data.appHeapUsedMb.toString() + " MB", Modifier.weight(1f))
                        StatCell("堆上限", data.appHeapMaxMb.toString() + " MB", Modifier.weight(1f))
                        StatCell("实际占用", data.appPssMb.toString() + " MB", Modifier.weight(1f))
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.sunkenBackground)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(heapFraction.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.accent)
                        )
                    }
                    Text(
                        "实际占用（PSS）比堆内存大是正常的，还包含图形缓冲、原生库和共享内存。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("处理器") }
        item {
            GroupedCard {
                KeyValueRow("核心数", data.cpuCores.toString(), copyable = false)
                RowDivider()
                KeyValueRow(
                    "最高主频",
                    if (data.cpuMaxMhz > 0) String.format("%.2f GHz", data.cpuMaxMhz / 1000.0) else "系统未开放读取",
                    copyable = false
                )
                RowDivider()
                KeyValueRow("架构", Build.SUPPORTED_ABIS.firstOrNull() ?: "未知", copyable = false)
                RowDivider()
                KeyValueRow("系统版本", "Android " + Build.VERSION.RELEASE + "（API " + Build.VERSION.SDK_INT + "）", copyable = false)
            }
        }
        if (history.size > 3) {
            item { SectionHeader("可用内存趋势") }
            item {
                GroupedCard {
                    CardPadding {
                        val maxValue = history.max().coerceAtLeast(1)
                        Row(
                            Modifier.fillMaxWidth().height(60.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            history.forEach { value ->
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    Box(
                                        Modifier
                                            .align(androidx.compose.ui.Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .height((60 * value / maxValue).dp.coerceAtLeast(2.dp))
                                            .background(palette.accent.copy(alpha = 0.6f))
                                    )
                                }
                            }
                        }
                        Text(
                            "每 1.5 秒采一次，越高表示可用内存越多。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "全部读自系统公开接口，不需要权限。现代 Android 不允许应用读取全局 CPU 占用率，" +
                            "所以这里只显示内存和硬件规格。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
