package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private val inkColors = listOf(
    Color(0xFF1C1C1E) to "黑",
    Color(0xFF003D99) to "蓝",
    Color(0xFFB00020) to "红"
)

private val inkWidths = listOf(4f, 7f, 11f)
private val inkWidthLabels = listOf("细", "中", "粗")

@Composable
fun SignatureToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val strokes = remember { mutableListOf<DrawStroke>().toMutableStateList() }

    var colorIndex by rememberSaveable { mutableStateOf(0) }
    var widthIndex by rememberSaveable { mutableStateOf(1) }
    var landscapeHint by rememberSaveable { mutableStateOf(true) }

    val totalPoints = strokes.sumOf { it.points.size }

    ToolScaffold {
        item { SectionHeader("签名区") }
        item {
            GroupedCard {
                CardPadding {
                    DrawingCanvas(
                        strokes = strokes,
                        color = inkColors[colorIndex].first,
                        width = inkWidths[widthIndex],
                        eraser = false,
                        canvasHeight = 260,
                        background = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = strokes.isNotEmpty()
                        ) { Text("撤销一笔") }
                        SolidButton(
                            onClick = { strokes.clear() },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = strokes.isNotEmpty()
                        ) { Text("重签") }
                    }
                    if (strokes.isEmpty() && landscapeHint) {
                        Text(
                            "手机横过来签会更顺手。用指尖或触控笔在白色区域里写。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    }
                }
            }
        }
        item { SectionHeader("笔") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        inkColors.forEachIndexed { index, (c, _) ->
                            Box(
                                Modifier
                                    .size(if (colorIndex == index) 40.dp else 32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { colorIndex = index }
                            )
                        }
                    }
                    SegmentedPicker(
                        options = inkColors.map { it.second },
                        selectedIndex = colorIndex,
                        onSelected = { colorIndex = it }
                    )
                    SegmentedPicker(
                        options = inkWidthLabels,
                        selectedIndex = widthIndex,
                        onSelected = { widthIndex = it }
                    )
                }
            }
        }
        item { SectionHeader("状态") }
        item {
            GroupedCard {
                KeyValueRow("笔画数", strokes.size.toString(), copyable = false)
                RowDivider()
                KeyValueRow("采样点", totalPoints.toString(), copyable = false)
                RowDivider()
                KeyValueRow("墨色", inkColors[colorIndex].second, copyable = false)
                RowDivider()
                KeyValueRow("笔宽", inkWidthLabels[widthIndex], copyable = false)
            }
        }
        item { SectionHeader("怎么用") }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "签好后用系统截图功能截取白色区域，就得到一张签名图。\n\n" +
                            "签名笔迹只在屏幕上，不保存文件、不上传。退出页面即清除。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "电子签名的法律效力取决于签署场景和相关约定，重要文件请走正规电子签约平台。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
