package com.toolbox.nativetoolbox.ui.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch

data class LiquidTab(val icon: ImageVector, val label: String)

/**
 * iOS 风格 Liquid Glass 底部标签栏(参考 Kyant catalog LiquidBottomTabs 三层结构):
 * 玻璃胶囊面板 + 强调色图标层(仅透过滑块可见)+ 可拖拽玻璃滑块(按压折射/色散)。
 */
@Composable
fun LiquidBottomTabs(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    tabs: List<LiquidTab>,
    modifier: Modifier = Modifier
) {
    val palette = LocalIosPalette.current
    val backdrop = LocalRootBackdrop.current
    val tabsBackdrop = rememberLayerBackdrop()
    val scope = rememberCoroutineScope()
    val currentOnSelected by rememberUpdatedState(onSelected)

    val thumbValue = remember { Animatable(selectedIndex.toFloat()) }
    val pressProgress = remember { Animatable(0f) }
    val valueSpec = remember { spring<Float>(0.8f, 380f, 0.001f) }
    val pressSpec = remember { spring<Float>(0.6f, 300f, 0.001f) }

    // 外部选中变化(如返回键)时滑块跟过去
    LaunchedEffect(selectedIndex) {
        if (thumbValue.targetValue.fastRoundToInt() != selectedIndex) {
            thumbValue.animateTo(selectedIndex.toFloat(), valueSpec)
        }
    }

    val containerColor =
        if (isRenderEffectSupported()) palette.glassSurface
        else palette.glassSurface.copy(alpha = 0.92f)

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val tabWidthPx = with(density) { (maxWidth - 8.dp).toPx() / tabs.size }

        fun settleTo(index: Int) {
            val target = index.coerceIn(0, tabs.lastIndex)
            scope.launch { thumbValue.animateTo(target.toFloat(), valueSpec) }
            if (target != selectedIndex) currentOnSelected(target)
        }

        @Composable
        fun TabsRow(tint: Color?) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    Column(
                        Modifier
                            .weight(1f)
                            .then(
                                if (tint == null) Modifier.pointerInput(index) {
                                    detectTapGestures(
                                        onPress = {
                                            scope.launch { pressProgress.animateTo(1f, pressSpec) }
                                            tryAwaitRelease()
                                            scope.launch { pressProgress.animateTo(0f, pressSpec) }
                                        },
                                        onTap = { settleTo(index) }
                                    )
                                } else Modifier
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(26.dp),
                            tint = tint ?: palette.secondaryLabel
                        )
                        Text(
                            tab.label,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = tint ?: palette.secondaryLabel
                        )
                    }
                }
            }
        }

        // 第一层:玻璃面板 + 灰色图标
        Box(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow(radius = 20.dp, color = Color.Black.copy(alpha = 0.10f)) },
                    layerBlock = {
                        val scale = lerp(1f, 1f + 8f.dp.toPx() / size.width, pressProgress.value)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .height(64.dp)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            TabsRow(tint = null)
        }

        // 第二层:强调色图标层,仅注册为 tabsBackdrop(屏幕上不可见),
        // 滑块玻璃组合此层后,滑过哪个 tab 哪个图标就"透"成强调色。
        Box(
            Modifier
                .clearAndSetSemantics {}
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .height(64.dp)
                .fillMaxWidth()
                .padding(4.dp)
                .graphicsLayer(colorFilter = ColorFilter.tint(palette.accent))
        ) {
            TabsRow(tint = palette.accent)
        }

        // 第三层:滑块(组合 根backdrop + 图标层)
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX = thumbValue.value * tabWidthPx
                }
                .pointerInput(tabs.size) {
                    detectDragGestures(
                        onDragStart = {
                            scope.launch { pressProgress.animateTo(1f, pressSpec) }
                        },
                        onDragEnd = {
                            scope.launch { pressProgress.animateTo(0f, pressSpec) }
                            settleTo(thumbValue.value.fastRoundToInt())
                        },
                        onDragCancel = {
                            scope.launch { pressProgress.animateTo(0f, pressSpec) }
                            settleTo(thumbValue.value.fastRoundToInt())
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            thumbValue.snapTo(
                                (thumbValue.value + dragAmount.x / tabWidthPx)
                                    .fastCoerceIn(0f, tabs.lastIndex.toFloat())
                            )
                        }
                    }
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        val p = pressProgress.value
                        lens(
                            10f.dp.toPx() * p,
                            14f.dp.toPx() * p,
                            chromaticAberration = true
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = pressProgress.value) },
                    shadow = { Shadow(alpha = pressProgress.value) },
                    innerShadow = {
                        InnerShadow(
                            radius = 8.dp * pressProgress.value,
                            alpha = pressProgress.value
                        )
                    },
                    layerBlock = {
                        val scale = lerp(1f, 78f / 56f, pressProgress.value * 0.35f)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = {
                        val p = pressProgress.value
                        drawRect(
                            if (palette.isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f),
                            alpha = 1f - p
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * p))
                    }
                )
                .height(56.dp)
                .fillMaxWidth(1f / tabs.size)
        )
    }
}
