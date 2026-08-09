package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultDarkColorScheme = darkColorScheme(
    primary = LavenderAccent,
    onPrimary = Color(0xFF121212),
    primaryContainer = DeepPurpleContainer,
    onPrimaryContainer = Color.White,
    secondary = SoftIndigoAccent,
    onSecondary = Color(0xFF121212),
    tertiary = EmeraldVerified,
    onTertiary = Color(0xFF121212),
    background = ObsidianBackground,
    onBackground = Color.White,
    surface = CharcoalSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFE0E0E0),
    error = RoseError,
    onError = Color.White
)

private val VaultLightColorScheme = darkColorScheme(
    primary = LavenderAccent,
    onPrimary = Color(0xFF121212),
    primaryContainer = DeepPurpleContainer,
    onPrimaryContainer = Color.White,
    secondary = SoftIndigoAccent,
    onSecondary = Color(0xFF121212),
    tertiary = EmeraldVerified,
    onTertiary = Color(0xFF121212),
    background = ObsidianBackground,
    onBackground = Color.White,
    surface = CharcoalSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFE0E0E0),
    error = RoseError,
    onError = Color.White
)

@Composable
fun VaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = VaultDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
