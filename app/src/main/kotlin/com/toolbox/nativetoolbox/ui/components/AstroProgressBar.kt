package com.toolbox.nativetoolbox.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.theme.AstroCorner
import com.toolbox.nativetoolbox.ui.theme.AstroDuration
import com.toolbox.nativetoolbox.ui.theme.AstroEasing
import com.toolbox.nativetoolbox.ui.theme.AstroSpacing

/**
 * 进度条组件
 * 支持百分比显示和颜色动画
 */
@Composable
fun AstroProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true,
    height: dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = AstroDuration.medium.inWholeMilliseconds.toInt(),
            easing = AstroEasing.emphasized
        ),
        label = "progress"
    )

    val animatedColor by animateColorAsState(
        targetValue = when {
            animatedProgress >= 1f -> MaterialTheme.colorScheme.primary
            animatedProgress >= 0.7f -> MaterialTheme.colorScheme.tertiary
            else -> progressColor
        },
        animationSpec = tween(
            durationMillis = AstroDuration.medium.inWholeMilliseconds.toInt()
        ),
        label = "progress_color"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AstroSpacing.xs)
    ) {
        if (showPercentage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "进度",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = animatedColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(AstroCorner.full))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(animatedColor)
            )
        }
    }
}

/**
 * 不确定进度条（无限循环）
 */
@Composable
fun AstroIndeterminateProgressBar(
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(AstroCorner.full)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
