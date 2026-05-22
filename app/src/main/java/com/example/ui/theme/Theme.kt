package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val CustomDarkColorScheme = darkColorScheme(
    primary = TelegramBlue,
    secondary = SecureGreen,
    tertiary = Purple80,
    background = SlateDarkBg,
    surface = SlateSurface,
    surfaceVariant = SlateSurfaceVariant,
    onPrimary = TextPrimary,
    onSecondary = SlateDarkBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderColor
)

private val CustomLightColorScheme = lightColorScheme(
    primary = TelegramBlue,
    secondary = SecureGreenDim,
    tertiary = Purple40,
    background = SlateDarkBg, // Force dark mode as requested by user
    surface = SlateSurface,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme as requested
    dynamicColor: Boolean = false, // Use our handcrafted security aesthetics for premium look
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CustomDarkColorScheme else CustomDarkColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
