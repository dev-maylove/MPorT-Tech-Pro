package com.mporttech.pro.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-wide theme mode holder. Toggle from Profile → Dark Mode switch.
 * Survives recomposition within the activity via CompositionLocal.
 */
val LocalThemeMode = staticCompositionLocalOf<MutableState<ThemeMode>> {
    error("LocalThemeMode not provided")
}

@Composable
fun rememberThemeModeState(
    initial: ThemeMode = ThemeMode.DARK
): MutableState<ThemeMode> = remember { mutableStateOf(initial) }
