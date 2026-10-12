package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.model.ActivityType
import com.example.data.model.AiPersona
import com.example.data.model.AppNotification
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.NotificationType
import com.example.data.model.PersonaDefaults
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.UserProfileRepository
import com.example.notifications.CherryNotificationHelper
import com.example.speech.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

enum class ScreenTab(val title: String, val iconEmoji: String) {
    CHAT("Chat", "💬"),
    PERSONAS("Personas", "🎭"),
    PROFILE("Profile", "👤"),
    ACTIVITY("History", "📜"),
    NOTIFICATIONS("Alerts", "🔔")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val notificationHelper = CherryNotificationHelper(context)
    private val ttsManager = TtsManager(context)
    private val apiClient = GeminiApiClient()

    val userProfileRepo = UserProfileRepository(context)
    val notificationRepo = NotificationRepository(context)
    val chatRepo = ChatRepository(
        context = context,
        apiClient = apiClient,
        userProfileRepo = userProfileRepo,
        onNewAiMessageNotification = { title, snippet ->
            notificationRepo.addNotification(
                type = NotificationType.NEW_MESSAGE,
                title = title,
                message = snippet,
                senderName = title,
                senderAvatarEmoji = "🍒"
            )
            notificationHelper.showAiMessageNotification(title, snippet)
        }
    )

    val userProfile: StateFlow<UserProfile> = userProfileRepo.userProfile
    val activityLogs = userProfileRepo.activityLogs
    val notifications = notificationRepo.notifications
    val sessions = chatRepo.sessions
    val activeSessionId = chatRepo.activeSessionId

    val isSpeaking = ttsManager.isPlaying
    val currentlySpeakingId = ttsManager.currentlySpeakingId

    private val _selectedTab = MutableStateFlow(ScreenTab.CHAT)
    val selectedTab: StateFlow<ScreenTab> = _selectedTab.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<Uri?>(null)
    val attachedImageUri: StateFlow<Uri?> = _attachedImageUri.asStateFlow()

    private val _attachedImageBase64 = MutableStateFlow<String?>(null)
    val attachedImageBase64: StateFlow<String?> = _attachedImageBase64.asStateFlow()

    private val _customApiKeyInput = MutableStateFlow(userProfileRepo.getCustomApiKey() ?: "")
    val customApiKeyInput: StateFlow<String> = _customApiKeyInput.asStateFlow()

    val unreadNotificationsCount: StateFlow<Int> = notificationRepo.notifications
        .combine(MutableStateFlow(0)) { list, _ ->
            list.count { !it.isRead }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentMessages: StateFlow<List<ChatMessage>> = combine(
        chatRepo.messages,
        chatRepo.activeSessionId
    ) { messagesMap, activeId ->
        messagesMap[activeId] ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSession: StateFlow<ChatSession?> = combine(
        chatRepo.sessions,
        chatRepo.activeSessionId
    ) { sessionList, activeId ->
        sessionList.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentPersona: StateFlow<AiPersona> = combine(
        currentSession,
        userProfile
    ) { session, profile ->
        val personaId = session?.personaId ?: profile.favoritePersonaId
        PersonaDefaults.getById(personaId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonaDefaults.getById("cherry_assistant"))

    fun setSelectedTab(tab: ScreenTab) {
        _selectedTab.value = tab
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun onImageSelected(uri: Uri?) {
        if (uri == null) {
            _attachedImageUri.value = null
            _attachedImageBase64.value = null
            return
        }
        _attachedImageUri.value = uri
        viewModelScope.launch {
            val base64 = uriToBase64(context, uri)
            _attachedImageBase64.value = base64
        }
    }

    fun clearAttachedImage() {
        _attachedImageUri.value = null
        _attachedImageBase64.value = null
    }

    fun sendMessage() {
        val query = _inputText.value.trim()
        val imageBase64 = _attachedImageBase64.value
        val imageUri = _attachedImageUri.value?.toString()

        if (query.isEmpty() && imageBase64 == null) return

        _inputText.value = ""
        clearAttachedImage()

        viewModelScope.launch {
            chatRepo.sendMessage(
                userText = query.ifEmpty { "Analyze this image for me." },
                imageBase64 = imageBase64,
                imageUri = imageUri
            )
        }
    }

    fun selectPromptSuggestion(prompt: String) {
        _inputText.value = prompt
        sendMessage()
    }

    fun createNewChat(personaId: String? = null) {
        val targetPersonaId = personaId ?: currentPersona.value.id
        chatRepo.createNewSession(targetPersonaId)
        _selectedTab.value = ScreenTab.CHAT
    }

    fun switchSession(sessionId: String) {
        chatRepo.switchSession(sessionId)
        _selectedTab.value = ScreenTab.CHAT
    }

    fun deleteSession(sessionId: String) {
        chatRepo.deleteSession(sessionId)
    }

    fun clearCurrentChat() {
        chatRepo.clearCurrentSession()
    }

    fun selectPersona(personaId: String) {
        val session = currentSession.value
        if (session != null && session.personaId == personaId) {
            _selectedTab.value = ScreenTab.CHAT
            return
        }
        createNewChat(personaId)
        userProfileRepo.logActivity(
            type = ActivityType.PERSONA_SWITCH,
            title = "Switched Persona",
            detail = "Selected ${PersonaDefaults.getById(personaId).name}",
            estimatedTokens = 10
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
        userProfileRepo.updateProfile(
            username = username,
            displayName = displayName,
            bio = bio,
            avatarUri = avatarUri,
            avatarPreset = avatarPreset,
            favoritePersonaId = favoritePersonaId
        )
    }

    fun clearActivityHistory() {
        userProfileRepo.clearActivityLogs()
    }

    fun markNotificationRead(id: String) {
        notificationRepo.markAsRead(id)
    }

    fun markAllNotificationsRead() {
        notificationRepo.markAllAsRead()
    }

    fun clearAllNotifications() {
        notificationRepo.clearAll()
    }

    fun triggerTestNotification(type: NotificationType) {
        when (type) {
            NotificationType.NEW_MESSAGE -> {
                val notif = notificationRepo.addNotification(
                    type = NotificationType.NEW_MESSAGE,
                    title = "Cherry AI Message",
                    message = "Here is a quick tip: You can attach photos using the image picker to analyze graphs or code screenshots!",
                    senderName = "Cherry AI",
                    senderAvatarEmoji = "🍒"
                )
                notificationHelper.showAiMessageNotification(notif.title, notif.message)
            }
            NotificationType.FRIEND_REQUEST -> {
                val notif = notificationRepo.addNotification(
                    type = NotificationType.FRIEND_REQUEST,
                    title = "Friend Request: Rafiqul Islam",
                    message = "Rafiqul sent you a friend request. Wants to collaborate on Gemini prompts.",
                    senderName = "Rafiqul Islam",
                    senderAvatarEmoji = "👨‍💻"
                )
                notificationHelper.showFriendRequestNotification("Rafiqul Islam", notif.message)
            }
            NotificationType.ANNOUNCEMENT -> {
                val notif = notificationRepo.addNotification(
                    type = NotificationType.ANNOUNCEMENT,
                    title = "Cherry AI 2.0 Update",
                    message = "Added Bengali voice queries, profile customization, and fast notification channels!",
                    senderName = "Team Cherry",
                    senderAvatarEmoji = "✨"
                )
                notificationHelper.showAnnouncementNotification(notif.title, notif.message)
            }
        }
    }

    fun toggleSpeakMessage(messageId: String, text: String) {
        ttsManager.speak(text, messageId)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun setCustomApiKey(key: String) {
        _customApiKeyInput.value = key
        userProfileRepo.setCustomApiKey(key)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Resize if too large
            val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
                val scale = 1024f / Math.max(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else {
                bitmap
            }

            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
