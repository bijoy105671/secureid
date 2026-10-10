package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CyberBlack,
    primaryContainer = CyberCardBg,
    onPrimaryContainer = NeonCyan,
    secondary = ShieldGreen,
    onSecondary = CyberBlack,
    secondaryContainer = ShieldGreenDark,
    onSecondaryContainer = ShieldGreen,
    tertiary = ElectricIndigo,
    onTertiary = TextPrimary,
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = CyberDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCardBg,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = TextPrimary,
    outline = CyberBorder
)

private val LightColorScheme = lightColorScheme(
    primary = NeonCyanVariant,
    onPrimary = SlateLightBg,
    primaryContainer = SlateLightCard,
    onPrimaryContainer = CyberBlack,
    secondary = ShieldGreen,
    onSecondary = SlateLightBg,
    secondaryContainer = Color(0xFFD7FCE5),
    onSecondaryContainer = Color(0xFF00391A),
    tertiary = ElectricIndigo,
    onTertiary = SlateLightBg,
    background = SlateLightBg,
    onBackground = CyberBlack,
    surface = SlateLightSurface,
    onSurface = CyberBlack,
    surfaceVariant = SlateLightCard,
    onSurfaceVariant = TextMuted,
    error = DangerRed,
    onError = SlateLightBg,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to cyber dark mode for high security aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = colorScheme.background.toArgb()
                it.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
