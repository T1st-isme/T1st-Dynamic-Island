package com.akira.island.core

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import com.akira.island.data.SettingsRepo
import java.util.Locale

/**
 * App and island language. SYSTEM supports Russian and Vietnamese, otherwise English.
 * Legacy tr(ru, en) labels use the exact-English Vietnamese catalog; dynamic labels
 * supply tr(ru, en, vi) explicitly.
 */
object Lang {
    val code: String
        get() = resolveLanguage(SettingsRepo.current.language.name, systemLanguage())

    /** Compatibility for non-Russian parser/release-note branches, not a locale ID. */
    val en: Boolean
        get() = code != "ru"

    /** Use for dates and user-facing number formatting, never for technical logs. */
    val locale: Locale get() = languageLocale(code)

    /**
     * Vietnamese formatting follows app language, even on an English phone.
     * Keep the existing system-locale number behavior for RU/EN.
     */
    fun format(pattern: String, vararg args: Any): String =
        String.format(if (code == "vi") locale else Locale.getDefault(), pattern, *args)

    private fun systemLanguage(): String =
        runCatching { Resources.getSystem().configuration.locales[0].language }.getOrNull() ?: Locale.getDefault().language
}

/** Source-compatible with existing pairs. Supply vi explicitly for interpolation. */
fun tr(ru: String, en: String, vi: String? = null): String = translate(Lang.code, ru, en, vi)

/**
 * Сменили язык (в настройках или в системе) — всё внутри рисуется заново уже на новом языке.
 * Состояние снаружи (открытая вкладка и экран) остаётся.
 */
@Composable
fun LangScope(content: @Composable () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    // Язык системы меняется вместе с конфигурацией — чтение подписывает на неё
    LocalConfiguration.current
    key(s.language, Lang.code) { content() }
}
