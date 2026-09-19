package com.mporttech.pro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Neon palette ──────────────────────────────────────────────
val MPorTCyan = Color(0xFF00F0FF)          // neon cyan
val MPorTBlue = Color(0xFF3B9EFF)          // neon blue
val MPorTBlueDeep = Color(0xFF1E6FFF)
val MPorTPurple = Color(0xFFB14DFF)        // neon purple
val MPorTGreen = Color(0xFF39FF14)         // neon green
val MPorTAmber = Color(0xFFFFD60A)         // neon amber
val MPorTRed = Color(0xFFFF2E63)           // neon red/pink
val MPorTHeart = Color(0xFFFF4D9A)
val MPorTNeonWhite = Color(0xFFE8FBFF)     // cool neon white
val MPorTNeonSoft = Color(0xFF9EE8FF)      // soft neon for secondary text
val MPorTNeonDim = Color(0xFF5EC8E8)       // dim neon for tertiary

private val DarkColorScheme = darkColorScheme(
    primary = MPorTCyan,
    onPrimary = Color(0xFF000000),  // pure black on neon cyan buttons
    primaryContainer = Color(0xFF003D4D),
    onPrimaryContainer = MPorTNeonWhite,
    secondary = MPorTBlue,
    onSecondary = Color(0xFF001528),
    secondaryContainer = Color(0xFF0A2540),
    onSecondaryContainer = MPorTNeonSoft,
    tertiary = MPorTPurple,
    onTertiary = Color.White,
    background = Color(0xFF03060F),
    onBackground = MPorTNeonWhite,           // neon text
    surface = Color(0xFF0A1020),
    onSurface = MPorTNeonWhite,              // neon text
    surfaceVariant = Color(0xFF101A2E),
    onSurfaceVariant = MPorTNeonSoft,        // neon secondary text
    outline = Color(0xFF1A4A60),
    outlineVariant = Color(0xFF123040),
    error = MPorTRed,
    onError = Color(0xFF2A0010),
    errorContainer = Color(0xFF5C001A),
    onErrorContainer = Color(0xFFFFB0C0),
    inverseSurface = MPorTNeonWhite,
    inverseOnSurface = Color(0xFF0A1020),
    inversePrimary = Color(0xFF0090A8),
    scrim = Color(0xCC000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0090A8),
    onPrimary = Color(0xFF000000),            // black label on cyan/teal buttons
    primaryContainer = Color(0xFFB8F4FF),
    onPrimaryContainer = Color(0xFF002A33),
    secondary = Color(0xFF0077CC),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0EBFF),
    onSecondaryContainer = Color(0xFF002040),
    tertiary = Color(0xFF8B2CE8),
    onTertiary = Color.White,
    background = Color(0xFFF0F4F8),
    onBackground = Color(0xFF0A1628),         // near-black — readable on light bg
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A1628),            // near-black card text
    surfaceVariant = Color(0xFFE8EEF4),
    onSurfaceVariant = Color(0xFF3A5568),     // muted but readable
    outline = Color(0xFF8AA0B0),
    outlineVariant = Color(0xFFD0D8E0),
    error = Color(0xFFD00040),
    onError = Color.White,
    errorContainer = Color(0xFFFFD0DC),
    onErrorContainer = Color(0xFF5C001A),
    inverseSurface = Color(0xFF0A1020),
    inverseOnSurface = MPorTNeonWhite,
    inversePrimary = MPorTCyan,
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
    val neonText: Color,
    val neonTextSoft: Color,
    val neonTextDim: Color,
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
    cardBorder = Color(0xFF1A3A50),
    elevatedSurface = Color(0xFF0C1528),
    heroGradientStart = Color(0xFF020617),
    heroGradientEnd = Color(0xFF050A18),
    neonText = MPorTNeonWhite,
    neonTextSoft = MPorTNeonSoft,
    neonTextDim = MPorTNeonDim,
    isDark = true
)

private val LightExtended = MPorTExtendedColors(
    cyan = Color(0xFF00B8D4),
    blue = Color(0xFF0284C7),
    purple = Color(0xFF8B2CE8),
    success = Color(0xFF00C853),
    warning = Color(0xFFFFAB00),
    danger = Color(0xFFE91E63),
    heart = Color(0xFFFF4081),
    cardBorder = Color(0xFFB0E0F0),
    elevatedSurface = Color(0xFFFFFFFF),
    heroGradientStart = Color(0xFFE8F9FF),
    heroGradientEnd = Color(0xFFF5FCFF),
    neonText = Color(0xFF003848),
    neonTextSoft = Color(0xFF0A5A70),
    neonTextDim = Color(0xFF3A8A9E),
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

/** Neon-forward typography — titles use cyan glow-ready white */
private val NeonTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun MPorTTechTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val colors = if (darkTheme) DarkExtended else LightExtended
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalMPorTColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NeonTypography,
            content = content
        )
    }
}


/** Primary action button: neon cyan fill + black label (always readable). */
@Composable
fun mportPrimaryButtonColors() = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )
