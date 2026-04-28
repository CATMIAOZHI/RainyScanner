package com.rainyscanner.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val RainyLightColorScheme = lightColorScheme(
    primary = SakuraPink,
    onPrimary = SakuraOnPrimary,
    primaryContainer = SakuraPinkLight,
    onPrimaryContainer = SakuraPinkDark,
    secondary = SakuraPinkLight,
    onSecondary = SakuraOnBackground,
    secondaryContainer = SakuraBgEnd,
    onSecondaryContainer = SakuraOnSurface,
    tertiary = SuccessGreen,
    onTertiary = SakuraOnPrimary,
    background = SakuraBgStart,
    onBackground = SakuraOnBackground,
    surface = SakuraSurface,
    onSurface = SakuraOnSurface,
    surfaceVariant = SakuraSurfaceVariant,
    onSurfaceVariant = SakuraOnSurfaceVariant,
    error = ErrorRed,
    onError = SakuraOnPrimary,
    outline = SakuraPinkLight
)

private val RainyDarkColorScheme = darkColorScheme(
    primary = SakuraDarkPrimary,
    onPrimary = SakuraOnBackground,
    primaryContainer = SakuraPinkDark,
    onPrimaryContainer = SakuraOnPrimary,
    secondary = SakuraPinkLight,
    onSecondary = SakuraOnBackground,
    secondaryContainer = SakuraDarkSurface,
    onSecondaryContainer = SakuraDarkOnBg,
    tertiary = SuccessGreen,
    onTertiary = SakuraOnBackground,
    background = SakuraDarkBg,
    onBackground = SakuraDarkOnBg,
    surface = SakuraDarkSurface,
    onSurface = SakuraDarkOnBg,
    surfaceVariant = Color(0xFF3D2530),
    onSurfaceVariant = Color(0xFFCDBCC4),
    error = ErrorRed,
    onError = SakuraOnBackground,
    outline = SakuraPinkDark
)

@Composable
fun RainyScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RainyDarkColorScheme else RainyLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RainyTypography,
        content = content
    )
}