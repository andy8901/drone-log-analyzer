package com.neosky.servicesupport.presentation.theme

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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Blue30,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal30,
    tertiary = Amber40,
    onTertiary = Color.White,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Blue10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red30,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
)

private val DarkColors = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue40,
    onPrimaryContainer = Blue90,
    secondary = Teal80,
    onSecondary = Teal30,
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,
    tertiary = Amber80,
    onTertiary = Amber40,
    tertiaryContainer = Amber40,
    onTertiaryContainer = Amber90,
    error = Red80,
    onError = Red30,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
)

/** Semantic status colors, resolved per-theme. Consumed only via [com.neosky.servicesupport.core.util.StatusColorMapper]. */
data class StatusColorPalette(
    val positive: Color,
    val info: Color,
    val warning: Color,
    val negative: Color,
    val neutral: Color,
)

private val LightStatusColors = StatusColorPalette(
    positive = StatusPositiveLight,
    info = StatusInfoLight,
    warning = StatusWarningLight,
    negative = StatusNegativeLight,
    neutral = StatusNeutralLight,
)

private val DarkStatusColors = StatusColorPalette(
    positive = StatusPositiveDark,
    info = StatusInfoDark,
    warning = StatusWarningDark,
    negative = StatusNegativeDark,
    neutral = StatusNeutralDark,
)

private val LocalStatusColorPalette = staticCompositionLocalOf { LightStatusColors }

/** Convenience accessor used by StatusColorMapper: `StatusColors.positive` etc. */
object StatusColors {
    val positive: Color @Composable get() = LocalStatusColorPalette.current.positive
    val info: Color @Composable get() = LocalStatusColorPalette.current.info
    val warning: Color @Composable get() = LocalStatusColorPalette.current.warning
    val negative: Color @Composable get() = LocalStatusColorPalette.current.negative
    val neutral: Color @Composable get() = LocalStatusColorPalette.current.neutral
}

@Composable
fun NeoSkyServiceSupportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+; disabled by default so the aviation-professional
    // brand palette is consistent across devices, but left wired up for a future toggle.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalStatusColorPalette provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NeoSkyTypography,
            content = content,
        )
    }
}
