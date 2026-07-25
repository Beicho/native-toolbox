package com.toolbox.nativetoolbox.ui.liquid

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

data class LiquidTab(val icon: ImageVector, val label: String)

/**
 * iOS 风格玻璃底栏。
 *
 * 【为什么不用「三层折射」方案】
 * 旧实现靠一个 alpha(0f) 的强调色图标层 + 滑块的 lens 折射把它「透」出来做选中态。
 * 但 lens 依赖 RuntimeShader(API 33+),而代码只判断了 isRenderEffectSupported(API 31+)。
 * 结果 Android 12/12L 及更早的机器上 lens 是空操作 —— 强调色图标永远透不出来,
 * 用户完全看不出当前选中哪个 Tab。另外滑块盖住当前 Tab 还吞掉了它的点击。
 *
 * 现在:选中态直接给图标上色(任何设备都对),玻璃面板 blur 在 API 31+ 生效、
 * 以下退化为半透明底,lens 只在 API 33+ 作为锦上添花叠加。滑块只是跟随的高亮胶囊,
 * 不承担显色职责,也不拦截点击。
 */
@Composable
fun LiquidBottomTabs(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    tabs: List<LiquidTab>,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return
    val palette = LocalIosPalette.current
    val backdrop = LocalRootBackdrop.current
    val density = LocalDensity.current

    // 面板底色:玻璃不可用时加重,保证可读性
    val containerColor =
        if (isRenderEffectSupported()) palette.glassSurface
        else palette.glassSurface.copy(alpha = 0.94f)

    val thumbOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "thumb"
    )

    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val innerWidth = maxWidth - 8.dp
        val tabWidthPx = with(density) { innerWidth.toPx() / tabs.size }

        Box(
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        blur(8f.dp.toPx())
                        // lens 需要 RuntimeShader(API 33+),低版本调用是空操作,
                        // 显式判断避免误以为它生效
                        if (isRuntimeShaderSupported()) {
                            lens(20f.dp.toPx(), 20f.dp.toPx())
                        }
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow(radius = 20.dp, color = Color.Black.copy(alpha = 0.10f)) },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .height(64.dp)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // 跟随选中项的高亮胶囊:纯色,不依赖任何 shader
            Box(
                Modifier
                    .graphicsLayer { translationX = thumbOffset * tabWidthPx }
                    .fillMaxWidth(1f / tabs.size)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp)
                    .clip(Capsule())
                    .background(
                        if (palette.isDark) Color.White.copy(alpha = 0.12f)
                        else Color.Black.copy(alpha = 0.06f)
                    )
            )

            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    val tint by animateColorAsState(
                        targetValue = if (selected) palette.accent else palette.secondaryLabel,
                        label = "tint"
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(Capsule())
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onSelected(index) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(26.dp),
                            tint = tint
                        )
                        Text(
                            tab.label,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = tint
                        )
                    }
                }
            }
        }
    }
}
