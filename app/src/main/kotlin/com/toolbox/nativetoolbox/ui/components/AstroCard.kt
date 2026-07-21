package com.toolbox.nativetoolbox.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.theme.AstroCorner
import com.toolbox.nativetoolbox.ui.theme.AstroDuration
import com.toolbox.nativetoolbox.ui.theme.AstroEasing
import com.toolbox.nativetoolbox.ui.theme.AstroElevation
import com.toolbox.nativetoolbox.ui.theme.AstroSpacing

/**
 * Astro Kit 标准卡片组件
 * 支持按压动画和自定义样式
 */
@Composable
fun AstroCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    elevation: Dp = AstroElevation.level2,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(AstroSpacing.md),
    enablePressAnimation: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedElevation by animateDpAsState(
        targetValue = if (enablePressAnimation && isPressed) elevation * 0.5f else elevation,
        animationSpec = tween(
            durationMillis = AstroDuration.quick.inWholeMilliseconds.toInt(),
            easing = AstroEasing.standard
        ),
        label = "card_elevation"
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
            border = border,
            interactionSource = interactionSource
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * 预设样式 - 信息卡片（轮廓样式）
 */
@Composable
fun AstroInfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AstroCard(
        modifier = modifier,
        shape = CardDefaults.shape,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        elevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content
    )
}

/**
 * 预设样式 - 强调卡片（带主色背景）
 */
@Composable
fun AstroEmphasizedCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AstroCard(
        onClick = onClick,
        modifier = modifier,
        shape = CardDefaults.shape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        elevation = AstroElevation.level3,
        content = content
    )
}
