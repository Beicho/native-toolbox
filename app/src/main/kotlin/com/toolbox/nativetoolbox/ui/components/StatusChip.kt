package com.toolbox.nativetoolbox.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.theme.AstroCorner
import com.toolbox.nativetoolbox.ui.theme.AstroSpacing

/**
 * 状态芯片组件
 * 用于显示状态标签（成功/失败/进行中等）
 */
@Composable
fun StatusChip(
    text: String,
    type: StatusType,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = scaleOut()
    ) {
        val colors = when (type) {
            StatusType.SUCCESS -> ChipColors(
                background = MaterialTheme.colorScheme.primaryContainer,
                text = MaterialTheme.colorScheme.onPrimaryContainer,
                border = MaterialTheme.colorScheme.primary
            )
            StatusType.ERROR -> ChipColors(
                background = MaterialTheme.colorScheme.errorContainer,
                text = MaterialTheme.colorScheme.onErrorContainer,
                border = MaterialTheme.colorScheme.error
            )
            StatusType.WARNING -> ChipColors(
                background = MaterialTheme.colorScheme.tertiaryContainer,
                text = MaterialTheme.colorScheme.onTertiaryContainer,
                border = MaterialTheme.colorScheme.tertiary
            )
            StatusType.INFO -> ChipColors(
                background = MaterialTheme.colorScheme.secondaryContainer,
                text = MaterialTheme.colorScheme.onSecondaryContainer,
                border = MaterialTheme.colorScheme.secondary
            )
            StatusType.NEUTRAL -> ChipColors(
                background = MaterialTheme.colorScheme.surfaceVariant,
                text = MaterialTheme.colorScheme.onSurfaceVariant,
                border = MaterialTheme.colorScheme.outline
            )
        }

        Box(
            modifier = modifier
                .background(
                    color = colors.background,
                    shape = RoundedCornerShape(AstroCorner.full)
                )
                .border(
                    width = 1.dp,
                    color = colors.border.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(AstroCorner.full)
                )
                .padding(horizontal = AstroSpacing.sm, vertical = AstroSpacing.xxs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = colors.text,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

enum class StatusType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    NEUTRAL
}

private data class ChipColors(
    val background: Color,
    val text: Color,
    val border: Color
)
