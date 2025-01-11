package com.etdofresh.cline4rider.api

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.model.ChatCompletionRequest
import com.etdofresh.cline4rider.model.ChatCompletionResponse
import com.etdofresh.cline4rider.model.ChatMessage
import com.etdofresh.cline4rider.model.ClineMessage.Role
import com.etdofresh.cline4rider.settings.ClineSettings
import com.etdofresh.cline4rider.api.openrouter.OpenRouterClient
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

    suspend fun sendMessages(messages: List<ClineMessage>, apiKey: String, provider: ClineSettings.Provider, settings: ClineSettings): Result<ClineMessage> = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                ClineSettings.Provider.OPENROUTER -> {
                    val openRouterClient = OpenRouterClient(settings)
                    val responseContent = openRouterClient.sendMessages(messages)
                    
                    Result.success(ClineMessage(
                        content = responseContent,
                        role = Role.ASSISTANT,
                        timestamp = System.currentTimeMillis()
                    ))
                }
                else -> {
                    val chatRequest = ChatCompletionRequest(
                        model = DEFAULT_MODEL,
                        messages = messages.map { ChatMessage(it.role, it.content) }
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

                    Result.success(ClineMessage(
                        content = assistantMessage.content,
                        role = Role.ASSISTANT,
                        timestamp = System.currentTimeMillis()
                    ))
                }
            }
        } catch (e: Exception) {
            logger.error("Error sending message to API", e)
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-3.5-turbo"
    }
}
