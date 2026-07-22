package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.onSizeChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import androidx.compose.foundation.gestures.detectTapGestures

@Composable
fun PickColorToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u: Uri? ->
        if (u != null) {
            bitmap = ImageUtil.loadBitmap(context, u, maxDim = 2048)
            picked = null
        }
    }

    val color = picked?.let { Color(it) }
    val hex = picked?.let { "#%06X".format(it and 0xFFFFFF) } ?: ""

    ToolScaffold {
        item { SectionHeader("图片(点任意位置取色)") }
        item {
            GroupedCard {
                CardPadding {
                    val bmp = bitmap
                    if (bmp == null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            SolidButton(onClick = { picker.launch("image/*") }) {
                                Text("选择图片", color = Color.White)
                            }
                        }
                    } else {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "取色图片",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .onSizeChanged { viewSize = it }
                                .pointerInput(bmp) {
                                    detectTapGestures { offset ->
                                        // ContentScale.Fit:按比例映射回原图坐标
                                        val scale = minOf(
                                            viewSize.width.toFloat() / bmp.width,
                                            viewSize.height.toFloat() / bmp.height
                                        )
                                        val dx = (viewSize.width - bmp.width * scale) / 2f
                                        val dy = (viewSize.height - bmp.height * scale) / 2f
                                        val x = ((offset.x - dx) / scale).toInt()
                                        val y = ((offset.y - dy) / scale).toInt()
                                        if (x in 0 until bmp.width && y in 0 until bmp.height) {
                                            picked = bmp.getPixel(x, y)
                                        }
                                    }
                                }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            SolidButton(onClick = { picker.launch("image/*") }, filled = false) {
                                Text("换一张", color = palette.accent)
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeader("取到的颜色(点击复制)") }
        item {
            GroupedCard {
                CardPadding {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color ?: palette.sunkenBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == null) {
                            Text("还没取色", color = palette.tertiaryLabel)
                        }
                    }
                }
                KeyValueRow("HEX", hex)
                RowDivider()
                KeyValueRow(
                    "RGB",
                    picked?.let { "rgb(${(it shr 16) and 0xFF}, ${(it shr 8) and 0xFF}, ${it and 0xFF})" } ?: ""
                )
            }
        }
    }
}
