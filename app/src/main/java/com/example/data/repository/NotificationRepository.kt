package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class NotificationRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("cherry_notifications", Context.MODE_PRIVATE)

    private val _notifications = MutableStateFlow<List<AppNotification>>(loadNotifications())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private fun loadNotifications(): List<AppNotification> {
        val jsonStr = prefs.getString("notifications_json", null) ?: return defaultInitialNotifications()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<AppNotification>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AppNotification(
                        id = obj.getString("id"),
                        type = NotificationType.valueOf(obj.getString("type")),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        timestamp = obj.getLong("timestamp"),
                        isRead = obj.getBoolean("isRead"),
                        senderName = if (obj.has("senderName")) obj.getString("senderName") else null,
                        senderAvatarEmoji = if (obj.has("senderAvatarEmoji")) obj.getString("senderAvatarEmoji") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultInitialNotifications()
        }
    }

    private fun defaultInitialNotifications(): List<AppNotification> {
        val now = System.currentTimeMillis()
        return listOf(
            AppNotification(
                type = NotificationType.NEW_MESSAGE,
                title = "Cherry AI Assistant",
                message = "Welcome! I am ready to help you with code, writing, and questions in Bengali & English.",
                timestamp = now - 3600000,
                isRead = false,
                senderName = "Cherry AI",
                senderAvatarEmoji = "🍒"
            ),
            AppNotification(
                type = NotificationType.FRIEND_REQUEST,
                title = "Friend Request: Ayla Tanvir",
                message = "Ayla sent you a friend request to collaborate on AI prompts.",
                timestamp = now - 7200000,
                isRead = false,
                senderName = "Ayla Tanvir",
                senderAvatarEmoji = "👩‍💻"
            ),
            AppNotification(
                type = NotificationType.ANNOUNCEMENT,
                title = "Gemini 3.5 Flash Support Active",
                message = "High-speed multimodal responses and Bengali language understanding are now live!",
                timestamp = now - 86400000,
                isRead = true,
                senderName = "System",
                senderAvatarEmoji = "📢"
            )
        )
    }

    fun addNotification(
        type: NotificationType,
        title: String,
        message: String,
        senderName: String? = null,
        senderAvatarEmoji: String? = null
    ): AppNotification {
        val newNotif = AppNotification(
            type = type,
            title = title,
            message = message,
            senderName = senderName,
            senderAvatarEmoji = senderAvatarEmoji
        )
        val current = _notifications.value.toMutableList()
        current.add(0, newNotif)
        _notifications.value = current
        saveNotifications(current)
        return newNotif
    }

    fun markAsRead(id: String) {
        val updated = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        _notifications.value = updated
        saveNotifications(updated)
    }

    fun markAllAsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        saveNotifications(updated)
    }

    fun removeNotification(id: String) {
        val updated = _notifications.value.filterNot { it.id == id }
        _notifications.value = updated
        saveNotifications(updated)
    }

    fun clearAll() {
        _notifications.value = emptyList()
        prefs.edit().remove("notifications_json").apply()
    }

    private fun saveNotifications(list: List<AppNotification>) {
        try {
            val array = JSONArray()
            list.forEach { notif ->
                val obj = JSONObject().apply {
                    put("id", notif.id)
                    put("type", notif.type.name)
                    put("title", notif.title)
                    put("message", notif.message)
                    put("timestamp", notif.timestamp)
                    put("isRead", notif.isRead)
                    notif.senderName?.let { put("senderName", it) }
                    notif.senderAvatarEmoji?.let { put("senderAvatarEmoji", it) }
                }
                array.put(obj)
            }
            prefs.edit().putString("notifications_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
