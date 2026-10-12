package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ActivityLog
import com.example.data.model.ActivityType
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class UserProfileRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("cherry_user_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _activityLogs = MutableStateFlow(loadActivityLogs())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs.asStateFlow()

    private fun loadProfile(): UserProfile {
        val username = prefs.getString("username", "CherryExplorer") ?: "CherryExplorer"
        val displayName = prefs.getString("display_name", "Cherry Explorer") ?: "Cherry Explorer"
        val bio = prefs.getString("bio", "Exploring the frontiers of AI with Cherry AI ✨") ?: "Exploring the frontiers of AI with Cherry AI ✨"
        val avatarUri = prefs.getString("avatar_uri", null)
        val avatarPreset = prefs.getString("avatar_preset", "cherry_blossom") ?: "cherry_blossom"
        val favoritePersonaId = prefs.getString("favorite_persona", "cherry_assistant") ?: "cherry_assistant"
        val joined = prefs.getLong("joined_time", System.currentTimeMillis())

        return UserProfile(
            username = username,
            displayName = displayName,
            bio = bio,
            avatarUri = avatarUri,
            avatarPreset = avatarPreset,
            favoritePersonaId = favoritePersonaId,
            joinedTimestamp = joined
        )
    }

    fun updateProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarUri: String?,
        avatarPreset: String,
        favoritePersonaId: String
    ) {
        val updated = _userProfile.value.copy(
            username = username.trim().ifEmpty { "CherryUser" },
            displayName = displayName.trim().ifEmpty { "Cherry User" },
            bio = bio.trim(),
            avatarUri = avatarUri,
            avatarPreset = avatarPreset,
            favoritePersonaId = favoritePersonaId
        )
        prefs.edit()
            .putString("username", updated.username)
            .putString("display_name", updated.displayName)
            .putString("bio", updated.bio)
            .putString("avatar_uri", updated.avatarUri)
            .putString("avatar_preset", updated.avatarPreset)
            .putString("favorite_persona", updated.favoritePersonaId)
            .apply()

        _userProfile.value = updated

        logActivity(
            type = ActivityType.PROFILE_UPDATE,
            title = "Profile Updated",
            detail = "Updated username to @${updated.username}",
            estimatedTokens = 0
        )
    }

    private fun loadActivityLogs(): List<ActivityLog> {
        val jsonStr = prefs.getString("activity_logs_json", null) ?: return defaultInitialLogs()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ActivityLog>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ActivityLog(
                        id = obj.getString("id"),
                        type = ActivityType.valueOf(obj.getString("type")),
                        title = obj.getString("title"),
                        detail = obj.getString("detail"),
                        timestamp = obj.getLong("timestamp"),
                        estimatedTokens = obj.optInt("estimatedTokens", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultInitialLogs()
        }
    }

    private fun defaultInitialLogs(): List<ActivityLog> {
        val now = System.currentTimeMillis()
        return listOf(
            ActivityLog(
                type = ActivityType.NOTIFICATION_ACTION,
                title = "Welcome to Cherry AI",
                detail = "User profile created and assistant initialized",
                timestamp = now - 3600000 * 2,
                estimatedTokens = 120
            ),
            ActivityLog(
                type = ActivityType.CHAT_QUERY,
                title = "Initial Conversation",
                detail = "Started chat with Cherry AI Assistant",
                timestamp = now - 1800000,
                estimatedTokens = 85
            )
        )
    }

    fun logActivity(
        type: ActivityType,
        title: String,
        detail: String,
        estimatedTokens: Int = 0
    ) {
        val newLog = ActivityLog(
            type = type,
            title = title,
            detail = detail,
            estimatedTokens = estimatedTokens
        )
        val current = _activityLogs.value.toMutableList()
        current.add(0, newLog)
        val trimmed = current.take(50) // keep last 50 activities
        _activityLogs.value = trimmed

        saveActivityLogs(trimmed)
    }

    private fun saveActivityLogs(logs: List<ActivityLog>) {
        try {
            val array = JSONArray()
            logs.forEach { log ->
                val obj = JSONObject().apply {
                    put("id", log.id)
                    put("type", log.type.name)
                    put("title", log.title)
                    put("detail", log.detail)
                    put("timestamp", log.timestamp)
                    put("estimatedTokens", log.estimatedTokens)
                }
                array.put(obj)
            }
            prefs.edit().putString("activity_logs_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearActivityLogs() {
        _activityLogs.value = emptyList()
        prefs.edit().remove("activity_logs_json").apply()
    }

    fun getCustomApiKey(): String? {
        return prefs.getString("custom_api_key", null)
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString("custom_api_key", key.trim()).apply()
    }
}
