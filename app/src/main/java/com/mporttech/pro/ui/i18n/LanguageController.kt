package com.mporttech.pro.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val labelNative: String) {
    INDONESIAN("id", "Indonesia"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INDONESIAN
    }
}

val LocalAppLanguage = staticCompositionLocalOf<MutableState<AppLanguage>> {
    error("LocalAppLanguage not provided")
}

private const val PREFS = "mport_prefs"
private const val KEY_LANG = "app_language"

fun loadSavedLanguage(context: Context): AppLanguage {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    return AppLanguage.fromCode(prefs.getString(KEY_LANG, AppLanguage.INDONESIAN.code))
}

fun saveLanguage(context: Context, language: AppLanguage) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LANG, language.code)
        .apply()
}

@Composable
fun rememberAppLanguageState(initial: AppLanguage): MutableState<AppLanguage> =
    remember { mutableStateOf(initial) }

/** Resolve a bilingual string for the current language. */
@Composable
fun t(id: String): String {
    val lang = LocalAppLanguage.current.value
    return Str.get(id, lang)
}

fun t(id: String, lang: AppLanguage): String = Str.get(id, lang)

/** Non-composable alias for use inside onClick / coroutines. */
fun tr(id: String, lang: AppLanguage): String = Str.get(id, lang)
