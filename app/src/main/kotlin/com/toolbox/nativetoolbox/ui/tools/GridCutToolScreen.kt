package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlin.math.min

/** 中心裁方后切 N×N */
private fun cut(src: Bitmap, n: Int): List<Bitmap> {
    val side = min(src.width, src.height)
    val x0 = (src.width - side) / 2
    val y0 = (src.height - side) / 2
    val cell = side / n
    return buildList {
        for (r in 0 until n) {
            for (c in 0 until n) {
                add(Bitmap.createBitmap(src, x0 + c * cell, y0 + r * cell, cell, cell))
            }
        }
    }
}

@Composable
fun GridCutToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var pieces by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var gridIndex by rememberSaveable { mutableStateOf(1) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u: Uri? ->
        if (u != null) {
            source = ImageUtil.loadBitmap(context, u, maxDim = 2560)
            pieces = emptyList()
        }
    }

    val n = listOf(2, 3, 4)[gridIndex]

    fun doCut() {
        val src = source ?: return
        pieces = cut(src, n)
    }

    fun saveAll() {
        if (pieces.isEmpty()) return
        val stamp = System.currentTimeMillis()
        var ok = 0
        pieces.forEachIndexed { i, bmp ->
            val bytes = ImageUtil.encode(bmp, Bitmap.CompressFormat.JPEG, 95)
            if (ImageUtil.saveToPictures(context, "grid_${stamp}_${i + 1}.jpg", bytes, "image/jpeg").isSuccess) ok++
        }
        Toast.makeText(context, "已保存 $ok/${pieces.size} 张(按序号 1-${pieces.size} 发圈)", Toast.LENGTH_SHORT).show()
    }

    ToolScaffold {
        item { SectionHeader("九宫格切图(朋友圈裂图)") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("2×2", "3×3", "4×4"),
                        selectedIndex = gridIndex,
                        onSelected = { gridIndex = it; pieces = emptyList() }
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        SolidButton(onClick = { picker.launch("image/*") }) {
                            Text("选图", color = Color.White)
                        }
                        SolidButton(onClick = { doCut() }, enabled = source != null) {
                            Text("切割", color = Color.White)
                        }
                        SolidButton(onClick = { saveAll() }, filled = false, enabled = pieces.isNotEmpty()) {
                            Text("全部保存", color = palette.accent)
                        }
                    }
                }
            }
        }
        item { SectionHeader("预览") }
        item {
            GroupedCard {
                CardPadding {
                    if (pieces.isEmpty()) {
                        val src = source
                        if (src != null) {
                            Image(
                                bitmap = src.asImageBitmap(),
                                contentDescription = "原图",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Text("选一张图,居中裁方后切割", color = palette.tertiaryLabel)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            pieces.chunked(n).forEach { rowPieces ->
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    rowPieces.forEach { piece ->
                                        Image(
                                            bitmap = piece.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
