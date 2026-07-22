package com.toolbox.nativetoolbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** 外观模式:0 跟随系统 / 1 浅色 / 2 深色 */
enum class ThemeMode { System, Light, Dark }

@Composable
fun AstroKitTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val palette = if (darkTheme) DarkIosPalette else LightIosPalette

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.label,
            secondary = palette.teal,
            background = palette.groupedBackground,
            onBackground = palette.label,
            surface = palette.cardBackground,
            onSurface = palette.label,
            surfaceVariant = palette.sunkenBackground,
            onSurfaceVariant = palette.secondaryLabel,
            outline = palette.separator,
            error = palette.red
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.cardBackground,
            secondary = palette.teal,
            background = palette.groupedBackground,
            onBackground = palette.label,
            surface = palette.cardBackground,
            onSurface = palette.label,
            surfaceVariant = palette.sunkenBackground,
            onSurfaceVariant = palette.secondaryLabel,
            outline = palette.separator,
            error = palette.red
        )
    }

    CompositionLocalProvider(LocalIosPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
