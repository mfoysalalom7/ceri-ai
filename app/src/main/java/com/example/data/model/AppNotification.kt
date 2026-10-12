package com.example.data.model

import java.util.UUID

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val senderName: String? = null,
    val senderAvatarEmoji: String? = null
)

enum class NotificationType(val channelName: String, val badgeColorHex: Long) {
    NEW_MESSAGE("AI Messages", 0xFFD81B60),
    FRIEND_REQUEST("Community & Friends", 0xFF9C27B0),
    ANNOUNCEMENT("Announcements", 0xFFFF6F00)
}
