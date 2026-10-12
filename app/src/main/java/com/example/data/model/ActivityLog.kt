package com.example.data.model

import java.util.UUID

data class ActivityLog(
    val id: String = UUID.randomUUID().toString(),
    val type: ActivityType,
    val title: String,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val estimatedTokens: Int = 0
)

enum class ActivityType(val label: String, val icon: String) {
    CHAT_QUERY("AI Query", "💬"),
    IMAGE_ANALYSIS("Vision Analysis", "🖼️"),
    VOICE_PROMPT("Voice Query", "🎙️"),
    PERSONA_SWITCH("Persona Switch", "🎭"),
    PROFILE_UPDATE("Profile Updated", "👤"),
    NOTIFICATION_ACTION("Notification", "🔔")
}
