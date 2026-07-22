package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil

private fun escapeWifi(s: String): String =
    s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:").replace("\"", "\\\"")

@Composable
fun WifiQrToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var ssid by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var authIndex by rememberSaveable { mutableStateOf(0) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    var qr by remember { mutableStateOf<Bitmap?>(null) }

    fun generate() {
        if (ssid.isBlank()) return
        val auth = listOf("WPA", "WEP", "nopass")[authIndex]
        val p = if (authIndex == 2) "" else "P:${escapeWifi(password)};"
        val payload = "WIFI:T:$auth;S:${escapeWifi(ssid)};${p}H:${if (hidden) "true" else "false"};;"
        qr = ImageUtil.generateQr(payload)
    }

    ToolScaffold {
        item { SectionHeader("WiFi 信息") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(value = ssid, onValueChange = { ssid = it }, placeholder = "WiFi 名称(SSID)")
                    IosTextField(value = password, onValueChange = { password = it }, placeholder = "密码", mono = true)
                    SegmentedPicker(
                        options = listOf("WPA/WPA2", "WEP", "无密码"),
                        selectedIndex = authIndex,
                        onSelected = { authIndex = it }
                    )
                }
                ToggleRow("隐藏网络", hidden, { hidden = it })
                CardPadding {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterHorizontally)
                    ) {
                        SolidButton(onClick = { generate() }, enabled = ssid.isNotBlank()) {
                            Text("生成", color = Color.White)
                        }
                        SolidButton(
                            onClick = {
                                val bmp = qr ?: return@SolidButton
                                val bytes = ImageUtil.encode(bmp, Bitmap.CompressFormat.PNG, 100)
                                val result = ImageUtil.saveToPictures(context, "wifi_qr_${System.currentTimeMillis()}.png", bytes, "image/png")
                                Toast.makeText(context, if (result.isSuccess) "已保存到相册" else "保存失败", Toast.LENGTH_SHORT).show()
                            },
                            filled = false,
                            enabled = qr != null
                        ) {
                            Text("保存", color = palette.accent)
                        }
                    }
                }
            }
        }
        item { SectionHeader("扫码即连") }
        item {
            GroupedCard {
                CardPadding {
                    val bmp = qr
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "WiFi 二维码",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(12.dp)
                        )
                    } else {
                        Text("填好信息点生成", color = palette.tertiaryLabel, modifier = Modifier.padding(vertical = 24.dp))
                    }
                }
            }
        }
    }
}
