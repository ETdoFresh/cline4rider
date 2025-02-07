package com.etdofresh.cline4rider.api.anthropic

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.api.anthropic.ChatCompletionRequest
import com.etdofresh.cline4rider.api.anthropic.AnthropicException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

private val json = Json { prettyPrint = true }

private fun Any.toJson(): String = json.encodeToString(this)

class AnthropicClient {
    private val client = OkHttpClient()
    private val baseUrl = "https://api.anthropic.com/v1"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessage(message: ClineMessage): String {
        try {
            val request = ChatCompletionRequest(
                model = "claude-2",
                prompt = "\n\nHuman: ${message.content}\n\nAssistant:",
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
