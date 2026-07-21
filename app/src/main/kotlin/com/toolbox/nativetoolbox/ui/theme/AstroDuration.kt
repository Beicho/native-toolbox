package com.toolbox.nativetoolbox.ui.theme

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Astro Kit 动画时长系统
 * Material Motion 标准时长
 */
object AstroDuration {
    val instant: Duration = 50.milliseconds      // 即时反馈
    val quick: Duration = 100.milliseconds       // 快速切换
    val short: Duration = 200.milliseconds       // 短动画
    val medium: Duration = 300.milliseconds      // 标准动画（默认）
    val long: Duration = 500.milliseconds        // 长动画
    val extraLong: Duration = 700.milliseconds   // 特殊效果
}
