package com.toolbox.native.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = MorandiBlue,
    onPrimary = Color.White,
    primaryContainer = MorandiBlueLight,
    onPrimaryContainer = MorlandiBlueDark,

    secondary = MorandiPink,
    onSecondary = Color.White,
    secondaryContainer = MorandiPinkLight,
    onSecondaryContainer = MorandiPinkDark,

    tertiary = MorandiPurple,
    onTertiary = Color.White,
    tertiaryContainer = MorandiPurpleLight,
    onTertiaryContainer = MorandiPurpleDark,

    error = MorandiRed,
    onError = Color.White,
    errorContainer = MorandiRedLight,
    onErrorContainer = MorandiRedDark,

    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,

    outline = OutlineLight,
    outlineVariant = DividerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = MorandiBlueLight,
    onPrimary = MorlandiBlueDark,
    primaryContainer = MorlandiBlueDark,
    onPrimaryContainer = MorandiBlueLight,

    secondary = MorandiPinkLight,
    onSecondary = MorandiPinkDark,
    secondaryContainer = MorandiPinkDark,
    onSecondaryContainer = MorandiPinkLight,

    tertiary = MorandiPurpleLight,
    onTertiary = MorandiPurpleDark,
    tertiaryContainer = MorandiPurpleDark,
    onTertiaryContainer = MorandiPurpleLight,

    error = MorandiRedLight,
    onError = MorandiRedDark,
    errorContainer = MorandiRedDark,
    onErrorContainer = MorandiRedLight,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,

    outline = OutlineDark,
    outlineVariant = DividerDark
)

@Composable
fun NativeToolboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 莫兰迪配色不使用动态取色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
