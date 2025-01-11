package com.etdofresh.cline4rider.api.openrouter

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.settings.ClineSettings
import com.etdofresh.cline4rider.api.openrouter.ChatCompletionRequest
import com.etdofresh.cline4rider.api.openrouter.ChatCompletionResponse
import com.etdofresh.cline4rider.api.openrouter.Message
import com.etdofresh.cline4rider.api.openrouter.OpenRouterException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

private val json = Json { 
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
}


private inline fun <reified T> T.toJson(): String = json.encodeToString(this)

class OpenRouterClient(private val settings: ClineSettings) {
    private val client = OkHttpClient()
    private val baseUrl = "https://openrouter.ai/api/v1"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessages(messages: List<ClineMessage>): String {
        val apiKey = settings.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            throw OpenRouterException("OpenRouter API key is not configured")
        }
        try {
            val request = ChatCompletionRequest(
                model = "openai/gpt-3.5-turbo",
                messages = messages.map { Message(it.role.toString().lowercase(), it.content) },
                temperature = 0.7
            )

            val requestBody = request.toJson().toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(requestBody)
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://github.com/etdofresh/cline4rider")
                .header("X-Title", "Cline for Rider")
                .build()

            val response: Response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                throw OpenRouterException("API request failed: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw OpenRouterException("Empty response body")
            
            val parsedResponse = json.decodeFromString<ChatCompletionResponse>(responseBody)
            return parsedResponse.choices.firstOrNull()?.message?.content
                ?: throw OpenRouterException("No response message found")
        } catch (e: Exception) {
            throw OpenRouterException("Failed to send message", e)
        }
    }
}
