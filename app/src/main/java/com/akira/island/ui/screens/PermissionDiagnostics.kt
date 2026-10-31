package com.akira.island.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.akira.island.core.IslandBus
import com.akira.island.core.IslandHealth
import com.akira.island.core.ServiceDiagnostics
import com.akira.island.core.tr
import com.akira.island.data.SettingsRepo
import com.akira.island.ui.Group
import com.akira.island.ui.GroupItem
import com.akira.island.ui.Perms
import com.akira.island.ui.PillAction
import com.akira.island.ui.Pos
import com.akira.island.ui.SectionHeader
import com.akira.island.ui.Tile
import com.akira.island.update.UpdateChecker

@Composable
internal fun rememberIslandHealth(accessibility: Boolean): IslandHealth {
    val s by SettingsRepo.state.collectAsState()
    val connected by IslandBus.serviceRunning.collectAsState()
    val runtime by ServiceDiagnostics.state.collectAsState()
    val landscape by IslandBus.landscape.collectAsState()
    val outsideSchedule by IslandBus.offSchedule.collectAsState()
    return IslandHealth.evaluate(
        accessibility, connected, runtime, s.enabled,
        s.hideInLandscape && landscape, outsideSchedule,
    )
}

internal fun islandHealthText(health: IslandHealth): String = when (health) {
    IslandHealth.ACCESSIBILITY_OFF -> tr("Включите службу в спец. возможностях", "Turn on the service in Accessibility")
    IslandHealth.SERVICE_DISCONNECTED -> tr(
        "Доступ разрешён, но служба не подключена. Если состояние не меняется, выключите и включите её в спец. возможностях",
        "Access is granted, but the service is not connected. If this persists, turn it off and on in Accessibility",
    )
    IslandHealth.OVERLAY_FAILED -> tr(
        "Не удалось создать окна острова. Выключите и включите службу в спец. возможностях",
        "The island windows could not be created. Turn the service off and on in Accessibility",
    )
    IslandHealth.PAUSED -> tr("Остров выключен переключателем в приложении", "The island is paused by the in-app switch")
    IslandHealth.LANDSCAPE -> tr("Остров скрыт в альбомной ориентации", "The island is hidden in landscape")
    IslandHealth.SCHEDULE -> tr("Остров скрыт вне расписания", "The island is hidden outside its schedule")
    IslandHealth.READY -> tr("Служба подключена, окна острова созданы", "Service connected; island windows attached")
}

internal fun callPermissionLabel(permission: String): String = when (permission) {
    Manifest.permission.READ_PHONE_STATE -> tr("состояние звонка", "phone state")
    Manifest.permission.READ_CALL_LOG -> tr("номер звонящего", "caller number")
    Manifest.permission.READ_CONTACTS -> tr("имя из контактов", "contact name")
    Manifest.permission.ANSWER_PHONE_CALLS -> tr("ответ и сброс", "answer and decline")
    else -> permission
}

/** Kept on the Permissions page: permission, connection and optional background advice are separate. */
@Composable
internal fun PermissionDiagnostics(perms: PermState, refresh: () -> Unit) {
    val context = LocalContext.current
    val s by SettingsRepo.state.collectAsState()
    val connected by IslandBus.serviceRunning.collectAsState()
    val runtime by ServiceDiagnostics.state.collectAsState()
    val health = rememberIslandHealth(perms.accessibility)

    SectionHeader(tr("Диагностика служб", "Service diagnostics"))
    InfoText(islandHealthText(health))
    Group {
        GroupItem(Pos.First, Icons.Rounded.AccessibilityNew, tr("Служба острова подключена", "Island service connected"),
            runtime.accessibilityError ?: tr("Фактическое подключение Android, не только разрешение", "Actual Android connection, not just permission"),
            onClick = { Perms.openAccessibility(context) }, color = Tile.Blue) { Status(connected) }
        GroupItem(Pos.Middle, Icons.Rounded.TouchApp, tr("Окна острова созданы", "Island windows attached"),
            runtime.overlayError ?: tr("Окно рисования и окно касаний", "Display window and touch window"),
            onClick = { Perms.openAccessibility(context) }, color = Tile.Purple) {
            Status(runtime.displayAttached && runtime.touchAttached)
        }
        GroupItem(Pos.Middle, Icons.Rounded.Notifications, tr("Служба уведомлений подключена", "Notification listener connected"),
            when {
                !perms.notifications -> tr("Сначала разрешите доступ к уведомлениям", "Grant notification access first")
                !runtime.notificationConnected -> tr(
                    "Доступ разрешён, но подключения нет. Если это не меняется, выключите и включите доступ к уведомлениям",
                    "Access granted, but not connected. If this persists, turn notification access off and on",
                )
                else -> tr("Музыка и уведомления могут поступать в остров", "Music and notifications can reach the island")
            }, onClick = { Perms.openNotifications(context) }, color = Tile.Red) { Status(runtime.notificationConnected) }
        GroupItem(Pos.Last, Icons.Rounded.PushPin, tr("Служба удержания запущена", "Keep-alive service running"),
            when {
                !s.keepAlive -> tr("Не запрошена: включите «Не выгружать остров», если нужно", "Not requested: enable Keep the island loaded if needed")
                runtime.keepAliveError != null -> tr("Ошибка запуска: ", "Start failure: ") + runtime.keepAliveError
                !perms.accessibility -> tr("Нужна включённая служба спец. возможностей", "Requires the accessibility service to be enabled")
                !runtime.keepAliveRunning -> tr("Запрошена, но запуск ещё не подтверждён", "Requested, but startup has not been confirmed")
                else -> tr("Android подтвердил запуск на переднем плане", "Foreground startup confirmed")
            }, color = Tile.Green) { Status(runtime.keepAliveRunning) }
    }

    SectionHeader(tr("Дополнительные проверки", "Additional checks"))
    Group {
        GroupItem(Pos.First, Icons.Rounded.Alarm, tr("Точные будильники", "Exact alarms"),
            if (perms.exactAlarms) tr("Таймер может разбудить телефон точно к завершению", "The timer can wake the phone at its deadline")
            else tr("Таймер использует неточный будильник и может опоздать во сне", "The timer uses an inexact alarm and may be late while the phone sleeps"),
            onClick = { Perms.openExactAlarms(context) }, color = Tile.Orange) { Status(perms.exactAlarms) }
        GroupItem(Pos.Middle, Icons.Rounded.BatteryChargingFull, tr("Исключение из оптимизации батареи", "Battery optimization exemption"),
            tr("Проверка Android; автозапуск и ограничения прошивки проверяйте вручную",
                "Android check only; verify firmware restrictions and autostart manually"),
            onClick = { Perms.openBattery(context) }, color = Tile.Green) { Status(perms.batteryExempt) }
        GroupItem(Pos.Last, Icons.Rounded.Notifications, tr("Уведомления самого приложения", "App's own notifications"),
            tr("Отдельно от доступа к чужим уведомлениям. Если запрещены, служба удержания всё ещё может работать",
                "Separate from notification access. If blocked, the keep-alive service can still run"),
            onClick = { Perms.openAppDetails(context) }, color = Tile.Red) { Status(perms.appNotifications) }
    }
    InfoText(tr(
        "Автозапуск, закрепление в недавних и ограниченные настройки нельзя надёжно проверить автоматически. " +
            "Отсутствие исключения батареи не означает, что служба остановлена.",
        "Autostart, locking in recent apps and restricted settings cannot be reliably checked automatically. " +
            "A missing battery exemption does not mean the service is stopped.",
    ))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PillAction(tr("Проверить снова", "Refresh"), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = refresh)
        PillAction(tr("Скопировать диагностику", "Copy diagnostics"), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            // Re-read grants on click. Only technical state: no device IDs, notification text or contact data.
            val accessibility = Perms.accessibility(context)
            val currentRuntime = ServiceDiagnostics.state.value
            val currentSettings = SettingsRepo.current
            val currentConnected = IslandBus.serviceRunning.value
            val currentHealth = IslandHealth.evaluate(
                accessibility, currentConnected, currentRuntime, currentSettings.enabled,
                currentSettings.hideInLandscape && IslandBus.landscape.value, IslandBus.offSchedule.value,
            )
            val report = buildString {
                appendLine("Dynamic Island service diagnostics")
                appendLine("App version: ${UpdateChecker.currentVersion(context)}")
                appendLine("Android SDK: ${Build.VERSION.SDK_INT}")
                appendLine("Manufacturer: ${Build.MANUFACTURER}")
                appendLine("Health: $currentHealth")
                appendLine("Accessibility granted: $accessibility")
                appendLine("Island connected: $currentConnected")
                appendLine("Notification access granted: ${Perms.notifications(context)}")
                appendLine("Island enabled: ${currentSettings.enabled}")
                appendLine("Hidden in landscape: ${currentSettings.hideInLandscape && IslandBus.landscape.value}")
                appendLine("Outside schedule: ${IslandBus.offSchedule.value}")
                appendLine("Keep-alive requested: ${currentSettings.keepAlive}")
                appendLine("Runtime: $currentRuntime")
                appendLine("Telephony available: ${Perms.telephony(context)}")
                Perms.CALL_PERMISSIONS.forEach { appendLine("$it: ${Perms.granted(context, it)}") }
                appendLine("Bluetooth granted: ${Perms.bluetooth(context)}")
                appendLine("Exact alarms allowed: ${Perms.exactAlarms(context)}")
                appendLine("Battery optimization exempt: ${Perms.batteryUnrestricted(context)}")
                appendLine("Own notifications allowed: ${Perms.appNotifications(context)}")
                appendLine("Autostart / recent-app lock / restricted settings: manual check required")
            }
            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Service diagnostics", report))
        }
    }
}
