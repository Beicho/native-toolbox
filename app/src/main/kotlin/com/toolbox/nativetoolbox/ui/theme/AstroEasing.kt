package com.toolbox.nativetoolbox.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * Astro Kit 缓动曲线系统
 * Material Motion 标准缓动函数
 */
object AstroEasing {
    // Material Design 标准缓动
    val standard: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)
    val decelerate: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1f)
    val accelerate: Easing = CubicBezierEasing(0.4f, 0.0f, 1f, 1f)
    val emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0f, 1f)

    // Compose 内置缓动（常用）
    val fastOutSlowIn: Easing = FastOutSlowInEasing
    val linearOutSlowIn: Easing = LinearOutSlowInEasing
}
