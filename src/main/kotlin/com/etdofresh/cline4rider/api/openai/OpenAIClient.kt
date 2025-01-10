package com.etdofresh.cline4rider.api.openai

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.api.openai.OpenAIModels.ChatCompletionRequest
import com.etdofresh.cline4rider.api.openai.OpenAIModels.ChatCompletionResponse
import com.etdofresh.cline4rider.api.openai.OpenAIModels.Message
import com.etdofresh.cline4rider.api.openai.OpenAIModels.Role
import com.etdofresh.cline4rider.api.openai.OpenAIException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class OpenAIClient {
    private val client = OkHttpClient()
    private val baseUrl = "https://api.openai.com/v1"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessage(message: ClineMessage): String {
        try {
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(Message(Role.USER, message.content)),
                temperature = 0.7
            )

            val requestBody = request.toJson().toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(requestBody)
                .build()

            val response: Response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                throw OpenAIException("API request failed: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw OpenAIException("Empty response body")
            
            return responseBody
        } catch (e: Exception) {
            throw OpenAIException("Failed to send message", e)
        }
    }
}
