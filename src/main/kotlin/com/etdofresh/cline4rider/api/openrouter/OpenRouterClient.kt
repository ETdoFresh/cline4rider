package com.etdofresh.cline4rider.api.openrouter

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.api.openrouter.OpenRouterModels.ChatCompletionRequest
import com.etdofresh.cline4rider.api.openrouter.OpenRouterModels.ChatCompletionResponse
import com.etdofresh.cline4rider.api.openrouter.OpenRouterModels.Message
import com.etdofresh.cline4rider.api.openrouter.OpenRouterException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class OpenRouterClient {
    private val client = OkHttpClient()
    private val baseUrl = "https://openrouter.ai/api/v1"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessage(message: ClineMessage): String {
        try {
            val request = ChatCompletionRequest(
                model = "openai/gpt-3.5-turbo",
                messages = listOf(Message("user", message.content)),
                temperature = 0.7
            )

            val requestBody = request.toJson().toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(requestBody)
                .build()

            val response: Response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                throw OpenRouterException("API request failed: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw OpenRouterException("Empty response body")
            
            return responseBody
        } catch (e: Exception) {
            throw OpenRouterException("Failed to send message", e)
        }
    }
}
