package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private fun renderQr(content: String, size: Int): Bitmap? = runCatching {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    }
}.getOrNull()

@Composable
fun MoveCarToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var mode by rememberSaveable { mutableStateOf(0) }
    var plate by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("挡到您了请打电话，马上来挪") }

    val maskedPhone = if (phone.length >= 7) {
        phone.take(3) + "****" + phone.takeLast(4)
    } else phone

    // 二维码内容：mode 0 直接拨号，mode 1 纯文字
    val qrContent = when (mode) {
        0 -> if (phone.isBlank()) "" else "tel:" + phone.trim()
        else -> buildString {
            if (plate.isNotBlank()) appendLine("车牌 " + plate.trim())
            if (phone.isNotBlank()) appendLine("电话 " + phone.trim())
            if (note.isNotBlank()) append(note.trim())
        }
    }

    val bitmap = remember(qrContent) {
        if (qrContent.isBlank()) null else renderQr(qrContent, 560)
    }

    val printable = buildString {
        appendLine("【临时停放　请多包容】")
        if (plate.isNotBlank()) appendLine("车牌：" + plate.trim())
        if (phone.isNotBlank()) appendLine("电话：" + phone.trim())
        if (note.isNotBlank()) appendLine(note.trim())
        append("扫码可直接拨号")
    }

    ToolScaffold {
        item { SectionHeader("挪车码") }
        item {
            GroupedCard {
                CardPadding {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(palette.sunkenBackground, RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(220.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(8.dp)
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "挪车二维码",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (plate.isNotBlank()) {
                                    Text(
                                        plate.trim(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.label
                                    )
                                }
                                Text(
                                    if (mode == 0) "扫码直接拨号" else "扫码看联系方式",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryLabel
                                )
                            }
                        } else {
                            Text(
                                "填上电话就会生成二维码",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.tertiaryLabel
                            )
                        }
                    }
                    Text(
                        "截图打印出来放在挡风玻璃上，别人扫一下就能联系到你。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("二维码类型") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("扫码直接拨号", "扫码显示文字"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    Text(
                        if (mode == 0) "扫描后手机会直接跳到拨号界面，最方便。"
                        else "扫描后显示文字信息，对方自己决定要不要打。号码会完整暴露。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader("信息") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = plate,
                        onValueChange = { plate = it.uppercase() },
                        placeholder = "车牌号，例如 京A12345"
                    )
                    IosTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { c -> c.isDigit() } },
                        placeholder = "联系电话",
                        mono = true
                    )
                    IosTextArea(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = "留言",
                        minHeight = 80.dp
                    )
                }
            }
        }
        item { SectionHeader("打印文案") }
        item { GroupedCard { CardPadding { OutputCard(text = printable, label = "复制后排版打印") } } }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "把手机号贴在车上会被采集到骚扰电话库里。想避免的话可以用运营商的隐私小号，或者只写微信号。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.orange
                    )
                }
            }
        }
    }
}
