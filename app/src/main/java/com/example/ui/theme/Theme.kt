package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultDarkColorScheme = darkColorScheme(
    primary = ElectricCyanBright,
    onPrimary = Color.Black,
    primaryContainer = DeepPurpleContainer,
    onPrimaryContainer = Color.White,
    secondary = SoftIndigoAccent,
    onSecondary = Color.Black,
    tertiary = EmeraldVerified,
    onTertiary = Color.Black,
    background = ObsidianBackground,
    onBackground = Color.White,
    surface = CharcoalSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = RoseError,
    onError = Color.Black
)

private val VaultLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7), // Vibrant Sapphire Blue for Light Mode
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF06B6D4), // Electric Cyan
    onSecondary = Color.White,
    tertiary = EmeraldVerified,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF64748B),
    error = RoseError,
    onError = Color.White
)

@Composable
fun VaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VaultDarkColorScheme else VaultLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
