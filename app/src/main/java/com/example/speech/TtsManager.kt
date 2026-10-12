package com.example.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentlySpeakingId = MutableStateFlow<String?>(null)
    val currentlySpeakingId: StateFlow<String?> = _currentlySpeakingId.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.getDefault()
        }
    }

    fun speak(text: String, messageId: String) {
        if (!isInitialized || tts == null) return

        // Clean markdown symbols for cleaner speech synthesis
        val cleanText = text
            .replace(Regex("[*#`_~]"), "")
            .replace(Regex("```[a-zA-Z]*"), "")
            .take(600) // Read reasonable snippet

        if (_currentlySpeakingId.value == messageId && _isPlaying.value) {
            stop()
            return
        }

        stop()

        // Check if text has Bengali characters
        val hasBengali = cleanText.any { it in '\u0980'..'\u09FF' }
        if (hasBengali) {
            tts?.language = Locale.Builder().setLanguage("bn").setRegion("BD").build()
        } else {
            tts?.language = Locale.US
        }

        _currentlySpeakingId.value = messageId
        _isPlaying.value = true

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, messageId)
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
        _currentlySpeakingId.value = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
