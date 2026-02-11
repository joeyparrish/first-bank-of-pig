package io.github.joeyparrish.fbop.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.joeyparrish.fbop.data.model.ThemeMode

// Custom theme values not covered by MaterialTheme
private val LocalWatermarkAlpha = staticCompositionLocalOf { 0.12f }

object AppTheme {
    val watermarkAlpha: Float
        @Composable
        get() = LocalWatermarkAlpha.current
}

private val LightColorScheme = lightColorScheme(
    primary = Pink500,
    onPrimary = Neutral50,
    primaryContainer = Pink100,
    onPrimaryContainer = Pink900,

    secondary = Pink300,
    onSecondary = Neutral800,
    secondaryContainer = Pink100,
    onSecondaryContainer = Pink800,

    tertiary = Pink400,
    onTertiary = Neutral50,
    tertiaryContainer = Pink100,
    onTertiaryContainer = Pink800,

    background = Neutral50,
    onBackground = Neutral900,

    surface = Neutral50,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral600,

    error = Error,
    onError = Neutral50,
    errorContainer = ErrorLight,
    onErrorContainer = Neutral800,

    outline = Neutral300,
    outlineVariant = Neutral200,
)

private val DarkColorScheme = darkColorScheme(
    primary = Pink400,
    onPrimary = Pink900,
    primaryContainer = Neutral800,
    onPrimaryContainer = Pink100,

    secondary = Pink300,
    onSecondary = Pink900,
    secondaryContainer = Pink700,
    onSecondaryContainer = Pink100,

    tertiary = Pink400,
    onTertiary = Pink900,
    tertiaryContainer = Pink700,
    onTertiaryContainer = Pink100,

    background = Neutral900,
    onBackground = Neutral50,

    surface = Neutral900,
    onSurface = Neutral50,
    surfaceVariant = Neutral800,
    onSurfaceVariant = Neutral300,

    error = Error,
    onError = Neutral50,
    errorContainer = Neutral800,
    onErrorContainer = ErrorLight,

    outline = Neutral600,
    outlineVariant = Neutral700,
)

@Composable
fun FirstBankOfPigTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val watermarkAlpha = if (darkTheme) 0.06f else 0.12f

    CompositionLocalProvider(
        LocalWatermarkAlpha provides watermarkAlpha
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
