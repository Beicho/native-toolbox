package com.toolbox.nativetoolbox.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 对应 iOS 字阶:LargeTitle 34 / Title1 28 / Title2 22 / Title3 20 /
 * Headline 17 semibold / Body 17 / Callout 16 / Subheadline 15 / Footnote 13 / Caption 12·11
 */
val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.37.sp),
    displayMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.36.sp),
    displaySmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.35.sp),
    headlineLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.41).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.41).sp),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.24).sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.41).sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.24).sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.08).sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.32).sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.07.sp)
)

/** 等宽,用于代码/哈希/时间戳输出 */
val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    lineHeight = 19.sp
)
