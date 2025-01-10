package com.etdofresh.cline4rider.api

import com.etdofresh.cline4rider.model.*
import com.google.gson.Gson
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service(Service.Level.PROJECT)
class ApiProvider {
    private val logger = Logger.getInstance(ApiProvider::class.java)
    private val gson = Gson()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    suspend fun sendMessage(message: String, apiKey: String): Result<ClineMessage> = withContext(Dispatchers.IO) {
        try {
            val chatRequest = ChatCompletionRequest(
                model = DEFAULT_MODEL,
                messages = listOf(ChatMessage(Role.USER.toString(), message))
            )

            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(chatRequest)))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() != 200) {
                logger.error("API request failed with status ${response.statusCode()}: ${response.body()}")
                return@withContext Result.failure(Exception("API request failed: ${response.statusCode()}"))
            }

            val completionResponse = gson.fromJson(response.body(), ChatCompletionResponse::class.java)
            val assistantMessage = completionResponse.choices.firstOrNull()?.message
                ?: throw Exception("No response message found")

            val clineMessage = ClineMessage(
                content = assistantMessage.content,
                role = Role.ASSISTANT,
                timestamp = System.currentTimeMillis()
            )

            Result.success(clineMessage)
        } catch (e: Exception) {
            logger.error("Error sending message to API", e)
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-3.5-turbo"
    }
}
