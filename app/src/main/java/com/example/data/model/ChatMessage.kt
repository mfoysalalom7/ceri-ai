package com.example.data.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val imageBase64: String? = null,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val personaId: String = "cherry_assistant",
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

enum class MessageRole {
    USER,
    MODEL
}
