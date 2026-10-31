package com.akira.island.data

import com.akira.island.core.tr
import kotlinx.serialization.Serializable

/** Maximum detail allowed in regular notification banners, never in calls or live activities. */
@Serializable
enum class NotificationContent(private val ru: String, private val en: String, private val vi: String) {
    FULL("Полное содержимое", "Full content", "Nội dung đầy đủ"),
    APP_ONLY("Только приложение", "App only", "Chỉ ứng dụng"),
    ICON_ONLY("Только значок", "Icon only", "Chỉ biểu tượng"),
    ;

    val title: String get() = tr(ru, en, vi)
}
