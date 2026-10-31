package com.akira.island.core

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM policy tests: no Android resources, SettingsRepo or Compose runtime. */
class LanguageTest {
    @Test
    fun explicitLanguagesOverrideTheSystemWithoutRenamingSavedValues() {
        assertEquals("ru", resolveLanguage("RU", "vi"))
        assertEquals("en", resolveLanguage("EN", "ru"))
        assertEquals("vi", resolveLanguage("VI", "en"))
        assertEquals("en", resolveLanguage("unknown future setting", "vi"))
    }

    @Test
    fun systemVietnameseAndRussianAreSelectedAutomatically() {
        for (tag in listOf("vi", "vi-VN", "vi_VN", "VI-vn")) {
            assertEquals(tag, "vi", resolveLanguage("SYSTEM", tag))
        }
        assertEquals("ru", resolveLanguage("SYSTEM", "ru-RU"))
        for (tag in listOf("en", "de", "fr", "", "invalid")) {
            assertEquals(tag, "en", resolveLanguage("SYSTEM", tag))
        }
        // Both EN and VI were previously Lang.en=true: scope identity must differ.
        assertFalse(resolveLanguage("SYSTEM", "en") == resolveLanguage("SYSTEM", "vi"))
    }

    @Test
    fun russianAndEnglishKeepTheirOriginalStrings() {
        for ((en, _) in VietnameseStrings.byEnglish) {
            assertEquals("Русская строка", translate("ru", "Русская строка", en, "Tiếng Việt"))
            assertEquals(en, translate("en", "Русская строка", en, "Tiếng Việt"))
        }
        assertEquals("English", translate("unsupported", "Русский", "English", "Tiếng Việt"))
    }

    @Test
    fun legacyVietnameseUsesExactKeysAndFallsBackToEnglish() {
        assertEquals("Đang sạc", translate("vi", "Зарядка", "Charging"))
        assertEquals("Trợ năng", translate("vi", "Спец. возможности", "Accessibility"))
        for (missing in listOf("charging", " Charging", "Charging ", "Charging 41%", "Not in catalog")) {
            assertEquals(missing, translate("vi", "Русский", missing))
        }
        // Copied technical diagnostics deliberately do not go through translation.
        assertEquals("title: ", translate("vi", "заголовок: ", "title: "))
        assertEquals("No amount — not a payment",
            translate("vi", "Без суммы — не оплата", "No amount — not a payment"))
    }

    @Test
    fun explicitVietnameseWinsAndPreservesInterpolatedContent() {
        val app = "Ứng dụng \"Anna\" / Telegram"
        val count = 17
        val vi = "Có $count tin nhắn từ $app"
        assertEquals(vi, translate("vi", "$app: $count", "$count messages from $app", vi))
        assertEquals("Tùy chọn khác", translate("vi", "Таймер", "Timer", "Tùy chọn khác"))
        assertEquals("", translate("vi", "Таймер", "Timer", ""))
        assertEquals("Anna", translate("vi", "Анна", "Anna"))
        assertEquals("Dynamic Island", translate("vi", "Динамический остров", "Dynamic Island"))
    }

    @Test
    fun existingSettingsEnumLabelsAndHintsAreCovered() {
        val labels = listOf(
            "30 fps — saves power", "60 fps — smooth", "Screen rate — up to 120",
            "Like a Fruit", "Spring", "Smooth", "No animation",
            "No bounce", "Light bounce", "Medium bounce", "Strong bounce",
            "Expand the capsule", "Open the app", "Play / pause", "Next track",
            "Screenshot without the island", "Nothing",
            "Timer", "Stopwatch", "Flashlight", "Screenshot", "Camera", "Lock the screen", "Lock",
            "Lavender", "The app's color", "System",
            "From your Android 12+ wallpaper (Material You)", "From the avatar",
            "Matched to the avatar or app icon", "Custom", "Any color from the palette",
            "System default",
        )
        labels.forEach { assertTrue("Missing enum label: $it", VietnameseStrings.byEnglish.containsKey(it)) }
    }

    @Test
    fun phaseOneServiceStatusAndPermissionLabelsAreCovered() {
        val labels = listOf(
            "Service diagnostics", "Island service connected", "Island windows attached",
            "Notification listener connected", "Keep-alive service running", "Additional checks",
            "Exact alarms", "Battery optimization exemption", "App's own notifications",
            "Service connected; island windows attached", "Copy diagnostics",
            "phone state", "caller number", "contact name", "answer and decline",
        )
        labels.forEach { assertTrue("Missing diagnostic UI: $it", VietnameseStrings.byEnglish.containsKey(it)) }
        assertTrue(VietnameseStrings.byEnglish.values.all { it.isNotBlank() })
    }

    @Test
    fun vietnameseLocaleFormatsDatesAndDecimalNumbers() {
        val locale = languageLocale("vi")
        assertEquals("vi", locale.language)
        assertEquals("VN", locale.country)
        assertEquals(Locale.ENGLISH, languageLocale("en"))
        assertEquals("ru", languageLocale("ru").language)
        assertEquals("1.234,5", NumberFormat.getNumberInstance(locale).format(1234.5))
        assertEquals("1,5 giây", String.format(locale, translate("vi", "%.1f с", "%.1f s"), 1.5))
        val utc = TimeZone.getTimeZone("UTC")
        val date = Calendar.getInstance(utc).apply {
            clear()
            set(2026, Calendar.JANUARY, 2, 12, 0)
        }.time
        val formatted = SimpleDateFormat("EEEE, d MMMM", locale).apply { timeZone = utc }.format(date)
        assertTrue(formatted, formatted.lowercase(locale).startsWith("thứ"))
        assertTrue(formatted, formatted.contains("tháng 1"))
    }
}
