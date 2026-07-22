package com.toolbox.nativetoolbox.ui.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch

/**
 * Liquid Glass 胶囊按钮(参考 Kyant catalog LiquidButton):
 * 玻璃折射 + 按压时放大、折射增强。tint 传 accent 即 iOS 主操作按钮。
 */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    height: Dp = 48.dp,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val palette = LocalIosPalette.current
    val backdrop = LocalRootBackdrop.current
    val scope = rememberCoroutineScope()
    val pressProgress = remember { Animatable(0f) }
    val pressSpec = remember { spring<Float>(0.5f, 380f, 0.001f) }

    val surfaceColor =
        if (tint.isSpecified) Color.Transparent
        else if (isRenderEffectSupported()) palette.glassSurface
        else palette.glassSurface.copy(alpha = 0.92f)

    Row(
        modifier
            .semantics { role = Role.Button }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(
                        12f.dp.toPx() * (1f + pressProgress.value),
                        24f.dp.toPx() * (1f + 0.5f * pressProgress.value)
                    )
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(radius = 12.dp, color = Color.Black.copy(alpha = 0.10f)) },
                layerBlock = {
                    val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, pressProgress.value)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = if (isRenderEffectSupported()) 0.75f else 0.95f))
                    } else {
                        drawRect(surfaceColor)
                    }
                    if (pressProgress.value > 0f) {
                        drawRect(Color.White.copy(alpha = 0.12f * pressProgress.value), blendMode = BlendMode.Plus)
                    }
                }
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    scope.launch { pressProgress.animateTo(1f, pressSpec) }
                    val up = waitForUpOrCancellation()
                    scope.launch { pressProgress.animateTo(0f, pressSpec) }
                    if (up != null) onClick()
                }
            }
            .height(height)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** 圆形玻璃图标按钮(顶栏返回等) */
@Composable
fun LiquidIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable () -> Unit
) {
    val palette = LocalIosPalette.current
    val backdrop = LocalRootBackdrop.current
    val scope = rememberCoroutineScope()
    val pressProgress = remember { Animatable(0f) }
    val pressSpec = remember { spring<Float>(0.5f, 380f, 0.001f) }

    Box(
        modifier
            .semantics { role = Role.Button }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(10f.dp.toPx(), 20f.dp.toPx())
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(radius = 10.dp, color = Color.Black.copy(alpha = 0.08f)) },
                layerBlock = {
                    val scale = lerp(1f, 1.08f, pressProgress.value)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    drawRect(
                        if (isRenderEffectSupported()) palette.glassSurface
                        else palette.glassSurface.copy(alpha = 0.92f)
                    )
                    if (pressProgress.value > 0f) {
                        drawRect(Color.White.copy(alpha = 0.12f * pressProgress.value), blendMode = BlendMode.Plus)
                    }
                }
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    scope.launch { pressProgress.animateTo(1f, pressSpec) }
                    val up = waitForUpOrCancellation()
                    scope.launch { pressProgress.animateTo(0f, pressSpec) }
                    if (up != null) onClick()
                }
            }
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
