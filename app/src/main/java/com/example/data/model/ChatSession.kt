package com.example.data.model

import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageSnippet: String = "",
    val personaId: String = "cherry_assistant"
)
