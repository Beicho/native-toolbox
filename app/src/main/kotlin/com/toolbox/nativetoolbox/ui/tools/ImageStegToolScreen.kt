package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAGIC = 0x41_53_54_52 // "ASTR",防止把普通图当隐写图误读

/** 把文字藏进 RGB 最低位:8 字节头(魔数+长度)+ UTF-8 正文 */
private fun embed(src: Bitmap, text: String): Bitmap? {
    val payload = text.toByteArray(Charsets.UTF_8)
    val header = ByteArray(8)
    for (i in 0..3) header[i] = (MAGIC shr (24 - i * 8) and 0xFF).toByte()
    for (i in 0..3) header[4 + i] = (payload.size shr (24 - i * 8) and 0xFF).toByte()
    val data = header + payload
    val capacityBits = src.width.toLong() * src.height * 3
    if (data.size * 8L > capacityBits) return null

    val out = src.copy(Bitmap.Config.ARGB_8888, true) ?: return null
    val pixels = IntArray(out.width * out.height)
    out.getPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
    var bit = 0
    outer@ for (p in pixels.indices) {
        var px = pixels[p]
        for (ch in 0..2) { // R,G,B 三通道
            if (bit >= data.size * 8) break@outer
            val b = (data[bit / 8].toInt() shr (7 - bit % 8)) and 1
            val shift = 16 - ch * 8
            px = (px and (1 shl shift).inv()) or (b shl shift)
            bit++
        }
        pixels[p] = px
    }
    out.setPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
    return out
}

private fun extract(src: Bitmap): String? {
    val pixels = IntArray(src.width * src.height)
    src.getPixels(pixels, 0, src.width, 0, 0, src.width, src.height)
    val totalBits = pixels.size.toLong() * 3

    fun readBytes(count: Int, startBit: Long): ByteArray? {
        if (startBit + count * 8L > totalBits) return null
        val out = ByteArray(count)
        var bit = startBit
        for (i in 0 until count) {
            var v = 0
            repeat(8) {
                val pixelIdx = (bit / 3).toInt()
                val ch = (bit % 3).toInt()
                val shift = 16 - ch * 8
                v = (v shl 1) or ((pixels[pixelIdx] shr shift) and 1)
                bit++
            }
            out[i] = v.toByte()
        }
        return out
    }

    val header = readBytes(8, 0) ?: return null
    var magic = 0
    for (i in 0..3) magic = (magic shl 8) or (header[i].toInt() and 0xFF)
    if (magic != MAGIC) return null
    var len = 0
    for (i in 4..7) len = (len shl 8) or (header[i].toInt() and 0xFF)
    if (len <= 0 || len > 10_000_000) return null
    val body = readBytes(len, 64) ?: return null
    return runCatching { String(body, Charsets.UTF_8) }.getOrNull()
}

@Composable
fun ImageStegToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf(0) } // 0 藏字 1 取字
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var secret by rememberSaveable { mutableStateOf("") }
    var extracted by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        source = ImageUtil.loadBitmap(context, uri, 3000)
        extracted = null
        status = if (source == null) "图片读不出来" else ""
        // 取字模式:选图后直接尝试提取
        if (mode == 1 && source != null) {
            busy = true
            scope.launch {
                val r = withContext(Dispatchers.Default) { extract(source!!) }
                extracted = r
                status = if (r == null) "这张图里没有藏字(或已被压缩破坏)" else ""
                busy = false
            }
        }
    }

    ToolScaffold {
        item {
            if (mode == 1 && extracted != null) OutputCard(extracted!!, Modifier, label = "取出的文字")
        }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("把字藏进图", "从图里取字"), mode, { mode = it; status = ""; extracted = null }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    val bmp = source
                    Text(
                        when {
                            bmp == null -> "先选一张图"
                            mode == 0 -> "已选图 ${bmp.width}×${bmp.height},最多能藏 ${(bmp.width.toLong() * bmp.height * 3 / 8 - 8) / 1024} KB 文字"
                            else -> "已选图 ${bmp.width}×${bmp.height}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryLabel
                    )
                    Spacer(Modifier.height(10.dp))
                    SolidButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth(), filled = source == null) {
                        Text(if (source == null) "选图" else "换一张")
                    }
                }
            }
        }
        item {
            if (mode == 0) {
                GroupedCard {
                    CardPadding {
                        IosTextArea(secret, { secret = it }, Modifier.fillMaxWidth(), placeholder = "想藏的悄悄话…", minHeight = 100.dp)
                        Spacer(Modifier.height(10.dp))
                        SolidButton(
                            onClick = {
                                val bmp = source ?: return@SolidButton
                                busy = true; status = ""
                                scope.launch {
                                    val saved = withContext(Dispatchers.Default) {
                                        val stego = embed(bmp, secret) ?: return@withContext "TOO_BIG"
                                        val bytes = ImageUtil.encode(stego, Bitmap.CompressFormat.PNG, 100)
                                        stego.recycle()
                                        ImageUtil.saveToPictures(context, "steg_${System.currentTimeMillis()}.png", bytes, "image/png")
                                            .fold({ it }, { null })
                                    }
                                    status = when (saved) {
                                        "TOO_BIG" -> "文字太长这张图装不下,换大图或删点字"
                                        null -> "保存失败"
                                        else -> "已存到相册($saved)。注意:发原图才有效,微信 QQ 压缩后字就没了"
                                    }
                                    busy = false
                                }
                            },
                            Modifier.fillMaxWidth(),
                            enabled = source != null && secret.isNotBlank() && !busy
                        ) { Text(if (busy) "藏字中…" else "藏好并保存 PNG") }
                        if (status.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("已存")) palette.green else palette.red)
                        }
                    }
                }
            } else if (status.isNotEmpty()) {
                GroupedCard { CardPadding { Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.red) } }
            }
        }
    }
}
