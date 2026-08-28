package com.ioniq.companion.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val HyundaiDarkColorScheme = darkColorScheme(
    primary = IoniqCyan,
    onPrimary = TextInverse,
    primaryContainer = SurfaceCardElevated,
    onPrimaryContainer = TextPrimary,
    secondary = HyundaiBlue,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceCardSecondary,
    onSecondaryContainer = TextSecondary,
    tertiary = EcoGreen,
    onTertiary = TextInverse,
    background = CockpitBg,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardSecondary,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = CardBorderSubtle
)

@Composable
fun IoniqCompanionTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = HyundaiDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CockpitBg.toArgb()
            window.navigationBarColor = CockpitBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
