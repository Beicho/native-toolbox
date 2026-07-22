package com.toolbox.nativetoolbox.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.theme.MonoStyle

/** iOS 风格圆角填充输入框 */
@Composable
fun IosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    mono: Boolean = false
) {
    val palette = LocalIosPalette.current
    val baseStyle = if (mono) MonoStyle else MaterialTheme.typography.bodyMedium
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.sunkenBackground),
        textStyle = baseStyle.copy(color = palette.label),
        cursorBrush = SolidColor(palette.accent),
        singleLine = singleLine,
        minLines = minLines,
        decorationBox = { inner ->
            Box(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = baseStyle, color = palette.tertiaryLabel)
                }
                inner()
            }
        }
    )
}

/** 多行文本输入区 */
@Composable
fun IosTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minHeight: androidx.compose.ui.unit.Dp = 120.dp,
    mono: Boolean = false
) {
    val palette = LocalIosPalette.current
    val baseStyle = if (mono) MonoStyle else MaterialTheme.typography.bodyMedium
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.sunkenBackground),
        textStyle = baseStyle.copy(color = palette.label),
        cursorBrush = SolidColor(palette.accent),
        decorationBox = { inner ->
            Box(Modifier.padding(12.dp)) {
                if (value.isEmpty()) {
                    Text(placeholder, style = baseStyle, color = palette.tertiaryLabel)
                }
                inner()
            }
        }
    )
}

/** iOS 分段选择器(滑动白色滑块) */
@Composable
fun SegmentedPicker(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalIosPalette.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(palette.fill.copy(alpha = 0.35f))
            .padding(2.dp)
    ) {
        val segWidth = maxWidth / options.size
        val segWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { segWidth.toPx() }
        val offsetX by animateFloatAsState(
            targetValue = selectedIndex * segWidthPx,
            animationSpec = spring(0.85f, 500f),
            label = "seg"
        )
        Box(
            Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxHeight()
                .fillMaxWidth(1f / options.size)
                .shadow(1.dp, RoundedCornerShape(7.dp))
                .clip(RoundedCornerShape(7.dp))
                .background(if (palette.isDark) Color(0xFF636366) else Color.White)
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                val textColor by animateColorAsState(
                    targetValue = palette.label,
                    label = "segText"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        option,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (index == selectedIndex) {
                                androidx.compose.ui.text.font.FontWeight.SemiBold
                            } else {
                                androidx.compose.ui.text.font.FontWeight.Normal
                            }
                        ),
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
