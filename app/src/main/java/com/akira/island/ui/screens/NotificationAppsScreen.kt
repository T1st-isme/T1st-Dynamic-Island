package com.akira.island.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.akira.island.core.tr
import com.akira.island.data.SettingsRepo
import com.akira.island.service.NotificationParser
import com.akira.island.ui.Group
import com.akira.island.ui.GroupToggle
import com.akira.island.ui.Page
import com.akira.island.ui.Pos
import com.akira.island.ui.Tile

@Composable
fun NotificationAppsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s by SettingsRepo.state.collectAsState()
    val packages = s.msgChannels.keys + s.blockedNotificationApps
    val apps = remember(packages, context) {
        packages.map { it to NotificationParser.appLabel(context, it) }
            .sortedWith(compareBy<Pair<String, String>> { it.second.lowercase() }.thenBy { it.first })
    }
    Page(tr("Приложения для сообщений", "Message apps", "Ứng dụng gửi tin nhắn"), onBack) {
        InfoText(tr(
            "Новые приложения разрешены по умолчанию и появляются здесь после первого сообщения в острове. Пока приложение выключено, его обычные сообщения скрыты. Очередь не очищается: после включения ещё ожидающее сообщение может появиться. Музыка, звонки, навигация и оплата настраиваются отдельно.",
            "New apps are allowed by default and appear here after their first message in the island. While an app is off, its regular messages are hidden. The queue is not purged: a still-pending banner may appear after you turn it back on. Music, calls, navigation and payments are configured separately.",
            "Ứng dụng mới được cho phép mặc định và xuất hiện ở đây sau tin nhắn đầu tiên trên đảo. Khi tắt, thẻ tin nhắn của ứng dụng bị ẩn. Hàng đợi không bị xóa: nếu bật lại khi thẻ vẫn còn chờ, thẻ đó có thể xuất hiện. Nhạc, cuộc gọi, chỉ đường và thanh toán được cấu hình riêng.",
        ))
        if (apps.isEmpty()) {
            InfoText(tr(
                "Пока нет приложений. Разрешите доступ к уведомлениям и получите обычное сообщение из другого приложения.",
                "No apps yet. Enable notification access and receive a regular message from another app.",
                "Chưa có ứng dụng. Hãy cấp quyền truy cập thông báo rồi nhận một tin nhắn thông thường từ ứng dụng khác.",
            ))
        } else {
            Group {
                apps.forEachIndexed { index, (pkg, label) ->
                    val pos = when {
                        apps.size == 1 -> Pos.Single
                        index == 0 -> Pos.First
                        index == apps.lastIndex -> Pos.Last
                        else -> Pos.Middle
                    }
                    GroupToggle(pos, Icons.Rounded.Apps, label, pkg,
                        pkg !in s.blockedNotificationApps, color = Tile.Blue) { allowed ->
                        SettingsRepo.update {
                            copy(blockedNotificationApps = if (allowed) blockedNotificationApps - pkg else blockedNotificationApps + pkg)
                        }
                    }
                }
            }
        }
    }
}
