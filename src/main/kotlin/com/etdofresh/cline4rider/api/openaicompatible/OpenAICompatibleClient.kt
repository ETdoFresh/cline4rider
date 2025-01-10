package com.etdofresh.cline4rider.api.openaicompatible

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.api.openaicompatible.ChatCompletionRequest
import com.etdofresh.cline4rider.api.openaicompatible.ChatCompletionResponse
import com.etdofresh.cline4rider.api.openaicompatible.Message
import com.etdofresh.cline4rider.api.openaicompatible.OpenAICompatibleException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

private val json = Json { prettyPrint = true }

private fun Any.toJson(): String = json.encodeToString(this)

class OpenAICompatibleClient(private val baseUrl: String) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessage(message: ClineMessage): String {
        try {
            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(Message("user", message.content)),
                temperature = 0.7
            )

            val requestBody = request.toJson().toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .post(requestBody)
                .build()

            val response: Response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                throw OpenAICompatibleException("API request failed: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw OpenAICompatibleException("Empty response body")
            
            return responseBody
        } catch (e: Exception) {
            throw OpenAICompatibleException("Failed to send message", e)
        }
    }
}
