package com.akira.island.core

import com.akira.island.data.IslandSettings
import com.akira.island.data.NotificationContent

/**
 * Pure policy shared by ingestion, queued delivery, rendering and actions.
 * Always pass current settings/lock state, not the snapshot that created a notification.
 * User preferences may narrow OS visibility, but never widen it.
 */
object NotificationPrivacy {
    /** Includes an unsecured keyguard and screen-off, not only a PIN/password-protected device. */
    fun isLocked(deviceLocked: Boolean, keyguard: Boolean, screenOn: Boolean): Boolean =
        deviceLocked || keyguard || !screenOn

    fun allowsSource(pkg: String, s: IslandSettings): Boolean =
        s.enabled && s.cardMessages && pkg !in s.blockedNotificationApps

    fun allowsMessage(
        m: AppMessage, s: IslandSettings, locked: Boolean,
        system: MessageSystemPolicy = MessageSystemPolicy(),
    ): Boolean = allowsSource(m.pkg, s) &&
        !(locked && (m.hideOnLock || !system.showOnLock || system.visibilityOverrides[m.key] == MessageVisibility.SECRET))

    /** Non-message events (calls, music, navigation, payment, etc.) are not governed by this policy. */
    fun allowsNotice(
        n: Notice, s: IslandSettings, locked: Boolean,
        system: MessageSystemPolicy = MessageSystemPolicy(),
    ): Boolean =
        if (n.style == NoticeStyle.MESSAGE) {
            n.message?.let { allowsMessage(it, s, locked, system) } == true
        } else true

    fun content(
        m: AppMessage, s: IslandSettings, locked: Boolean,
        system: MessageSystemPolicy = MessageSystemPolicy(),
    ): NotificationContent = when {
        s.notificationContent == NotificationContent.ICON_ONLY -> NotificationContent.ICON_ONLY
        s.notificationContent == NotificationContent.APP_ONLY -> NotificationContent.APP_ONLY
        locked && (s.hideNotificationContentOnLock || m.sensitive ||
            !system.showPrivate && (system.visibilityOverrides[m.key] == MessageVisibility.PRIVATE ||
                system.visibilityOverrides[m.key] == null && m.privateOnLock)) -> NotificationContent.APP_ONLY
        else -> NotificationContent.FULL
    }

    /** Opening the app and expanding history are interactions too; no lockscreen message actions. */
    fun allowsInteraction(
        m: AppMessage, s: IslandSettings, locked: Boolean,
        system: MessageSystemPolicy = MessageSystemPolicy(),
    ): Boolean = allowsMessage(m, s, locked, system) && !locked &&
        content(m, s, locked, system) == NotificationContent.FULL

    fun allowsActions(
        m: AppMessage, s: IslandSettings, locked: Boolean,
        system: MessageSystemPolicy = MessageSystemPolicy(),
    ): Boolean = s.msgActions && allowsInteraction(m, s, locked, system)
}
