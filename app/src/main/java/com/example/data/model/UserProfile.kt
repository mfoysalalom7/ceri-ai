package com.example.data.model

data class UserProfile(
    val username: String = "CherryExplorer",
    val displayName: String = "Cherry Explorer",
    val bio: String = "Exploring the frontiers of AI with Cherry AI ✨",
    val avatarUri: String? = null,
    val avatarPreset: String = "cherry_blossom",
    val favoritePersonaId: String = "cherry_assistant",
    val joinedTimestamp: Long = System.currentTimeMillis()
)

object AvatarPresets {
    val presets = listOf(
        AvatarPreset("cherry_blossom", "🍒", "Cherry Blossom"),
        AvatarPreset("cyber_robot", "🤖", "Cyber Bot"),
        AvatarPreset("star_gazer", "✨", "Star Gazer"),
        AvatarPreset("pixel_cat", "🐱", "Neon Cat"),
        AvatarPreset("creative_fox", "🦊", "Wise Fox"),
        AvatarPreset("digital_owl", "🦉", "Digital Owl")
    )
}

data class AvatarPreset(
    val id: String,
    val emoji: String,
    val name: String
)
