package com.toolbox.nativetoolbox.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * iOS 系统色板(Human Interface Guidelines / UIKit system colors)
 * 内容层用实色,玻璃只出现在悬浮控件层。
 */
@Immutable
data class IosPalette(
    val isDark: Boolean,
    // 强调色
    val accent: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val yellow: Color,
    val purple: Color,
    val teal: Color,
    val indigo: Color,
    val pink: Color,
    val gray: Color,
    // 背景(grouped 风格,同 iOS 设置页)
    val groupedBackground: Color,
    val cardBackground: Color,
    val sunkenBackground: Color,
    // 文字
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    // 分隔线与填充
    val separator: Color,
    val fill: Color,
    // 玻璃容器基色(drawBackdrop onDrawSurface 用)
    val glassSurface: Color
)

val LightIosPalette = IosPalette(
    isDark = false,
    accent = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    orange = Color(0xFFFF9500),
    yellow = Color(0xFFFFCC00),
    purple = Color(0xFFAF52DE),
    teal = Color(0xFF30B0C7),
    indigo = Color(0xFF5856D6),
    pink = Color(0xFFFF2D55),
    gray = Color(0xFF8E8E93),
    groupedBackground = Color(0xFFF2F2F7),
    cardBackground = Color(0xFFFFFFFF),
    sunkenBackground = Color(0xFFE9E9EE),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    tertiaryLabel = Color(0x4D3C3C43),
    separator = Color(0x5B3C3C43),
    fill = Color(0x33787880),
    glassSurface = Color(0xFFFAFAFA).copy(alpha = 0.4f)
)

val DarkIosPalette = IosPalette(
    isDark = true,
    accent = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    orange = Color(0xFFFF9F0A),
    yellow = Color(0xFFFFD60A),
    purple = Color(0xFFBF5AF2),
    teal = Color(0xFF40C8E0),
    indigo = Color(0xFF5E5CE6),
    pink = Color(0xFFFF375F),
    gray = Color(0xFF8E8E93),
    groupedBackground = Color(0xFF000000),
    cardBackground = Color(0xFF1C1C1E),
    sunkenBackground = Color(0xFF2C2C2E),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    tertiaryLabel = Color(0x4DEBEBF5),
    separator = Color(0xA6545458),
    fill = Color(0x5C787880),
    glassSurface = Color(0xFF121212).copy(alpha = 0.4f)
)

val LocalIosPalette = staticCompositionLocalOf { LightIosPalette }
