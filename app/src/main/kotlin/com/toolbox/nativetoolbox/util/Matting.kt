package com.toolbox.nativetoolbox.util

import android.graphics.Bitmap

/**
 * 抠图入口。
 *
 * 原来用 ML Kit selfie-segmentation,但它带 mediapipe 一共 22MB ——
 * 只为一个工具让所有用户多下 22MB 不划算。改用 SmartCutout(纯算法,零依赖)。
 * 对纯色/渐变背景效果够用,复杂背景诚实告知用户。
 */
object Matting {

    /** 返回带透明通道的抠图结果;失败返回 null */
    suspend fun cutout(src: Bitmap, threshold: Float = 0.55f, feather: Boolean = true): Bitmap? =
        runCatching {
            // threshold 0..1 映射到颜色容差 12..55
            val tolerance = (12 + threshold * 43).toInt()
            SmartCutout.cutout(src, tolerance = tolerance, featherPx = if (feather) 2 else 0)
        }.getOrNull()

    fun compose(cut: Bitmap, bg: Int): Bitmap = SmartCutout.compose(cut, bg)
}
