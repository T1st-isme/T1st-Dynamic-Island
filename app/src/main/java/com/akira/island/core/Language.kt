package com.akira.island.core

import java.util.Locale

/**
 * Pure language policy shared by settings and the island. Saved enum names stay
 * unchanged; unsupported system languages intentionally use English.
 */
internal fun resolveLanguage(setting: String, systemLanguage: String): String = when (setting) {
    "RU" -> "ru"
    "EN" -> "en"
    "VI" -> "vi"
    "SYSTEM" -> when (systemLanguage.replace('_', '-').substringBefore('-').lowercase(Locale.ROOT)) {
        "ru" -> "ru"
        "vi" -> "vi"
        else -> "en"
    }
    else -> "en"
}

internal fun languageLocale(code: String): Locale = when (code) {
    "ru" -> Locale.forLanguageTag("ru")
    "vi" -> Locale.forLanguageTag("vi-VN")
    else -> Locale.ENGLISH
}

/**
 * Only app-owned labels belong here. Never pass notification text, contact names,
 * song titles or app labels through the catalog. Interpolation needs explicit vi.
 */
internal fun translate(code: String, ru: String, en: String, vi: String? = null): String =
    when (code) {
        "ru" -> ru
        "vi" -> vi ?: VietnameseStrings.byEnglish[en] ?: en
        else -> en
    }
