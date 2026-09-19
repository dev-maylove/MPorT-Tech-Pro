package com.mporttech.pro.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.mporttech.pro.core.common.Constants

enum class AppLanguage(val code: String, val labelNative: String) {
    INDONESIAN("id", "Indonesia"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INDONESIAN
    }
}

/**
 * Must be provided from Activity setContent via CompositionLocalProvider.
 * Calling [LocalAppLanguage.current] outside @Composable causes a compile error.
 */
val LocalAppLanguage = staticCompositionLocalOf<MutableState<AppLanguage>> {
    error("LocalAppLanguage not provided — wrap content in CompositionLocalProvider in MainActivity")
}

private const val KEY_LANG = "app_language"
/** Legacy prefs name used before Constants.PREFS_LANG. */
private const val LEGACY_PREFS = "mport_prefs"

fun loadSavedLanguage(context: Context): AppLanguage {
    val canonical = context.getSharedPreferences(Constants.PREFS_LANG, Context.MODE_PRIVATE)
    var code = canonical.getString(KEY_LANG, null)
    if (code == null) {
        val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        code = legacy.getString(KEY_LANG, null)
        if (code != null) {
            canonical.edit().putString(KEY_LANG, code).apply()
        }
    }
    return AppLanguage.fromCode(code ?: AppLanguage.INDONESIAN.code)
}

fun saveLanguage(context: Context, language: AppLanguage) {
    context.getSharedPreferences(Constants.PREFS_LANG, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LANG, language.code)
        .apply()
}

@Composable
fun rememberAppLanguageState(initial: AppLanguage): MutableState<AppLanguage> =
    remember { mutableStateOf(initial) }

/**
 * Resolve bilingual string for the **current composition language**.
 *
 * ⚠️ **@Composable only** — do NOT call from:
 * - onClick / onCheckedChange / callbacks
 * - viewModelScope / rememberCoroutineScope launches
 * - Toast.makeText arguments evaluated in those callbacks
 *
 * For those contexts use [tCtx] or [tr] / [Str.get] instead.
 */
@Composable
@ReadOnlyComposable
fun t(id: String): String {
    val lang = LocalAppLanguage.current.value
    return Str.get(id, lang)
}

/** Non-composable: explicit language. */
fun t(id: String, lang: AppLanguage): String = Str.get(id, lang)

/** Non-composable alias (onClick / coroutines). */
fun tr(id: String, lang: AppLanguage): String = Str.get(id, lang)

/**
 * Non-composable: resolve string from Context prefs (safe in onClick, Toast, coroutines).
 * Prefer this over [t] whenever you are outside composition.
 */
fun tCtx(context: Context, id: String): String =
    Str.get(id, loadSavedLanguage(context))
