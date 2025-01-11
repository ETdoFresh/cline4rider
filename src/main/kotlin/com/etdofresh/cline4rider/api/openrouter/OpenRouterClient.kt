package com.etdofresh.cline4rider.api.openrouter

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.settings.ClineSettings
import com.etdofresh.cline4rider.api.openrouter.ChatCompletionRequest
import com.etdofresh.cline4rider.api.openrouter.ChatCompletionResponse
import com.etdofresh.cline4rider.api.openrouter.Message
import com.etdofresh.cline4rider.api.openrouter.OpenRouterException
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.io.IOException

private val json = Json { 
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
}


private inline fun <reified T> T.toJson(): String = json.encodeToString(this)

class OpenRouterClient(private val settings: ClineSettings) {
    private val logger = Logger.getInstance(OpenRouterClient::class.java)
    private val client = OkHttpClient()
    private val baseUrl = "https://openrouter.ai/api/v1"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessages(messages: List<ClineMessage>, onChunk: ((String) -> Unit)? = null): String {
        val apiKey = settings.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            throw OpenRouterException("OpenRouter API key is not configured")
        }
        
        // Validate messages
        if (messages.isEmpty()) {
            throw OpenRouterException("Message list cannot be empty")
        }
        
        // Validate message content
        messages.forEach { message ->
            if (message.content.isBlank()) {
                throw OpenRouterException("Message content cannot be blank")
            }
        }

        try {
            val request = ChatCompletionRequest(
                model = settings.state.model ?: "openai/gpt-3.5-turbo",
                messages = messages.map { 
                    Message(
                        role = when (it.role) {
                            ClineMessage.Role.USER -> "user"
                            ClineMessage.Role.ASSISTANT -> "assistant"
                            ClineMessage.Role.SYSTEM -> "system"
                            else -> "user"
                        },
                        content = it.content
                    )
                },
                temperature = settings.state.temperature ?: 0.7,
                stream = onChunk != null
            )

            val requestBody = request.toJson().toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(requestBody)
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://github.com/etdofresh/cline4rider")
                .header("X-Title", "Cline for Rider")
                .build()

            if (onChunk != null) {
                // Streaming mode
                var fullResponse = StringBuilder()
                client.newCall(httpRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                            throw OpenRouterException("Streaming request failed", e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                throw OpenRouterException("API request failed: ${response.code}")
                            }
                            return
                        }

                        try {
                            response.body?.source()?.use { source ->
                                val buffer = Buffer()
                                while (!source.exhausted()) {
                                    source.read(buffer, 8192)
                                    var line: String?
                                    while (buffer.readUtf8Line().also { line = it } != null) {
                                        if (line!!.startsWith("data: ")) {
                                            val data = line!!.substring(6)
                                            if (data == "[DONE]") continue
                                            try {
                                                val chunk = json.decodeFromString<ChatCompletionChunk>(data)
                                                val content = chunk.choices.firstOrNull()?.delta?.content
                                                if (content != null) {
                                                    fullResponse.append(content)
                                                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                                        onChunk(content)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // Skip malformed chunks
                                                logger.warn("Failed to parse chunk: $data")
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                throw OpenRouterException("Error processing stream", e)
                            }
                        }
                    }
                })
                return "" // Actual response will be delivered via callback
            } else {
                // Non-streaming mode
                val response: Response = client.newCall(httpRequest).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "No error details"
                    throw OpenRouterException("""
                        API request failed: ${response.code} ${response.message}
                        Error details: $errorBody
                    """.trimIndent())
                }

                val responseBody = response.body?.string()
                    ?: throw OpenRouterException("Empty response body")
                
                val parsedResponse = json.decodeFromString<ChatCompletionResponse>(responseBody)
                return parsedResponse.choices.firstOrNull()?.message?.content
                    ?: throw OpenRouterException("No response message found")
            }
        } catch (e: Exception) {
            throw OpenRouterException("Failed to send message", e)
        }
    }
}
