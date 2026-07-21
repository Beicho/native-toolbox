package com.toolbox.nativetoolbox.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Astro Kit 圆角系统
 * 遵循 Material Design 3 圆角规范
 */
object AstroCorner {
    val none = 0.dp      // 无圆角
    val xs = 4.dp        // 极小圆角（小组件）
    val sm = 8.dp        // 小圆角（按钮）
    val md = 12.dp       // 标准圆角（卡片）
    val lg = 16.dp       // 大圆角（大卡片）
    val xl = 20.dp       // 超大圆角（对话框）
    val xxl = 24.dp      // 特大圆角
    val full = 999.dp    // 完全圆角（Chip/Pill）
}
