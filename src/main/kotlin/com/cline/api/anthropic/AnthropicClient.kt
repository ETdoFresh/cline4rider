package com.cline.api

import com.cline.settings.ClineSettings
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class AnthropicClient(private val settings: ClineSettings) {
    private val gson = Gson()
    private val baseUrl = "https://api.anthropic.com/v1"
    private val timeout = Duration.ofSeconds(30)

    private fun createHttpClient(): HttpClient {
        val builder = HttpClient.newBuilder()
            .connectTimeout(timeout)

        settings.getProxy()?.let { proxy ->
            val address = proxy.address() as? InetSocketAddress
            if (address != null) {
                builder.proxy(ProxySelector.of(address))
            }
        }

        return builder.build()
    }

    suspend fun sendMessage(message: String): String = withContext(Dispatchers.IO) {
        if (settings.apiKey.isEmpty()) {
            throw AnthropicException("API key not configured", 401)
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/messages"))
            .header("x-api-key", settings.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                gson.toJson(mapOf(
                    "model" to settings.model,
                    "max_tokens" to settings.maxTokens,
                    "messages" to listOf(mapOf(
                        "role" to "user",
                        "content" to message
                    )),
                    "temperature" to settings.temperature
                ))
            ))
            .build()

        try {
            val response = createHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                throw AnthropicException("API request failed: ${response.body()}", response.statusCode())
            }
            response.body()
        } catch (e: Exception) {
            when (e) {
                is AnthropicException -> throw e
                else -> throw AnthropicException("Failed to send message: ${e.message}", 500)
            }
        }
    }

    suspend fun streamResponse(message: String): Flow<String> = flow {
        if (settings.apiKey.isEmpty()) {
            throw AnthropicException("API key not configured", 401)
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/messages"))
            .header("x-api-key", settings.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .header("accept", "text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(
                gson.toJson(mapOf(
                    "model" to settings.model,
                    "max_tokens" to settings.maxTokens,
                    "messages" to listOf(mapOf(
                        "role" to "user",
                        "content" to message
                    )),
                    "temperature" to settings.temperature,
                    "stream" to true
                ))
            ))
            .build()

        val responses = withContext(Dispatchers.IO) {
            try {
                createHttpClient().send(request, HttpResponse.BodyHandlers.ofLines()).body().toList()
            } catch (e: Exception) {
                when (e) {
                    is AnthropicException -> throw e
                    else -> throw AnthropicException("Failed to stream response: ${e.message}", 500)
                }
            }
        }

        responses.forEach { line ->
            if (line.startsWith("data: ")) {
                val data = line.substring(6)
                if (data != "[DONE]") {
                    val response = gson.fromJson(data, StreamResponse::class.java)
                    response.delta?.text?.let { emit(it) }
                }
            }
        }
    }

    private data class StreamResponse(
        val type: String,
        val delta: Delta?
    )

    private data class Delta(
        val type: String,
        val text: String?
    )
}
