package com.mporttech.pro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val MPorTCyan = Color(0xFF00D9FF)
val MPorTBlue = Color(0xFF21B6FF)
val MPorTBlueDeep = Color(0xFF2563EB)
val MPorTPurple = Color(0xFF7C3AED)
val MPorTGreen = Color(0xFF35E381)
val MPorTAmber = Color(0xFFFFB020)
val MPorTRed = Color(0xFFFF5E67)
val MPorTHeart = Color(0xFFFF4D6D)

private val DarkColorScheme = darkColorScheme(
    primary = MPorTBlue,
    onPrimary = Color(0xFF001A2B),
    primaryContainer = Color(0xFF063D63),
    onPrimaryContainer = Color(0xFFB8E6FF),
    secondary = Color(0xFF66E6FF),
    onSecondary = Color(0xFF003544),
    secondaryContainer = Color(0xFF0D2A45),
    onSecondaryContainer = Color(0xFFB8DFFF),
    tertiary = MPorTPurple,
    onTertiary = Color.White,
    background = Color(0xFF050914),
    onBackground = Color(0xFFF3F7FC),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFF3F7FC),
    surfaceVariant = Color(0xFF111C2E),
    onSurfaceVariant = Color(0xFF9DB0C7),
    outline = Color(0xFF233956),
    outlineVariant = Color(0xFF1A2A42),
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF3B0010),
    errorContainer = Color(0xFF5C001A),
    onErrorContainer = Color(0xFFFFDAD9),
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = Color(0xFF00639A),
    scrim = Color(0xCC000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0077B6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCAE9FF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = Color(0xFF0E7490),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF4FC),
    onSecondaryContainer = Color(0xFF003641),
    tertiary = Color(0xFF6D28D9),
    onTertiary = Color.White,
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8EEF6),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = Color(0xFF7DD3FC),
    scrim = Color(0x99000000)
)

data class MPorTExtendedColors(
    val cyan: Color,
    val blue: Color,
    val purple: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val heart: Color,
    val cardBorder: Color,
    val elevatedSurface: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val isDark: Boolean
)

private val DarkExtended = MPorTExtendedColors(
    cyan = MPorTCyan,
    blue = MPorTBlue,
    purple = MPorTPurple,
    success = MPorTGreen,
    warning = MPorTAmber,
    danger = MPorTRed,
    heart = MPorTHeart,
    cardBorder = Color(0xFF1F2937),
    elevatedSurface = Color(0xFF0E131D),
    heroGradientStart = Color(0xFF020617),
    heroGradientEnd = Color(0xFF09090B),
    isDark = true
)

private val LightExtended = MPorTExtendedColors(
    cyan = Color(0xFF0891B2),
    blue = Color(0xFF0284C7),
    purple = Color(0xFF7C3AED),
    success = Color(0xFF16A34A),
    warning = Color(0xFFD97706),
    danger = Color(0xFFDC2626),
    heart = Color(0xFFE11D48),
    cardBorder = Color(0xFFE2E8F0),
    elevatedSurface = Color(0xFFFFFFFF),
    heroGradientStart = Color(0xFFF0F9FF),
    heroGradientEnd = Color(0xFFF8FAFC),
    isDark = false
)

val LocalMPorTColors = staticCompositionLocalOf { DarkExtended }

enum class ThemeMode { SYSTEM, DARK, LIGHT }

object MPorTTheme {
    val colors: MPorTExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMPorTColors.current
}

@Composable
fun MPorTTechTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extended = if (darkTheme) DarkExtended else LightExtended

    CompositionLocalProvider(LocalMPorTColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}
