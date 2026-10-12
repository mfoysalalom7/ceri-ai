package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.GeminiApiClient
import com.example.data.model.ActivityType
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MessageRole
import com.example.data.model.PersonaDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val apiClient: GeminiApiClient,
    private val userProfileRepo: UserProfileRepository,
    private val onNewAiMessageNotification: (title: String, message: String) -> Unit = { _, _ -> }
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cherry_chat_sessions", Context.MODE_PRIVATE)

    private val _sessions = MutableStateFlow<List<ChatSession>>(loadSessions())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow(
        _sessions.value.firstOrNull()?.id ?: createInitialSession().id
    )
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(loadAllMessages())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    private fun createInitialSession(): ChatSession {
        val initialSession = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "Welcome to Cherry AI",
            personaId = "cherry_assistant",
            lastMessageSnippet = "Hello! Ask me anything."
        )
        val initialMsg = ChatMessage(
            sessionId = initialSession.id,
            role = MessageRole.MODEL,
            content = "🍒 **হ্যালো! Welcome to Cherry AI!**\n\n" +
                    "I am your personal AI assistant powered by Gemini. You can ask me questions in English or Bengali (বাংলা), request coding help, analyze images, brainstorm creative ideas, or try our custom AI Personas!",
            personaId = "cherry_assistant"
        )
        _sessions.value = listOf(initialSession)
        _messages.value = mapOf(initialSession.id to listOf(initialMsg))
        saveSessions(listOf(initialSession))
        saveMessagesForSession(initialSession.id, listOf(initialMsg))
        return initialSession
    }

    private fun loadSessions(): List<ChatSession> {
        val json = prefs.getString("sessions_list", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<ChatSession>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ChatSession(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        createdAt = obj.getLong("createdAt"),
                        lastMessageSnippet = obj.optString("snippet", ""),
                        personaId = obj.optString("personaId", "cherry_assistant")
                    )
                )
            }
            if (list.isEmpty()) emptyList() else list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadAllMessages(): Map<String, List<ChatMessage>> {
        val map = mutableMapOf<String, List<ChatMessage>>()
        _sessions.value.forEach { session ->
            val json = prefs.getString("messages_${session.id}", null)
            if (json != null) {
                try {
                    val arr = JSONArray(json)
                    val msgs = mutableListOf<ChatMessage>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        msgs.add(
                            ChatMessage(
                                id = obj.getString("id"),
                                sessionId = session.id,
                                role = MessageRole.valueOf(obj.getString("role")),
                                content = obj.getString("content"),
                                imageBase64 = if (obj.has("imageBase64")) obj.getString("imageBase64") else null,
                                imageUri = if (obj.has("imageUri")) obj.getString("imageUri") else null,
                                timestamp = obj.getLong("timestamp"),
                                personaId = obj.optString("personaId", session.personaId),
                                isError = obj.optBoolean("isError", false)
                            )
                        )
                    }
                    map[session.id] = msgs
                } catch (e: Exception) {
                    map[session.id] = emptyList()
                }
            } else {
                map[session.id] = emptyList()
            }
        }
        return map
    }

    fun createNewSession(personaId: String = "cherry_assistant", customTitle: String? = null): ChatSession {
        val persona = PersonaDefaults.getById(personaId)
        val title = customTitle ?: "Chat with ${persona.name}"
        val newSession = ChatSession(
            id = UUID.randomUUID().toString(),
            title = title,
            personaId = personaId,
            lastMessageSnippet = "Started conversation"
        )
        val welcomeMsg = ChatMessage(
            sessionId = newSession.id,
            role = MessageRole.MODEL,
            content = "${persona.iconEmoji} **${persona.name} is ready.**\n\n${persona.shortDescription}.\n\nHow can I help you today?",
            personaId = personaId
        )

        val updatedSessions = listOf(newSession) + _sessions.value
        _sessions.value = updatedSessions
        _activeSessionId.value = newSession.id

        val currentMessages = _messages.value.toMutableMap()
        currentMessages[newSession.id] = listOf(welcomeMsg)
        _messages.value = currentMessages

        saveSessions(updatedSessions)
        saveMessagesForSession(newSession.id, listOf(welcomeMsg))

        userProfileRepo.logActivity(
            type = ActivityType.CHAT_QUERY,
            title = "New Chat Created",
            detail = "Created session with ${persona.name}",
            estimatedTokens = 35
        )

        return newSession
    }

    fun switchSession(sessionId: String) {
        _activeSessionId.value = sessionId
    }

    fun deleteSession(sessionId: String) {
        val updated = _sessions.value.filterNot { it.id == sessionId }
        _sessions.value = updated
        val msgMap = _messages.value.toMutableMap()
        msgMap.remove(sessionId)
        _messages.value = msgMap
        prefs.edit().remove("messages_$sessionId").apply()
        saveSessions(updated)

        if (_activeSessionId.value == sessionId) {
            if (updated.isNotEmpty()) {
                _activeSessionId.value = updated.first().id
            } else {
                createNewSession()
            }
        }
    }

    fun clearCurrentSession() {
        val currentId = _activeSessionId.value
        val currentSession = _sessions.value.find { it.id == currentId } ?: return
        val persona = PersonaDefaults.getById(currentSession.personaId)
        val restartMsg = ChatMessage(
            sessionId = currentId,
            role = MessageRole.MODEL,
            content = "${persona.iconEmoji} Conversation cleared. Ready for fresh questions!",
            personaId = currentSession.personaId
        )
        val msgMap = _messages.value.toMutableMap()
        msgMap[currentId] = listOf(restartMsg)
        _messages.value = msgMap
        saveMessagesForSession(currentId, listOf(restartMsg))
    }

    suspend fun sendMessage(
        userText: String,
        imageBase64: String? = null,
        imageUri: String? = null
    ) {
        val currentId = _activeSessionId.value
        val session = _sessions.value.find { it.id == currentId } ?: createNewSession()
        val persona = PersonaDefaults.getById(session.personaId)

        // 1. Add user message
        val userMsg = ChatMessage(
            sessionId = session.id,
            role = MessageRole.USER,
            content = userText,
            imageBase64 = imageBase64,
            imageUri = imageUri,
            personaId = session.personaId
        )

        // 2. Add temporary loading message for AI
        val loadingMsg = ChatMessage(
            id = "loading_${UUID.randomUUID()}",
            sessionId = session.id,
            role = MessageRole.MODEL,
            content = "Cherry AI is thinking...",
            personaId = session.personaId,
            isLoading = true
        )

        val currentSessionMsgs = (_messages.value[session.id] ?: emptyList()).toMutableList()
        currentSessionMsgs.add(userMsg)
        currentSessionMsgs.add(loadingMsg)

        val updatedMap = _messages.value.toMutableMap()
        updatedMap[session.id] = currentSessionMsgs
        _messages.value = updatedMap

        // Update session snippet & title if it's the first query
        val isFirstQuery = currentSessionMsgs.count { it.role == MessageRole.USER } <= 1
        val updatedTitle = if (isFirstQuery) {
            userText.take(28).trim().ifEmpty { session.title }
        } else {
            session.title
        }

        updateSessionMeta(session.id, updatedTitle, userText.take(40))

        userProfileRepo.logActivity(
            type = if (imageBase64 != null) ActivityType.IMAGE_ANALYSIS else ActivityType.CHAT_QUERY,
            title = if (imageBase64 != null) "Vision Query" else "AI Chat Query",
            detail = userText.take(60),
            estimatedTokens = (userText.length / 4) + (if (imageBase64 != null) 250 else 30)
        )

        // Prepare conversation history
        val history = currentSessionMsgs
            .filter { !it.isLoading && !it.isError }
            .map { (if (it.role == MessageRole.USER) "user" else "model") to it.content }

        // Call Gemini
        val customKey = userProfileRepo.getCustomApiKey()
        val result = apiClient.generateContent(
            prompt = userText,
            persona = persona,
            conversationHistory = history,
            imageBase64 = imageBase64,
            customApiKey = customKey
        )

        // Remove loading message and append response
        val finalMsgs = (_messages.value[session.id] ?: emptyList()).filterNot { it.isLoading }.toMutableList()

        result.fold(
            onSuccess = { responseText ->
                val aiMsg = ChatMessage(
                    sessionId = session.id,
                    role = MessageRole.MODEL,
                    content = responseText,
                    personaId = session.personaId,
                    isError = false
                )
                finalMsgs.add(aiMsg)
                val finalMap = _messages.value.toMutableMap()
                finalMap[session.id] = finalMsgs
                _messages.value = finalMap
                saveMessagesForSession(session.id, finalMsgs)

                updateSessionMeta(session.id, updatedTitle, responseText.take(40))

                onNewAiMessageNotification(persona.name, responseText.take(80))
            },
            onFailure = { error ->
                val errorMsg = ChatMessage(
                    sessionId = session.id,
                    role = MessageRole.MODEL,
                    content = "⚠️ Unable to get response: ${error.localizedMessage}",
                    personaId = session.personaId,
                    isError = true
                )
                finalMsgs.add(errorMsg)
                val finalMap = _messages.value.toMutableMap()
                finalMap[session.id] = finalMsgs
                _messages.value = finalMap
                saveMessagesForSession(session.id, finalMsgs)
            }
        )
    }

    private fun updateSessionMeta(sessionId: String, title: String, snippet: String) {
        val updated = _sessions.value.map {
            if (it.id == sessionId) it.copy(title = title, lastMessageSnippet = snippet) else it
        }
        _sessions.value = updated
        saveSessions(updated)
    }

    private fun saveSessions(sessions: List<ChatSession>) {
        try {
            val arr = JSONArray()
            sessions.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("createdAt", s.createdAt)
                    put("snippet", s.lastMessageSnippet)
                    put("personaId", s.personaId)
                }
                arr.put(obj)
            }
            prefs.edit().putString("sessions_list", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveMessagesForSession(sessionId: String, msgs: List<ChatMessage>) {
        try {
            val arr = JSONArray()
            msgs.forEach { m ->
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("role", m.role.name)
                    put("content", m.content)
                    m.imageBase64?.let { put("imageBase64", it) }
                    m.imageUri?.let { put("imageUri", it) }
                    put("timestamp", m.timestamp)
                    put("personaId", m.personaId)
                    put("isError", m.isError)
                }
                arr.put(obj)
            }
            prefs.edit().putString("messages_$sessionId", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
