package com.cline.api.openaicompatible

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OpenAICompatibleClient(
    private val apiKey: String,
    private val baseUrl: String,
    private val additionalHeaders: Map<String, String> = emptyMap()
) {
    private val client = HttpClient.newBuilder().build()
    private val mapper = ObjectMapper().registerKotlinModule()

    fun chat(request: OpenAICompatibleRequest): OpenAICompatibleResponse {
        val jsonRequest = mapper.writeValueAsString(request)
        
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
        
        // Add any additional headers specific to the service
        additionalHeaders.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        val httpRequest = requestBuilder
            .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
            .build()

        val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            val error = mapper.readValue(response.body(), OpenAICompatibleError::class.java)
            throw OpenAICompatibleException(error.error.message)
        }

        return mapper.readValue(response.body(), OpenAICompatibleResponse::class.java)
    }

    companion object {
        // Common base URLs for compatible services
        const val TOGETHER_AI_URL = "https://api.together.xyz/v1"
        const val MISTRAL_AI_URL = "https://api.mistral.ai/v1"
        const val GROQ_URL = "https://api.groq.com/v1"
    }
}
