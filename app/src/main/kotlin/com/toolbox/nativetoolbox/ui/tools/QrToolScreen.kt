package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ShareBus
import com.toolbox.nativetoolbox.util.ImageUtil

@Composable
fun QrToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(0) }
    var input by rememberSaveable { mutableStateOf(ShareBus.consume() ?: "") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var decoded by rememberSaveable { mutableStateOf("") }
    var decodeError by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            decodeError = false
            decoded = ""
            val bitmap = ImageUtil.loadBitmap(context, uri, maxDim = 2048)
            val result = bitmap?.let { ImageUtil.decodeQr(it) }
            if (result != null) decoded = result else decodeError = true
        }
    }

    ToolScaffold {
        item {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SegmentedPicker(
                    options = listOf("生成", "识别"),
                    selectedIndex = mode,
                    onSelected = { mode = it }
                )
            }
        }

        if (mode == 0) {
            item { SectionHeader("内容") }
            item {
                GroupedCard {
                    CardPadding {
                        IosTextArea(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = "输入文本 / 链接…",
                            minHeight = 100.dp
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterHorizontally)) {
                            SolidButton(
                                onClick = {
                                    if (input.isNotBlank()) qrBitmap = ImageUtil.generateQr(input)
                                },
                                filled = true
                            ) {
                                Text("生成", color = Color.White)
                            }
                            SolidButton(
                                onClick = {
                                    val bmp = qrBitmap ?: return@SolidButton
                                    val bytes = ImageUtil.encode(bmp, Bitmap.CompressFormat.PNG, 100)
                                    val name = "qr_${System.currentTimeMillis()}.png"
                                    val result = ImageUtil.saveToPictures(context, name, bytes, "image/png")
                                    Toast.makeText(
                                        context,
                                        if (result.isSuccess) "已保存到 ${result.getOrNull()}" else "保存失败",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                filled = false,
                                enabled = qrBitmap != null
                            ) {
                                Text("保存", color = palette.accent)
                            }
                        }
                    }
                }
            }
            item { SectionHeader("二维码") }
            item {
                GroupedCard {
                    CardPadding {
                        val bmp = qrBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "二维码",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(12.dp)
                            )
                        } else {
                            Text(
                                "生成后在这里展示",
                                color = palette.tertiaryLabel,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    }
                }
            }
        } else {
            item { SectionHeader("从相册识别") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            SolidButton(onClick = { picker.launch("image/*") }, filled = true) {
                                Text("选择图片", color = Color.White)
                            }
                        }
                        if (decodeError) {
                            Text("未识别到二维码/条码", color = palette.red)
                        }
                        OutputCard(text = decoded, label = "识别结果")
                    }
                }
            }
        }
    }
}
