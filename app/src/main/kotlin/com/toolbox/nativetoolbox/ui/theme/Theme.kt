package com.toolbox.nativetoolbox.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AstroLightColorScheme = lightColorScheme(
    primary = AstroBlue,
    onPrimary = Color.White,
    primaryContainer = AstroBlueContainer,
    onPrimaryContainer = AstroBlueDark,

    secondary = AstroPurple,
    onSecondary = Color.White,
    secondaryContainer = AstroPurpleContainer,
    onSecondaryContainer = AstroPurpleDark,

    tertiary = AstroOrange,
    onTertiary = Color.White,
    tertiaryContainer = AstroOrangeContainer,
    onTertiaryContainer = AstroOrangeDark,

    error = AstroRed,
    onError = Color.White,
    errorContainer = AstroRedContainer,
    onErrorContainer = AstroRedDark,

    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,

    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,

    scrim = Scrim
)

private val AstroDarkColorScheme = darkColorScheme(
    primary = AstroBlueLight,
    onPrimary = AstroBlueDark,
    primaryContainer = AstroBlueDark,
    onPrimaryContainer = AstroBlueContainer,

    secondary = AstroPurpleLight,
    onSecondary = AstroPurpleDark,
    secondaryContainer = AstroPurpleDark,
    onSecondaryContainer = AstroPurpleContainer,

    tertiary = AstroOrangeLight,
    onTertiary = AstroOrangeDark,
    tertiaryContainer = AstroOrangeDark,
    onTertiaryContainer = AstroOrangeContainer,

    error = AstroRedLight,
    onError = AstroRedDark,
    errorContainer = AstroRedDark,
    onErrorContainer = AstroRedContainer,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,

    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,

    scrim = Scrim
)

@Composable
fun AstroKitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // 启用 Material You 动态取色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AstroDarkColorScheme
        else -> AstroLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
