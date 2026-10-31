package com.akira.island.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Color
import com.akira.island.data.IslandSettings
import com.akira.island.data.NotificationContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPrivacyTest {
    private val defaults = IslandSettings()
    private val message = AppMessage(
        key = "message", pkg = "messenger", appName = "Messenger", text = "Private text",
        avatar = null, appIcon = null, intent = null, history = listOf("Older text", "Private text"),
    )

    private fun notice(m: AppMessage = message) = Notice(
        title = "Private sender", icon = Icons.Default.Info, tint = Color.White,
        style = NoticeStyle.MESSAGE, message = m,
    )

    @Test fun `effective message lock covers secure unsecured and sleeping lock states`() {
        listOf(false, true).forEach { deviceLocked ->
            listOf(false, true).forEach { keyguard ->
                listOf(false, true).forEach { screenOn ->
                    val locked = NotificationPrivacy.isLocked(deviceLocked, keyguard, screenOn)
                    assertEquals(deviceLocked || keyguard || !screenOn, locked)
                    assertEquals(
                        if (locked) NotificationContent.APP_ONLY else NotificationContent.FULL,
                        NotificationPrivacy.content(message, defaults, locked),
                    )
                    assertEquals(!locked, NotificationPrivacy.allowsActions(message, defaults, locked))
                }
            }
        }
    }

    @Test fun `content matrix never widens user or system restrictions`() {
        NotificationContent.entries.forEach { requested ->
            listOf(false, true).forEach { locked ->
                listOf(false, true).forEach { sensitive ->
                    listOf(false, true).forEach { hideOnLock ->
                        val s = defaults.copy(
                            notificationContent = requested,
                            hideNotificationContentOnLock = hideOnLock,
                        )
                        val expected = when {
                            requested == NotificationContent.ICON_ONLY -> NotificationContent.ICON_ONLY
                            requested == NotificationContent.APP_ONLY -> NotificationContent.APP_ONLY
                            locked && (sensitive || hideOnLock) -> NotificationContent.APP_ONLY
                            else -> NotificationContent.FULL
                        }
                        assertEquals(
                            "mode=$requested locked=$locked sensitive=$sensitive hideOnLock=$hideOnLock",
                            expected,
                            NotificationPrivacy.content(message.copy(sensitive = sensitive), s, locked),
                        )
                    }
                }
            }
        }
    }

    @Test fun `defaults redact all locked messages but restore full content on unlock`() {
        assertEquals(NotificationContent.FULL, defaults.notificationContent)
        assertTrue(defaults.hideNotificationContentOnLock)
        assertEquals(NotificationContent.APP_ONLY, NotificationPrivacy.content(message, defaults, true))
        assertEquals(NotificationContent.FULL, NotificationPrivacy.content(message, defaults, false))
    }

    @Test fun `turning off lock redaction cannot override system sensitive`() {
        val s = defaults.copy(hideNotificationContentOnLock = false)
        val privateMessage = message.copy(sensitive = true)
        assertEquals(NotificationContent.APP_ONLY, NotificationPrivacy.content(privateMessage, s, true))
        assertEquals(NotificationContent.FULL, NotificationPrivacy.content(privateMessage, s, false))
        assertEquals(NotificationContent.FULL, NotificationPrivacy.content(message, s, true))
        // Even visible full content on a lockscreen never grants actions or history expansion.
        assertFalse(NotificationPrivacy.allowsInteraction(message, s, true))
        assertFalse(NotificationPrivacy.allowsActions(message, s, true))
    }

    @Test fun `blocking and disabling are checked by ingestion and delivery`() {
        val n = notice()
        val denied = listOf(
            defaults.copy(blockedNotificationApps = setOf(message.pkg)),
            defaults.copy(cardMessages = false),
            defaults.copy(enabled = false),
        )
        denied.forEach { current ->
            assertFalse(NotificationPrivacy.allowsSource(message.pkg, current))
            listOf(false, true).forEach { locked ->
                assertFalse(NotificationPrivacy.allowsMessage(message, current, locked))
                assertFalse(NotificationPrivacy.allowsNotice(n, current, locked))
                assertFalse(NotificationPrivacy.allowsInteraction(message, current, locked))
                assertFalse(NotificationPrivacy.allowsActions(message, current, locked))
            }
        }
        assertTrue(NotificationPrivacy.allowsSource(message.pkg, defaults.copy(blockedNotificationApps = setOf("other"))))
    }

    @Test fun `queued notice uses latest settings not ingestion snapshot`() {
        val queued = notice()
        assertTrue(NotificationPrivacy.allowsNotice(queued, defaults, false))
        val blocked = defaults.copy(blockedNotificationApps = setOf(message.pkg))
        assertFalse(NotificationPrivacy.allowsNotice(queued, blocked, false))
        assertFalse(NotificationPrivacy.allowsNotice(queued, defaults.copy(cardMessages = false), false))
        assertFalse(NotificationPrivacy.allowsNotice(queued, defaults.copy(enabled = false), false))
        // A reduced detail setting does not discard a banner; its render policy is reevaluated instead.
        val iconOnly = defaults.copy(notificationContent = NotificationContent.ICON_ONLY)
        assertTrue(NotificationPrivacy.allowsNotice(queued, iconOnly, false))
        assertEquals(NotificationContent.ICON_ONLY, NotificationPrivacy.content(message, iconOnly, false))
        assertFalse(NotificationPrivacy.allowsActions(message, iconOnly, false))
    }

    @Test fun `OS hidden notification captured unlocked stays ineligible after lock`() {
        // Both VISIBILITY_SECRET and OS lockscreen notifications disabled produce this metadata.
        val n = notice(message.copy(hideOnLock = true, sensitive = true))
        val s = defaults.copy(hideNotificationContentOnLock = false)
        assertTrue(NotificationPrivacy.allowsNotice(n, s, false))
        assertFalse(NotificationPrivacy.allowsNotice(n, s, true))
        assertFalse(NotificationPrivacy.allowsActions(n.message!!, s, true))
        assertTrue(NotificationPrivacy.allowsNotice(n, s, false))
    }

    @Test fun `new Android lockscreen restriction suppresses a notice captured under old policy`() {
        val queued = notice()
        val s = defaults.copy(hideNotificationContentOnLock = false)
        assertTrue(NotificationPrivacy.allowsNotice(queued, s, false, MessageSystemPolicy()))
        val current = MessageSystemPolicy(showOnLock = false)
        NotificationContent.entries.forEach { mode ->
            assertFalse(NotificationPrivacy.allowsNotice(queued, s.copy(notificationContent = mode), true, current))
        }
        assertTrue(NotificationPrivacy.allowsNotice(queued, s, false, current))
    }

    @Test fun `current private-content restriction redacts previously public-to-lockscreen payload`() {
        val privateMessage = message.copy(privateOnLock = true)
        val s = defaults.copy(hideNotificationContentOnLock = false)
        assertEquals(NotificationContent.FULL, NotificationPrivacy.content(privateMessage, s, true, MessageSystemPolicy()))
        val current = MessageSystemPolicy(showPrivate = false)
        assertEquals(NotificationContent.APP_ONLY, NotificationPrivacy.content(privateMessage, s, true, current))
        assertEquals(NotificationContent.FULL, NotificationPrivacy.content(message, s, true, current))
        assertEquals(NotificationContent.FULL, NotificationPrivacy.content(privateMessage, s, false, current))
    }

    @Test fun `current ranking restrictions apply to already queued messages`() {
        val s = defaults.copy(hideNotificationContentOnLock = false)
        val secret = MessageSystemPolicy(visibilityOverrides = mapOf(message.key to MessageVisibility.SECRET))
        assertFalse(NotificationPrivacy.allowsNotice(notice(), s, true, secret))
        val private = MessageSystemPolicy(showPrivate = false, visibilityOverrides = mapOf(message.key to MessageVisibility.PRIVATE))
        assertEquals(NotificationContent.APP_ONLY, NotificationPrivacy.content(message, s, true, private))
        val public = private.copy(visibilityOverrides = mapOf(message.key to MessageVisibility.PUBLIC))
        assertEquals(NotificationContent.FULL, NotificationPrivacy.content(message.copy(privateOnLock = true), s, true, public))
    }

    @Test fun `current system policy never relaxes stored or user privacy restrictions`() {
        val current = MessageSystemPolicy(visibilityOverrides = mapOf(message.key to MessageVisibility.PUBLIC))
        val s = defaults.copy(hideNotificationContentOnLock = false)
        assertEquals(NotificationContent.APP_ONLY, NotificationPrivacy.content(message.copy(sensitive = true), s, true, current))
        assertFalse(NotificationPrivacy.allowsNotice(notice(message.copy(hideOnLock = true)), s, true, current))
        assertEquals(NotificationContent.ICON_ONLY,
            NotificationPrivacy.content(message, s.copy(notificationContent = NotificationContent.ICON_ONLY), true, current))
    }

    @Test fun `action matrix requires unlocked full content enabled buttons and eligible source`() {
        NotificationContent.entries.forEach { mode ->
            listOf(false, true).forEach { locked ->
                listOf(false, true).forEach { buttons ->
                    val s = defaults.copy(notificationContent = mode, msgActions = buttons)
                    val interaction = mode == NotificationContent.FULL && !locked
                    assertEquals(interaction, NotificationPrivacy.allowsInteraction(message, s, locked))
                    assertEquals(interaction && buttons, NotificationPrivacy.allowsActions(message, s, locked))
                }
            }
        }
    }

    @Test fun `message privacy does not filter non-message activity banners`() {
        val restrictive = defaults.copy(
            enabled = false, cardMessages = false, notificationContent = NotificationContent.ICON_ONLY,
            blockedNotificationApps = setOf(message.pkg),
        )
        NoticeStyle.entries.filter { it != NoticeStyle.MESSAGE }.forEach { style ->
            val activity = notice(message.copy(hideOnLock = true)).copy(style = style)
            assertTrue("$style", NotificationPrivacy.allowsNotice(activity, restrictive, true))
        }
        // A malformed MESSAGE cannot fall through into a generic banner exposing its title.
        assertFalse(NotificationPrivacy.allowsNotice(notice().copy(message = null), defaults, false))
    }
}
