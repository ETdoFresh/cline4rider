package com.etdofresh.cline4rider.api.deepseek

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.api.deepseek.DeepSeekModels.ChatCompletionRequest
import com.etdofresh.cline4rider.api.deepseek.DeepSeekModels.ChatCompletionResponse
import com.etdofresh.cline4rider.api.deepseek.DeepSeekModels.Message
import com.etdofresh.cline4rider.api.deepseek.DeepSeekException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class DeepSeekClient {
    private val client = OkHttpClient()
    private val baseUrl = "https://api.deepseek.com/v1"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessage(message: ClineMessage): String {
        try {
            val request = ChatCompletionRequest(
                model = "deepseek-chat",
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
                throw DeepSeekException("API request failed: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw DeepSeekException("Empty response body")
            
            return responseBody
        } catch (e: Exception) {
            throw DeepSeekException("Failed to send message", e)
        }
    }
}
