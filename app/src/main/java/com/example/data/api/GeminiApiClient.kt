package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiPersona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    suspend fun generateContent(
        prompt: String,
        persona: AiPersona,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        imageBase64: String? = null,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> null
        }

        if (apiKey.isNullOrBlank()) {
            return@withContext Result.success(getSmartFallbackResponse(prompt, persona, isApiKeyMissing = true))
        }

        try {
            val rootJson = JSONObject()

            // System Instruction
            val sysInstructionObj = JSONObject()
            val sysPartsArray = JSONArray()
            val sysPart = JSONObject().apply {
                put("text", persona.systemInstruction)
            }
            sysPartsArray.put(sysPart)
            sysInstructionObj.put("parts", sysPartsArray)
            rootJson.put("systemInstruction", sysInstructionObj)

            // Contents array (Conversation History + Current Prompt)
            val contentsArray = JSONArray()

            // Add previous recent messages (up to last 6 for context efficiency)
            conversationHistory.takeLast(6).forEach { (role, text) ->
                val contentObj = JSONObject()
                contentObj.put("role", if (role == "user") "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().apply { put("text", text) })
                contentObj.put("parts", parts)
                contentsArray.put(contentObj)
            }

            // Current turn
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()

            // Current text
            currentParts.put(JSONObject().apply { put("text", prompt) })

            // Optional image
            if (!imageBase64.isNullOrBlank()) {
                val inlineDataObj = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", imageBase64)
                }
                currentParts.put(JSONObject().apply {
                    put("inlineData", inlineDataObj)
                })
            }

            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            rootJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 2048)
            }
            rootJson.put("generationConfig", genConfig)

            val url = "$baseUrl?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiApiClient", "API call failed HTTP ${response.code}: $responseBodyString")
                if (response.code == 400 || response.code == 403 || response.code == 404) {
                    return@withContext Result.success(
                        "⚠️ **Gemini API Error (${response.code})**\n\n" +
                        "Could not verify the API key or endpoint.\n\n" +
                        "Here is an offline response from **${persona.name}**:\n\n" +
                        getSmartFallbackResponse(prompt, persona, isApiKeyMissing = false)
                    )
                }
                return@withContext Result.failure(Exception("API returned error code ${response.code}: $responseBodyString"))
            }

            val responseJson = JSONObject(responseBodyString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val fullText = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("text")) {
                            fullText.append(part.getString("text"))
                        }
                    }
                    if (fullText.isNotBlank()) {
                        return@withContext Result.success(fullText.toString())
                    }
                }
            }

            Result.success("Cherry AI received an empty response. Please try rephrasing your prompt.")
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Exception during Gemini request", e)
            Result.success(
                "📡 *Note: Connecting in Offline/Demo Mode (${e.localizedMessage ?: "Network Timeout"})*\n\n" +
                getSmartFallbackResponse(prompt, persona, isApiKeyMissing = false)
            )
        }
    }

    private fun getSmartFallbackResponse(prompt: String, persona: AiPersona, isApiKeyMissing: Boolean): String {
        val lower = prompt.lowercase()
        val bengaliIntro = if (isApiKeyMissing) {
            "🍒 **Cherry AI Assistant**\n\n" +
            "*(💡 টিপস: আপনি AI Studio-র Secrets প্যানেলে `GEMINI_API_KEY` সেট করতে পারেন অথবা অ্যাপ সেটিংসে নিজের কী যুক্ত করতে পারেন।)*\n\n"
        } else ""

        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("কেমন") || lower.contains("হাই") || lower.contains("হ্যালো") -> {
                "$bengaliIntro" +
                "হ্যালো! আমি **Cherry AI** (${persona.name}) 🍒\n\n" +
                "আমি আপনাকে কীভাবে সাহায্য করতে পারি? আপনি বাংলা বা ইংরেজিতে যেকোনো প্রশ্ন করতে পারেন, কোডিং সমাধান চাইতে পারেন, বা নতুন কোনো চিন্তা শেয়ার করতে পারেন!"
            }
            lower.contains("code") || lower.contains("kotlin") || lower.contains("compose") || lower.contains("android") -> {
                "$bengaliIntro" +
                "Here is an idiomatic solution for your Android development query:\n\n" +
                "```kotlin\n" +
                "// Cherry AI Kotlin snippet\n" +
                "@Composable\n" +
                "fun SmartAiCard(title: String, onClick: () -> Unit) {\n" +
                "    ElevatedCard(\n" +
                "        onClick = onClick,\n" +
                "        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)\n" +
                "    ) {\n" +
                "        Text(text = title, style = MaterialTheme.typography.titleMedium)\n" +
                "    }\n" +
                "}\n" +
                "```\n\n" +
                "This composable adheres strictly to Jetpack Compose Material 3 standards. Let me know if you would like me to extend this with StateFlow and Coroutines!"
            }
            lower.contains("বাংলা") || lower.contains("গল্প") || lower.contains("কবিতা") -> {
                "$bengaliIntro" +
                "**চেরি ফুলের স্বপ্ন** 🌸\n\n" +
                "দূর আকাশের তারার দেশে,\n" +
                "প্রযুক্তির আলো ভাসে ভালোবেসে।\n" +
                "চেরি রঙের স্নিগ্ধ হাসিতে,\n" +
                "মন ভেসে যায় নতুন বাশিতে।\n\n" +
                "শব্দরা জোড়ে নতুন আশা,\n" +
                "ডিজিটাল যুগেও অটুট ভালোবাসা।"
            }
            else -> {
                "$bengaliIntro" +
                "**${persona.name} এর উত্তর:**\n\n" +
                "আপনার প্রশ্নটি পেয়েছি: *\"$prompt\"*\n\n" +
                "Cherry AI আপনার অনুরোধ বিশ্লেষণ করেছে। আমি আপনার যেকোনো প্রশ্নের উত্তর দিতে, কোড রিভিউ করতে, ভাষা অনুবাদ করতে এবং ক্রিয়েটিভ লেখার কাজে সম্পূর্ণ প্রস্তুত!"
            }
        }
    }
}
