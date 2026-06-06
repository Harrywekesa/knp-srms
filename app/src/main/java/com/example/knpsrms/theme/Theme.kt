package com.example.knpsrms.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ShinyGold,
    secondary = DarkMaroon,
    tertiary = DoveGray,
    background = Color(0xFF1E1416),      // Maroon-tinted dark background
    surface = Color(0xFF2B1D20),         // Maroon-tinted dark surface
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFECEEF1),
    onSurface = Color(0xFFECEEF1),
    surfaceVariant = Color(0xFF3D2C30),
    onSurfaceVariant = Color(0xFFD4AF37)
)

private val LightColorScheme = lightColorScheme(
    primary = DarkMaroon,
    secondary = ShinyGold,
    tertiary = DoveGray,
    background = LightDoveGray,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1E1E24),
    onSurface = Color(0xFF1E1E24),
    surfaceVariant = Color(0xFFECEEF1),
    onSurfaceVariant = Color(0xFF5C1324)
)

@Composable
fun KNPSRMSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled to force our custom Maroon & Gold branding
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
