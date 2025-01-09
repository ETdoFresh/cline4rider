package com.cline.api

import com.cline.settings.ClineSettings
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class AnthropicClient {
    private val logger = Logger.getInstance(AnthropicClient::class.java)
    val settings = ClineSettings.getInstance()
    private val gson = Gson()
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    fun sendMessage(
        messages: List<Message>,
        systemPrompt: String? = null
    ): ApiResult<AnthropicResponse> {
        if (settings.apiKey.isEmpty()) {
            return ApiResult.Error("API key not configured. Please set it in the settings.")
        }

        val systemContent = systemPrompt ?: run {
            val file = File(settings.systemPromptPath)
            if (file.exists()) file.readText() else null
        }

        val request = AnthropicRequest(
            model = settings.model,
            messages = messages,
            max_tokens = settings.maxTokens,
            temperature = settings.temperature,
            system = systemContent
        )

        return try {
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(settings.apiEndpoint))
                .header("Content-Type", "application/json")
                .header("x-api-key", settings.apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .build()

            val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            
            when (response.statusCode()) {
                200 -> ApiResult.Success(gson.fromJson(response.body(), AnthropicResponse::class.java))
                401 -> ApiResult.Error("Invalid API key. Please check your settings.", 401)
                429 -> ApiResult.Error("Rate limit exceeded. Please try again later.", 429)
                500 -> ApiResult.Error("Server error. Please try again later.", 500)
                else -> ApiResult.Error(
                    "Unexpected error: ${response.statusCode()} - ${response.body()}",
                    response.statusCode()
                )
            }
        } catch (e: Exception) {
            logger.warn("Error sending message to Anthropic API", e)
            ApiResult.Error("Failed to communicate with Anthropic API: ${e.message}")
        }
    }

    companion object {
        private var instance: AnthropicClient? = null

        fun getInstance(): AnthropicClient {
            if (instance == null) {
                instance = AnthropicClient()
            }
            return instance!!
        }
    }
}
