package com.mporttech.pro.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.mporttech.pro.core.common.Constants

/**
 * App-wide theme mode holder. Toggle from Profile → Dark Mode switch.
 * Survives recomposition via CompositionLocal and process death via SharedPreferences.
 */
val LocalThemeMode = staticCompositionLocalOf<MutableState<ThemeMode>> {
    error("LocalThemeMode not provided — wrap content in CompositionLocalProvider in MainActivity")
}

private const val KEY_THEME = "theme_mode"

fun loadSavedThemeMode(context: Context): ThemeMode {
    val raw = context.getSharedPreferences(Constants.PREFS_APP, Context.MODE_PRIVATE)
        .getString(KEY_THEME, null)
    return ThemeMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: ThemeMode.DARK
}

fun saveThemeMode(context: Context, mode: ThemeMode) {
    context.getSharedPreferences(Constants.PREFS_APP, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_THEME, mode.name)
        .apply()
}

@Composable
fun rememberThemeModeState(
    initial: ThemeMode = ThemeMode.DARK
): MutableState<ThemeMode> = remember { mutableStateOf(initial) }
