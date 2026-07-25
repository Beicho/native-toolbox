package com.toolbox.nativetoolbox.ui.liquid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

/**
 * 全局内容层 backdrop:MainActivity 把整个内容区注册进来,
 * 悬浮玻璃控件从这里取"背后的内容"做折射。
 */
val LocalRootBackdrop = staticCompositionLocalOf<LayerBackdrop> {
    error("RootBackdrop not provided")
}

/**
 * 标准 Liquid Glass 面板(Apple Regular 变体感觉):
 * blur 在 API 31+ 生效,以下退化为半透明磨砂底;lens 折射需 RuntimeShader(API 33+),
 * 低版本静默跳过。不用 vibrancy —— 它属于 ColorFilter,深色模式下会让玻璃发灰。
 */
fun Modifier.liquidPanel(
    backdrop: Backdrop,
    shape: () -> Shape = { Capsule() },
    surfaceColor: Color,
    blurRadiusPx: Float,
    lensHeightPx: Float,
    lensAmountPx: Float,
    withShadow: Boolean = true
): Modifier {
    // RenderEffect 不可用时玻璃不折射,加重底色保证可读性
    val effectiveSurface =
        if (isRenderEffectSupported()) surfaceColor
        else surfaceColor.copy(alpha = 0.92f)
    return drawBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = {
            blur(blurRadiusPx)
            if (isRuntimeShaderSupported()) lens(lensHeightPx, lensAmountPx)
        },
        shadow = if (withShadow) {
            { Shadow(radius = 16.dp, color = Color.Black.copy(alpha = 0.08f)) }
        } else null,
        highlight = { Highlight.Default },
        onDrawSurface = { drawRect(effectiveSurface) }
    )
}

/** 以 dp 便捷封装的玻璃面板 */
@Composable
fun Modifier.liquidPanel(
    backdrop: Backdrop,
    surfaceColor: Color,
    shape: () -> Shape = { Capsule() },
    blurRadius: Dp = 8.dp,
    lensHeight: Dp = 24.dp,
    lensAmount: Dp = 24.dp,
    withShadow: Boolean = true
): Modifier {
    val density = LocalDensity.current
    return with(density) {
        liquidPanel(
            backdrop = backdrop,
            shape = shape,
            surfaceColor = surfaceColor,
            blurRadiusPx = blurRadius.toPx(),
            lensHeightPx = lensHeight.toPx(),
            lensAmountPx = lensAmount.toPx(),
            withShadow = withShadow
        )
    }
}
