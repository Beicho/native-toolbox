package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/** 常见文件类型的魔数签名，用来判断一段字节到底是什么 */
private val magics = listOf(
    Triple("PNG 图片", byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47), 0),
    Triple("JPEG 图片", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), 0),
    Triple("GIF 图片", "GIF8".toByteArray(), 0),
    Triple("WebP 图片", "WEBP".toByteArray(), 8),
    Triple("PDF 文档", "%PDF".toByteArray(), 0),
    Triple("ZIP 或 APK/JAR", byteArrayOf(0x50, 0x4B, 0x03, 0x04), 0),
    Triple("GZIP 压缩", byteArrayOf(0x1F, 0x8B.toByte()), 0),
    Triple("7z 压缩", byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte()), 0),
    Triple("RAR 压缩", "Rar!".toByteArray(), 0),
    Triple("ELF 可执行", byteArrayOf(0x7F, 0x45, 0x4C, 0x46), 0),
    Triple("Java class", byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte()), 0),
    Triple("MP3 音频", byteArrayOf(0x49, 0x44, 0x33), 0),
    Triple("MP4 视频", "ftyp".toByteArray(), 4),
    Triple("SQLite 数据库", "SQLite format 3".toByteArray(), 0)
)

private fun parseInput(text: String, mode: Int): ByteArray? = runCatching {
    when (mode) {
        0 -> text.toByteArray(Charsets.UTF_8)
        1 -> {
            val clean = text.replace(Regex("[^0-9a-fA-F]"), "")
            if (clean.length % 2 != 0) return@runCatching null
            ByteArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        }
        else -> android.util.Base64.decode(text.trim(), android.util.Base64.DEFAULT)
    }
}.getOrNull()

private fun hexDump(bytes: ByteArray, limit: Int = 2048): String {
    val builder = StringBuilder()
    val shown = bytes.take(limit)
    shown.chunked(16).forEachIndexed { rowIndex, row ->
        val offset = String.format("%08X", rowIndex * 16)
        val hex = row.joinToString(" ") { String.format("%02X", it) }.padEnd(47)
        val ascii = row.joinToString("") { b ->
            val c = b.toInt() and 0xFF
            if (c in 32..126) c.toChar().toString() else "."
        }
        builder.append(offset).append("  ").append(hex).append("  ").append(ascii).append("\n")
    }
    if (bytes.size > limit) {
        builder.append("… 只显示前 ").append(limit).append(" 字节，共 ").append(bytes.size).append(" 字节")
    }
    return builder.toString().trimEnd()
}

private fun detectType(bytes: ByteArray): String {
    magics.forEach { (name, magic, offset) ->
        if (bytes.size >= offset + magic.size) {
            var match = true
            magic.indices.forEach { i -> if (bytes[offset + i] != magic[i]) match = false }
            if (match) return name
        }
    }
    val printable = bytes.take(512).count { (it.toInt() and 0xFF) in 9..126 }
    val ratio = if (bytes.isEmpty()) 0.0 else printable.toDouble() / minOf(bytes.size, 512)
    return if (ratio > 0.9) "看起来是纯文本" else "未识别的二进制数据"
}

@Composable
fun HexViewerToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var input by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(0) }

    val bytes = if (input.isBlank()) null else parseInput(input, mode)
    val dump = bytes?.let { hexDump(it) } ?: ""

    ToolScaffold {
        item { SectionHeader("输入") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("文本", "十六进制", "Base64"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = when (mode) {
                            0 -> "输入任意文本，看它的字节表示"
                            1 -> "粘贴十六进制，空格和换行会自动忽略"
                            else -> "粘贴 Base64 字符串"
                        },
                        minHeight = 110.dp,
                        mono = mode != 0
                    )
                    if (input.isNotBlank() && bytes == null) {
                        Text(
                            if (mode == 1) "十六进制格式不对，字符数要成双" else "Base64 解不出来，检查一下内容",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    }
                }
            }
        }
        if (bytes != null) {
            item { SectionHeader("概览") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("字节数", bytes.size.toString(), Modifier.weight(1f))
                            StatCell(
                                "大小",
                                if (bytes.size < 1024) bytes.size.toString() + " B"
                                else String.format("%.2f KB", bytes.size / 1024.0),
                                Modifier.weight(1f)
                            )
                            StatCell("行数", ((bytes.size + 15) / 16).toString(), Modifier.weight(1f))
                        }
                        Text(
                            detectType(bytes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.accent
                        )
                    }
                }
            }
            item { SectionHeader("十六进制转储") }
            item { GroupedCard { CardPadding { OutputCard(text = dump, label = "offset  hex  ascii") } } }
            item { SectionHeader("其他表示") }
            item {
                GroupedCard {
                    KeyValueRow(
                        "连续十六进制",
                        bytes.take(64).joinToString("") { String.format("%02X", it) } +
                            if (bytes.size > 64) "…" else ""
                    )
                    RowDivider()
                    KeyValueRow(
                        "Base64",
                        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP).take(120) +
                            if (bytes.size > 90) "…" else ""
                    )
                    RowDivider()
                    KeyValueRow(
                        "前 8 字节十进制",
                        bytes.take(8).joinToString(" ") { (it.toInt() and 0xFF).toString() }
                    )
                    RowDivider()
                    KeyValueRow(
                        "UTF-8 解码",
                        runCatching { String(bytes, Charsets.UTF_8).take(80) }.getOrDefault("解不出来")
                    )
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "会按魔数识别常见文件类型。转储最多显示前 2048 字节，全部在本地计算。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
