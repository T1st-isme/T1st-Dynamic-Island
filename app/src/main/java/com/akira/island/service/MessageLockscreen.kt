package com.akira.island.service

import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.akira.island.core.IslandBus
import com.akira.island.core.MessageSystemPolicy
import com.akira.island.core.MessageVisibility

/** Process-lifetime observers use application context only; no message text or service is retained. */
object MessageLockscreen {
    private var observer: ContentObserver? = null

    fun start(context: Context) {
        if (observer != null) return
        val app = context.applicationContext
        refresh(app)
        val changes = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh(app)
        }
        observer = changes
        listOf("lock_screen_show_notifications", "lock_screen_allow_private_notifications").forEach { name ->
            runCatching {
                app.contentResolver.registerContentObserver(Settings.Secure.getUriFor(name), false, changes)
            }
        }
    }

    /** Also used synchronously at interaction time and before lock/screen transitions. */
    fun read(context: Context): MessageSystemPolicy {
        val secure = runCatching { context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure != false }
            .getOrDefault(true)
        return IslandBus.messageSystemPolicy.value.copy(
            showOnLock = setting(context, "lock_screen_show_notifications"),
            showPrivate = !secure || setting(context, "lock_screen_allow_private_notifications"),
        )
    }

    fun refresh(context: Context) {
        IslandBus.messageSystemPolicy.value = read(context)
    }

    fun updateRankings(rankingMap: NotificationListenerService.RankingMap, keys: Set<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val overrides = keys.mapNotNull { key ->
            val ranking = NotificationListenerService.Ranking()
            if (!rankingMap.getRanking(key, ranking)) return@mapNotNull null
            val visibility = when (ranking.lockscreenVisibilityOverride) {
                Notification.VISIBILITY_SECRET -> MessageVisibility.SECRET
                Notification.VISIBILITY_PRIVATE -> MessageVisibility.PRIVATE
                Notification.VISIBILITY_PUBLIC -> MessageVisibility.PUBLIC
                else -> return@mapNotNull null
            }
            key to visibility
        }.toMap()
        IslandBus.messageSystemPolicy.value = IslandBus.messageSystemPolicy.value.copy(visibilityOverrides = overrides)
    }

    fun forget(key: String) {
        IslandBus.forgetMessage(key)
        val current = IslandBus.messageSystemPolicy.value
        if (key in current.visibilityOverrides) {
            IslandBus.messageSystemPolicy.value = current.copy(visibilityOverrides = current.visibilityOverrides - key)
        }
    }

    private fun setting(context: Context, name: String): Boolean = try {
        Settings.Secure.getInt(context.contentResolver, name) != 0
    } catch (_: Settings.SettingNotFoundException) {
        true // Android defaults before the setting has been explicitly changed.
    } catch (_: Exception) {
        false // An unreadable policy must not reveal content.
    }
}
