package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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

internal class DrawStroke(
    val points: MutableList<Offset>,
    val color: Color,
    val width: Float
)

private val penColors = listOf(
    Color(0xFF1C1C1E),
    Color(0xFFD70015),
    Color(0xFF007AFF),
    Color(0xFF34C759),
    Color(0xFFFF9500),
    Color(0xFFAF52DE)
)

private val penWidths = listOf(3f, 6f, 12f, 22f)
private val penWidthLabels = listOf("细", "中", "粗", "很粗")

@Composable
internal fun DrawingCanvas(
    strokes: SnapshotStateList<DrawStroke>,
    color: Color,
    width: Float,
    eraser: Boolean,
    canvasHeight: Int,
    background: Color
) {
    var active by remember { mutableStateOf<DrawStroke?>(null) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(canvasHeight.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .pointerInput(color, width, eraser) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val stroke = DrawStroke(
                            mutableListOf(offset),
                            if (eraser) background else color,
                            if (eraser) width * 3 else width
                        )
                        active = stroke
                        strokes.add(stroke)
                    },
                    onDrag = { change, _ ->
                        active?.let { stroke ->
                            val index = strokes.indexOf(stroke)
                            if (index >= 0) {
                                val updated = DrawStroke(
                                    (stroke.points + change.position).toMutableList(),
                                    stroke.color,
                                    stroke.width
                                )
                                strokes[index] = updated
                                active = updated
                            }
                        }
                    },
                    onDragEnd = { active = null },
                    onDragCancel = { active = null }
                )
            }
    ) {
        Canvas(Modifier.fillMaxWidth().height(canvasHeight.dp)) {
            strokes.forEach { stroke ->
                if (stroke.points.size == 1) {
                    drawCircle(
                        color = stroke.color,
                        radius = stroke.width / 2,
                        center = stroke.points.first()
                    )
                } else if (stroke.points.size > 1) {
                    val path = Path().apply {
                        moveTo(stroke.points.first().x, stroke.points.first().y)
                        stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = path,
                        color = stroke.color,
                        style = Stroke(
                            width = stroke.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun WhiteboardToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val strokes = remember { mutableListOf<DrawStroke>().toMutableStateList() }

    var colorIndex by rememberSaveable { mutableStateOf(0) }
    var widthIndex by rememberSaveable { mutableStateOf(1) }
    var eraser by rememberSaveable { mutableStateOf(false) }

    ToolScaffold {
        item { SectionHeader("画板") }
        item {
            GroupedCard {
                CardPadding {
                    DrawingCanvas(
                        strokes = strokes,
                        color = penColors[colorIndex],
                        width = penWidths[widthIndex],
                        eraser = eraser,
                        canvasHeight = 420,
                        background = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = strokes.isNotEmpty()
                        ) { Text("撤销") }
                        SolidButton(
                            onClick = { eraser = !eraser },
                            modifier = Modifier.weight(1f),
                            filled = eraser
                        ) { Text(if (eraser) "橡皮中" else "橡皮") }
                        SolidButton(
                            onClick = { strokes.clear() },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = strokes.isNotEmpty()
                        ) { Text("清空") }
                    }
                }
            }
        }
        item { SectionHeader("画笔") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        penColors.forEachIndexed { index, c ->
                            Box(
                                Modifier
                                    .size(if (colorIndex == index && !eraser) 40.dp else 32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable {
                                        colorIndex = index
                                        eraser = false
                                    }
                            )
                        }
                    }
                    SegmentedPicker(
                        options = penWidthLabels,
                        selectedIndex = widthIndex,
                        onSelected = { widthIndex = it }
                    )
                    Text(
                        "上面数字对应左边的颜色顺序。橡皮实际是用白色涂抹。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
