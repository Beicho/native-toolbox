package com.toolbox.nativetoolbox.ui.tools

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
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
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun humanSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format("%.2f GB", bytes.toDouble() / (1L shl 30))
    bytes >= 1L shl 20 -> String.format("%.1f MB", bytes.toDouble() / (1L shl 20))
    bytes >= 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
    else -> bytes.toString() + " B"
}

private class DirUsage(val label: String, val path: String, val bytes: Long, val files: Int)

private fun dirUsage(label: String, dir: File?): DirUsage {
    if (dir == null || !dir.exists()) return DirUsage(label, dir?.absolutePath ?: "—", 0, 0)
    var total = 0L
    var count = 0
    val stack = ArrayDeque<File>().apply { add(dir) }
    var guard = 0
    while (stack.isNotEmpty() && guard < 200_000) {
        guard++
        val current = stack.removeFirst()
        if (current.isDirectory) {
            current.listFiles()?.forEach { stack.add(it) }
        } else {
            total += current.length()
            count++
        }
    }
    return DirUsage(label, dir.absolutePath, total, count)
}

@Composable
fun StorageCleanToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val stat = remember { StatFs(Environment.getDataDirectory().path) }
    val totalBytes = remember { stat.blockCountLong * stat.blockSizeLong }
    val freeBytes = remember { stat.availableBlocksLong * stat.blockSizeLong }
    val usedBytes = totalBytes - freeBytes

    var usages by remember { mutableStateOf<List<DirUsage>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var cleaned by remember { mutableStateOf("") }

    fun scan() {
        scanning = true
        cleaned = ""
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                listOf(
                    dirUsage("应用缓存", context.cacheDir),
                    dirUsage("外部缓存", context.externalCacheDir),
                    dirUsage("应用文件", context.filesDir),
                    dirUsage("外部文件", context.getExternalFilesDir(null)),
                    dirUsage("代码缓存", context.codeCacheDir)
                )
            }
            usages = result
            scanning = false
        }
    }

    fun clearCaches() {
        scope.launch {
            val freed = withContext(Dispatchers.IO) {
                var sum = 0L
                listOf(context.cacheDir, context.externalCacheDir, context.codeCacheDir).forEach { dir ->
                    if (dir != null && dir.exists()) {
                        val before = dirUsage("", dir).bytes
                        dir.listFiles()?.forEach { it.deleteRecursively() }
                        sum += before
                    }
                }
                sum
            }
            cleaned = "已清理约 " + humanSize(freed)
            scan()
        }
    }

    val usedFraction = if (totalBytes == 0L) 0f else usedBytes.toFloat() / totalBytes
    val appTotal = usages.sumOf { it.bytes }

    ToolScaffold {
        item { SectionHeader("设备存储") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("已用", humanSize(usedBytes), Modifier.weight(1f))
                        StatCell("可用", humanSize(freeBytes), Modifier.weight(1f))
                        StatCell("占用率", String.format("%.0f%%", usedFraction * 100), Modifier.weight(1f))
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(palette.sunkenBackground)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(usedFraction.coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    when {
                                        usedFraction > 0.92f -> palette.red
                                        usedFraction > 0.8f -> palette.orange
                                        else -> palette.green
                                    }
                                )
                        )
                    }
                    if (usedFraction > 0.9f) {
                        Text(
                            "剩余空间不足 10%，系统会开始卡顿，建议腾一些出来。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    }
                    KeyValueRow("总容量", humanSize(totalBytes), copyable = false)
                }
            }
        }
        item { SectionHeader("本应用占用") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = { scan() },
                            modifier = Modifier.weight(1f),
                            enabled = !scanning
                        ) { Text(if (scanning) "扫描中…" else "扫描本应用目录") }
                        SolidButton(
                            onClick = { clearCaches() },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = !scanning && usages.isNotEmpty()
                        ) { Text("清理缓存") }
                    }
                    if (cleaned.isNotBlank()) {
                        Text(cleaned, style = MaterialTheme.typography.bodyMedium, color = palette.green)
                    }
                    Text(
                        "只统计和清理本应用自己的目录，不会触碰你的相册、下载或其他应用的数据。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        if (usages.isNotEmpty()) {
            item { SectionHeader("目录明细（合计 " + humanSize(appTotal) + "）") }
            item {
                GroupedCard {
                    usages.forEachIndexed { index, usage ->
                        KeyValueRow(
                            usage.label,
                            humanSize(usage.bytes) + "　" + usage.files + " 个文件",
                            copyable = false
                        )
                        if (index != usages.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
