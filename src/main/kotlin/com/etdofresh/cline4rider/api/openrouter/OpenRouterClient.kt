package com.etdofresh.cline4rider.api.openrouter

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.settings.ClineSettings
import com.etdofresh.cline4rider.api.ResponseStats
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.io.IOException
import java.util.concurrent.TimeUnit

private val json = Json { 
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
}

private inline fun <reified T> T.toJson(): String = json.encodeToString(this)

class OpenRouterClient(private val settings: ClineSettings) {
    private val logger = Logger.getInstance(OpenRouterClient::class.java)
    private val client = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun fetchGenerationStats(id: String, apiKey: String): ResponseStats? {
        return try {
            logger.debug("Fetching generation stats for id: $id")
            
            val baseUrl = settings.state.openRouterBaseUrl.removeSuffix("/")
            val statsResponse = client.newCall(
                Request.Builder()
                    .url("$baseUrl/generation?id=$id")
                    .header("Authorization", "Bearer $apiKey")
                    .build()
            ).execute()
            
            if (statsResponse.isSuccessful) {
                val responseBody = statsResponse.body?.string()
                logger.debug("Generation stats response: $responseBody")
                if (!responseBody.isNullOrBlank()) {
                    try {
                        val response = json.decodeFromString<GenerationStatsResponse>(responseBody)
                        val stats = response.data
                        if (stats.total_cost > 0.0) {
                            logger.debug("Successfully parsed stats with cost: ${stats.total_cost}")
                            ResponseStats(
                                total_cost = stats.total_cost,
                                tokens_prompt = stats.tokens_prompt,
                                tokens_completion = stats.tokens_completion,
                                native_tokens_prompt = stats.native_tokens_prompt,
                                native_tokens_completion = stats.native_tokens_completion,
                                cache_discount = stats.cache_discount
                            )
                        } else {
                            logger.debug("Stats had zero cost")
                            null
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to parse generation stats", e)
                        null
                    }
                } else {
                    logger.debug("Empty response body")
                    null
                }
            } else {
                logger.warn("Failed to fetch generation stats: ${statsResponse.code}")
                logger.warn("Error response: ${statsResponse.body?.string()}")
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch generation stats", e)
            null
        }
    }

    fun sendMessages(messages: List<ClineMessage>, onChunk: ((String, ResponseStats?) -> Unit)? = null): String {
        val apiKey = settings.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            throw OpenRouterException("API key is not configured. Please configure your API key in Settings | Tools | Cline")
        }
        
        if (messages.isEmpty()) {
            throw OpenRouterException("Message list cannot be empty")
        }
        
        messages.forEach { message ->
            if (message.content.isEmpty()) {
                throw OpenRouterException("Message content cannot be empty")
            }
            // Check if any text content is blank
            if (message.content.filterIsInstance<ClineMessage.Content.Text>().all { it.text.isBlank() }) {
                throw OpenRouterException("Message text content cannot be blank")
            }
        }

        try {
            val request = ChatCompletionRequest(
                model = settings.state.model ?: "openai/gpt-3.5-turbo",
                messages = messages.map { msg ->
                    val textContent = msg.content.filterIsInstance<ClineMessage.Content.Text>()
                        .joinToString("\n") { it.text }
                    Message(
                        role = when (msg.role) {
                            ClineMessage.Role.USER -> "user"
                            ClineMessage.Role.ASSISTANT -> "assistant"
                            ClineMessage.Role.SYSTEM -> "system"
                            else -> "user"
                        },
                        content = textContent,
                        cache_control = if (textContent.length > 1024) CacheControl() else null
                    )
                },
                temperature = settings.state.temperature ?: 0.7,
                stream = onChunk != null
            )

            val requestBody = request.toJson().toRequestBody(jsonMediaType)
            val httpRequest = Request.Builder()
                .url("${settings.state.openRouterBaseUrl.removeSuffix("/")}/chat/completions")
                .post(requestBody)
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://github.com/etdofresh/cline4rider")
                .header("X-Title", "Cline for Rider")
                .build()

            if (onChunk != null) {
                var fullResponse = StringBuilder()
                var lastChunkId: String? = null
                var streamComplete = false
                
                client.newCall(httpRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                            throw OpenRouterException("Streaming request failed", e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string()
                            logger.error("API request failed: ${response.code} ${response.message}")
                            logger.error("Error body: $errorBody")
                            
                            val errorMessage = when (response.code) {
                                401 -> "Invalid API key. Please check your API key in Settings | Tools | Cline"
                                403 -> "Access denied. Please verify your API key has the correct permissions"
                                429 -> "Rate limit exceeded. Please try again later"
                                500, 502, 503, 504 -> "Server error (${response.code}). Please try again later"
                                else -> try {
                                    val errorJson = errorBody?.let { json.decodeFromString<Map<String, Any>>(it) }
                                    errorJson?.get("error")?.toString() ?: "API request failed: ${response.code}"
                                } catch (e: Exception) {
                                    "API request failed: ${response.code} - $errorBody"
                                }
                            }
                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                throw OpenRouterException(errorMessage)
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
                                            if (data == "[DONE]") {
                                                streamComplete = true
                                                // Fetch stats in a separate thread with UI update
                                                lastChunkId?.let { id ->
                                                    logger.debug("Stream complete, waiting before fetching stats for ID: $id")
                                                    Thread {
                                                        // Add a 1000ms delay before fetching stats
                                                        Thread.sleep(1000)
                                                        val stats = fetchGenerationStats(id, apiKey)
                                                        // Always invoke callback with final stats, even if null
                                                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                                            onChunk("", stats)
                                                        }
                                                    }.start()
                                                }
                                                continue
                                            }
                                            
                                            try {
                                                val chunk = json.decodeFromString<ChatCompletionChunk>(data)
                                                lastChunkId = chunk.id
                                                logger.debug("Got chunk ID: ${chunk.id}")
                                                val content = chunk.choices.firstOrNull()?.delta?.content
                                                if (content != null) {
                                                    fullResponse.append(content)
                                                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                                        onChunk(content, null)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                logger.warn("Failed to parse chunk", e)
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
                    val errorBody = response.body?.string()
                    logger.error("API request failed: ${response.code} ${response.message}")
                    logger.error("Error body: $errorBody")
                    
                    val errorMessage = when (response.code) {
                        401 -> "Invalid API key. Please check your API key in Settings | Tools | Cline"
                        403 -> "Access denied. Please verify your API key has the correct permissions"
                        429 -> "Rate limit exceeded. Please try again later"
                        500, 502, 503, 504 -> "Server error (${response.code}). Please try again later"
                        else -> try {
                            // Try to parse error message from response body
                            val errorJson = errorBody?.let { json.decodeFromString<Map<String, Any>>(it) }
                            errorJson?.get("error")?.toString() ?: "API request failed: ${response.code}"
                        } catch (e: Exception) {
                            "API request failed: ${response.code} - $errorBody"
                        }
                    }
                    throw OpenRouterException(errorMessage)
                }

                val responseBody = response.body?.string()
                    ?: throw OpenRouterException("Empty response body")
                
                val parsedResponse = json.decodeFromString<ChatCompletionResponse>(responseBody)
                val content = parsedResponse.choices.firstOrNull()?.message?.content
                    ?: throw OpenRouterException("No response message found")
                
                // Get stats for non-streaming response
                logger.debug("Non-streaming response complete, fetching stats for ID: ${parsedResponse.id}")
                val stats = fetchGenerationStats(parsedResponse.id, apiKey)
                if (stats != null) {
                    onChunk?.invoke(content, stats)
                }
                
                return content
            }
        } catch (e: Exception) {
            throw OpenRouterException("Failed to send message", e)
        }
    }
}
