package com.akira.island.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.akira.island.core.tr
import com.akira.island.data.AppLanguage
import com.akira.island.data.NotificationContent
import com.akira.island.data.SettingsRepo
import com.akira.island.data.SetupStep
import com.akira.island.ui.ChoiceDialog
import com.akira.island.ui.Group
import com.akira.island.ui.GroupItem
import com.akira.island.ui.GroupToggle
import com.akira.island.ui.Page
import com.akira.island.ui.Perms
import com.akira.island.ui.PillAction
import com.akira.island.ui.Pos
import com.akira.island.ui.Screen
import com.akira.island.ui.Tile

/** A resumable guide, not a permission gate. Only explicit actions open Android permission screens. */
@Composable
fun SetupScreen(onFinish: () -> Unit, onBack: () -> Unit, open: (Screen) -> Unit) {
    val context = LocalContext.current
    val s by SettingsRepo.state.collectAsState()
    val (perms, _) = rememberPermState()
    val step = s.setupStep
    var languageDialog by remember { mutableStateOf(false) }
    var contentDialog by remember { mutableStateOf(false) }
    fun finish() {
        SettingsRepo.update { copy(setupCompleted = true) }
        onFinish()
    }
    Page(
        tr("Начальная настройка", "Setup guide", "Hướng dẫn thiết lập"),
        onBack = onBack,
        subtitle = tr(
            "Шаг ${step.ordinal + 1} из ${SetupStep.entries.size}",
            "Step ${step.ordinal + 1} of ${SetupStep.entries.size}",
            "Bước ${step.ordinal + 1}/${SetupStep.entries.size}",
        ),
    ) {
        when (step) {
            SetupStep.WELCOME -> {
                InfoText(tr(
                    "Выберите язык. Затем включите только нужные возможности. Любой шаг можно пропустить и вернуться к нему позже.",
                    "Choose your language, then enable only the features you need. You can skip any step and return later.",
                    "Chọn ngôn ngữ, sau đó chỉ bật các tính năng bạn cần. Bạn có thể bỏ qua bất kỳ bước nào và quay lại sau.",
                ))
                GroupItem(Pos.Single, Icons.Rounded.Language, tr("Язык", "Language", "Ngôn ngữ"), s.language.title,
                    onClick = { languageDialog = true }, color = Tile.Blue) { Arrow() }
            }
            SetupStep.ACCESSIBILITY -> {
                InfoText(tr(
                    "Спец. возможности нужны, чтобы рисовать остров над строкой состояния. Служба также может читать экраны поддерживаемых навигаторов, часов и платёжных приложений. Включайте её, только если доверяете приложению.",
                    "Accessibility is needed to draw the island above the status bar. The service can also read screens in supported navigation, clock and payment apps. Enable it only if you trust this app.",
                    "Quyền Trợ năng cần để vẽ đảo phía trên thanh trạng thái. Dịch vụ cũng có thể đọc màn hình ứng dụng chỉ đường, đồng hồ và thanh toán được hỗ trợ. Chỉ bật khi bạn tin tưởng ứng dụng.",
                ))
                Group {
                    GroupItem(Pos.First, Icons.Rounded.AccessibilityNew, tr("Спец. возможности", "Accessibility", "Trợ năng"),
                        tr("Открыть настройки Android", "Open Android settings", "Mở cài đặt Android"),
                        onClick = { Perms.openAccessibility(context) }, color = Tile.Blue) { Status(perms.accessibility) }
                    GroupItem(Pos.Last, Icons.Rounded.Security, tr("Если доступ ограничен", "If access is restricted", "Nếu quyền bị hạn chế"),
                        tr("Инструкция для Android 13+", "Instructions for Android 13+", "Hướng dẫn cho Android 13+"),
                        onClick = { open(Screen.INSTRUCTION) }, color = Tile.Orange) { Arrow() }
                }
                InfoText(islandHealthText(rememberIslandHealth(perms.accessibility)))
            }
            SetupStep.NOTIFICATIONS -> {
                InfoText(tr(
                    "Необязательно. Доступ к уведомлениям позволяет читать сообщения и медиасессии других приложений для показа в острове. Без него доступны настройки внешнего вида и функции, не зависящие от уведомлений.",
                    "Optional. Notification access lets the island read other apps' messages and media sessions. Without it, appearance settings and features independent of notifications remain available.",
                    "Không bắt buộc. Quyền truy cập thông báo cho phép đảo đọc tin nhắn và phiên phát nhạc của ứng dụng khác. Không cấp quyền vẫn có thể chỉnh giao diện và dùng các tính năng không phụ thuộc thông báo.",
                ))
                GroupItem(Pos.Single, Icons.Rounded.Notifications, tr("Доступ к уведомлениям", "Notification access", "Truy cập thông báo"),
                    tr("Открыть настройки Android", "Open Android settings", "Mở cài đặt Android"),
                    onClick = { Perms.openNotifications(context) }, color = Tile.Red) { Status(perms.notifications) }
            }
            SetupStep.OPTIONAL -> {
                InfoText(tr(
                    "Звонки, контакты и Bluetooth нужны только для соответствующих функций. Разрешение контактов добавляет имя и фото; Bluetooth — сведения о наушниках. Не запрашиваем их автоматически.",
                    "Calls, contacts and Bluetooth are needed only for their respective features. Contacts add caller names and photos; Bluetooth adds headset details. They are not requested automatically.",
                    "Quyền cuộc gọi, danh bạ và Bluetooth chỉ phục vụ tính năng tương ứng. Danh bạ bổ sung tên, ảnh người gọi; Bluetooth bổ sung thông tin tai nghe. Ứng dụng không tự yêu cầu các quyền này.",
                ))
                Group {
                    GroupItem(Pos.First, Icons.Rounded.Security, tr("Дополнительные разрешения", "Optional permissions", "Quyền tùy chọn"),
                        tr("Выберите нужные функции", "Choose the features you need", "Chọn tính năng bạn cần"),
                        onClick = { open(Screen.PERMISSIONS) }, color = Tile.Green) { Arrow() }
                    KeepAliveItem(Pos.Last, open)
                }
            }
            SetupStep.PRIVACY -> {
                InfoText(tr(
                    "Настройте содержимое сообщений. Выбор приложения и дополнительные параметры находятся в разделе «Уведомления». Системные ограничения экрана блокировки сохраняются.",
                    "Choose what message banners reveal. App filtering and further options are in Notifications. Android lockscreen restrictions still apply.",
                    "Chọn nội dung được phép hiện trên thẻ tin nhắn. Bộ lọc ứng dụng và tùy chọn khác nằm trong mục Thông báo. Các giới hạn màn hình khóa của Android vẫn được giữ.",
                ))
                Group {
                    GroupItem(Pos.First, Icons.Rounded.Notifications, tr("Содержимое уведомлений", "Notification content", "Nội dung thông báo"),
                        s.notificationContent.title, onClick = { contentDialog = true }, color = Tile.Blue) { Arrow() }
                    GroupToggle(Pos.Last, Icons.Rounded.Lock,
                        tr("Скрывать содержимое при блокировке", "Hide content while locked", "Ẩn nội dung khi khóa máy"),
                        tr("Без отправителя, текста и аватара", "No sender, text or avatar", "Không hiện người gửi, nội dung hoặc ảnh đại diện"),
                        s.hideNotificationContentOnLock, color = Tile.Purple) { value ->
                        SettingsRepo.update { copy(hideNotificationContentOnLock = value) }
                    }
                }
            }
            SetupStep.TRY_IT -> {
                InfoText(tr(
                    "Подгоните капсулу под камеру и попробуйте примеры. Примеры проверяют отображение, а реальные звонки, уведомления и работу в фоне нужно проверить отдельно. Завершение настройки не включает службы и не выдаёт разрешения.",
                    "Fit the capsule around your camera and try the demos. Demos check display only; test real calls, notifications and background operation separately. Finishing setup does not enable services or grant permissions.",
                    "Căn đảo theo camera rồi thử các ví dụ. Ví dụ chỉ kiểm tra hiển thị; cuộc gọi, thông báo thật và chạy nền cần được thử riêng. Hoàn tất hướng dẫn không tự bật dịch vụ hay cấp quyền.",
                ))
                Group {
                    GroupItem(Pos.First, Icons.Rounded.Tune, tr("Положение и размер", "Position and size", "Vị trí và kích thước"),
                        onClick = { open(Screen.SIZE) }, color = Tile.Blue) { Arrow() }
                    GroupItem(Pos.Last, Icons.Rounded.Science, tr("Проверка", "Try it", "Dùng thử"),
                        onClick = { open(Screen.TESTS) }, color = Tile.Purple) { Arrow() }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val next = step.next
            val nextLabel = when {
                next == null -> tr("Готово", "Finish setup", "Hoàn tất thiết lập")
                step == SetupStep.ACCESSIBILITY && !perms.accessibility ->
                    tr("Продолжить без службы", "Continue without the service", "Tiếp tục khi chưa bật dịch vụ")
                step == SetupStep.NOTIFICATIONS && !perms.notifications ->
                    tr("Пропустить этот шаг", "Skip this step", "Bỏ qua bước này")
                else -> tr("Далее", "Continue", "Tiếp tục")
            }
            PillAction(nextLabel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                if (next == null) finish() else SettingsRepo.update { copy(setupStep = next) }
            }
            if (next != null) {
                PillAction(tr("Настроить позже", "Set up later", "Thiết lập sau"),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = ::finish)
            }
        }
    }
    if (languageDialog) {
        ChoiceDialog(tr("Язык", "Language", "Ngôn ngữ"), AppLanguage.entries, s.language, { it.title },
            { value -> SettingsRepo.update { copy(language = value) } }) { languageDialog = false }
    }
    if (contentDialog) {
        ChoiceDialog(tr("Содержимое уведомлений", "Notification content", "Nội dung thông báo"),
            NotificationContent.entries, s.notificationContent, { it.title },
            { value -> SettingsRepo.update { copy(notificationContent = value) } }) { contentDialog = false }
    }
}
