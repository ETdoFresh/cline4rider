package com.etdofresh.cline4rider.api.anthropic

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.api.anthropic.AnthropicModels.CompleteRequest
import com.etdofresh.cline4rider.api.anthropic.AnthropicModels.CompleteResponse
import com.etdofresh.cline4rider.api.anthropic.AnthropicException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class AnthropicClient {
    private val client = OkHttpClient()
    private val baseUrl = "https://api.anthropic.com/v1"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessage(message: ClineMessage): String {
        try {
            val request = CompleteRequest(
                prompt = message.content,
                model = "claude-2",
                max_tokens_to_sample = 1000,
                temperature = 0.7
            )

            val requestBody = request.toJson().toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url("$baseUrl/complete")
                .post(requestBody)
                .build()

            val response: Response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                throw AnthropicException("API request failed: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw AnthropicException("Empty response body")
            
            return responseBody
        } catch (e: Exception) {
            throw AnthropicException("Failed to send message", e)
        }
    }
}
