package com.toolbox.nativetoolbox.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Astro Kit 设计令牌系统
 */

// 间距系统
object AstroSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp
}

// 圆角系统
object AstroCorner {
    val none = 0.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val full = 9999.dp
}

// 阴影系统
object AstroElevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 3.dp
    val level3 = 6.dp
    val level4 = 8.dp
    val level5 = 12.dp
}

// 动画时长
object AstroDuration {
    val instant: Duration = 0.milliseconds
    val quick: Duration = 100.milliseconds
    val normal: Duration = 200.milliseconds
    val medium: Duration = 300.milliseconds
    val slow: Duration = 400.milliseconds
    val slower: Duration = 600.milliseconds
}

// 动画缓动曲线
object AstroEasing {
    // Material Design 3 标准曲线
    val standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val decelerated: Easing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val accelerated: Easing = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    // 自定义弹性曲线
    val bouncy: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
}
