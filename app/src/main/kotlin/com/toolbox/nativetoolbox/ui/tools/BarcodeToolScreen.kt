package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.theme.MonoStyle

private class BarcodeKind(
    val name: String,
    val format: BarcodeFormat,
    val hint: String,
    val validate: (String) -> String?
)

private fun digitsOnly(text: String, expected: Int): String? = when {
    text.any { !it.isDigit() } -> "只能是数字"
    text.length != expected -> "要正好 " + expected + " 位数字"
    else -> null
}

private val kinds = listOf(
    BarcodeKind("CODE 128", BarcodeFormat.CODE_128, "最通用，支持字母数字和符号，物流快递常用") { text ->
        if (text.isEmpty()) "输入点内容" else null
    },
    BarcodeKind("CODE 39", BarcodeFormat.CODE_39, "只支持大写字母、数字和少量符号，工业设备常见") { text ->
        when {
            text.isEmpty() -> "输入点内容"
            text.any { it !in "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%" } ->
                "只支持大写字母、数字和 - . 空格 $ / + %"
            else -> null
        }
    },
    BarcodeKind("EAN-13", BarcodeFormat.EAN_13, "商品条码，超市扫的就是这个") { text ->
        digitsOnly(text, 13) ?: digitsOnly(text, 12).let { if (text.length == 12) null else it }
    },
    BarcodeKind("EAN-8", BarcodeFormat.EAN_8, "短版商品条码，小包装用") { text -> digitsOnly(text, 8) },
    BarcodeKind("UPC-A", BarcodeFormat.UPC_A, "北美商品条码") { text -> digitsOnly(text, 12) },
    BarcodeKind("ITF", BarcodeFormat.ITF, "交叉二五码，外箱包装用，位数必须是偶数") { text ->
        when {
            text.any { !it.isDigit() } -> "只能是数字"
            text.length % 2 != 0 -> "位数必须是偶数"
            text.isEmpty() -> "输入点内容"
            else -> null
        }
    }
)

private fun renderBarcode(content: String, format: BarcodeFormat, width: Int, height: Int): Bitmap? =
    runCatching {
        val hints = mapOf(EncodeHintType.MARGIN to 2)
        val matrix = MultiFormatWriter().encode(content, format, width, height, hints)
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, y,
                        if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
        }
    }.getOrNull()

/** EAN-13 校验位：奇偶加权求和后取补 */
private fun ean13Checksum(twelve: String): Char? {
    if (twelve.length != 12 || twelve.any { !it.isDigit() }) return null
    var sum = 0
    twelve.forEachIndexed { index, c ->
        sum += (c - '0') * (if (index % 2 == 0) 1 else 3)
    }
    return ('0' + ((10 - sum % 10) % 10))
}

@Composable
fun BarcodeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var content by rememberSaveable { mutableStateOf("") }
    var kindIndex by rememberSaveable { mutableStateOf(0) }
    var showText by rememberSaveable { mutableStateOf(true) }

    val kind = kinds[kindIndex]
    val trimmed = content.trim()
    val error = kind.validate(trimmed)

    // EAN-13 输入 12 位时自动补校验位
    val effective = if (kind.format == BarcodeFormat.EAN_13 && trimmed.length == 12) {
        trimmed + (ean13Checksum(trimmed) ?: ' ')
    } else trimmed

    val bitmap = remember(effective, kindIndex) {
        if (error != null || effective.isBlank()) null
        else renderBarcode(effective, kind.format, 720, 260)
    }

    ToolScaffold {
        item { SectionHeader("条形码") }
        item {
            GroupedCard {
                CardPadding {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "条形码",
                                    modifier = Modifier.fillMaxWidth().height(120.dp)
                                )
                                if (showText) {
                                    Text(
                                        effective,
                                        style = MonoStyle.copy(color = Color.Black, fontSize = 14.sp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                error ?: "输入内容生成条形码",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                    Text(
                        "截图后可以打印贴在物品上，用扫码枪或手机相机都能读。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("内容") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = kind.hint,
                        mono = true
                    )
                    if (error != null && trimmed.isNotEmpty()) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = palette.red)
                    }
                    if (kind.format == BarcodeFormat.EAN_13 && trimmed.length == 12) {
                        Text(
                            "已自动补上校验位 " + (ean13Checksum(trimmed) ?: '?'),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.green
                        )
                    }
                    com.toolbox.nativetoolbox.ui.components.ToggleRow(
                        "在条码下显示文字",
                        showText,
                        onCheckedChange = { showText = it }
                    )
                }
            }
        }
        item { SectionHeader("码制") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = kinds.take(3).map { it.name },
                        selectedIndex = kindIndex.coerceAtMost(2),
                        onSelected = { kindIndex = it }
                    )
                    SegmentedPicker(
                        options = kinds.drop(3).map { it.name },
                        selectedIndex = (kindIndex - 3).coerceAtLeast(0),
                        onSelected = { kindIndex = it + 3 }
                    )
                    Text(
                        kind.hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
    }
}
