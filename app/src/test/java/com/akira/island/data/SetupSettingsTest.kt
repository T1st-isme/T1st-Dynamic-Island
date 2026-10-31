package com.akira.island.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupSettingsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Test fun `fresh defaults offer setup without requiring optional grants`() {
        val s = IslandSettings()
        assertFalse(s.setupCompleted)
        assertEquals(SetupStep.WELCOME, s.setupStep)
        assertEquals(NotificationContent.FULL, s.notificationContent)
        assertTrue(s.hideNotificationContentOnLock)
        assertTrue(s.blockedNotificationApps.isEmpty())
    }

    @Test fun `legacy saved settings skip first run without resetting user choices`() {
        val s = restoreSettings("""{"enabled":false,"language":"EN","width":137,"cardMessages":false}""", json)
        assertTrue(s.setupCompleted)
        assertFalse(s.enabled)
        assertFalse(s.cardMessages)
        assertEquals(137, s.width)
        assertEquals(AppLanguage.EN, s.language)
        assertTrue(s.hideNotificationContentOnLock)
    }

    @Test fun `explicit unfinished setup and its step survive restart`() {
        val original = IslandSettings(setupStep = SetupStep.NOTIFICATIONS, enabled = false)
        val restored = restoreSettings(json.encodeToString(original), json)
        assertEquals(original, restored)
        assertFalse(restored.setupCompleted)
        assertEquals(SetupStep.NOTIFICATIONS, restored.setupStep)
    }

    @Test fun `finishing or skipping setup does not grant permissions or change feature settings`() {
        val before = IslandSettings(enabled = false, keepAlive = false, cardMessages = false)
        val after = before.copy(setupCompleted = true)
        assertEquals(before, after.copy(setupCompleted = false))
        assertEquals(after, restoreSettings(json.encodeToString(after), json))
    }

    @Test fun `manually reopened setup becomes resumable on an existing installation`() {
        val installed = restoreSettings("""{"language":"EN"}""", json)
        assertTrue(installed.setupCompleted)
        val active = installed.copy(setupCompleted = false, setupStep = SetupStep.WELCOME)
            .copy(setupStep = SetupStep.ACCESSIBILITY)
        val restarted = restoreSettings(json.encodeToString(active), json)
        assertFalse(restarted.setupCompleted)
        assertEquals(SetupStep.ACCESSIBILITY, restarted.setupStep)
        assertEquals(AppLanguage.EN, restarted.language)
    }

    @Test fun `Vietnamese language and privacy choices round trip`() {
        val original = IslandSettings(
            language = AppLanguage.VI,
            notificationContent = NotificationContent.ICON_ONLY,
            hideNotificationContentOnLock = true,
            blockedNotificationApps = setOf("chat.example", "mail.example"),
        )
        assertEquals(original, json.decodeFromString<IslandSettings>(json.encodeToString(original)))
    }

    @Test fun `existing language names and unknown future enum values remain compatible`() {
        listOf(AppLanguage.SYSTEM, AppLanguage.RU, AppLanguage.EN).forEach { language ->
            assertEquals(language, json.decodeFromString<IslandSettings>("""{"language":"${language.name}"}""").language)
        }
        assertEquals(AppLanguage.SYSTEM, json.decodeFromString<IslandSettings>("""{"language":"FUTURE"}""").language)
        assertEquals(NotificationContent.FULL, json.decodeFromString<IslandSettings>("""{"notificationContent":"FUTURE"}""").notificationContent)
    }

    @Test fun `setup navigation has no invalid previous or next step`() {
        assertEquals(null, SetupStep.WELCOME.previous)
        assertEquals(null, SetupStep.TRY_IT.next)
        SetupStep.entries.zipWithNext().forEach { (first, second) ->
            assertEquals(second, first.next)
            assertEquals(first, second.previous)
        }
    }
}
