package com.kaleidofin.originator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandTeal,
    onPrimary = Color.White,
    primaryContainer = BrandNavy,
    secondary = BrandTealDark,
    onSecondary = Color.White,
    background = BrandNavy,
    surface = BrandNavy,
    onSurface = SurfaceMuted,
    tertiary = SuccessGreen,
    outline = BorderLight
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandTeal,
    onPrimaryContainer = BrandNavy,
    secondary = BrandTealDark,
    onSecondary = Color.White,
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    background = Color.White,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    outline = BorderLight
)

@Composable
fun OriginatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}