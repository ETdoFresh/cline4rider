package com.cline.api.openrouter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OpenRouterClient(
    private val apiKey: String,
    private val appName: String = "Cline4Rider"
) {
    private val client = HttpClient.newBuilder().build()
    private val mapper = ObjectMapper().registerKotlinModule()
    private val baseUrl = "https://openrouter.ai/api/v1"

    fun chat(request: OpenRouterRequest): OpenRouterResponse {
        val jsonRequest = mapper.writeValueAsString(request)
        
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://github.com/ETdoFresh/cline4rider")
            .header("X-Title", appName)
            .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
            .build()

        val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            val error = mapper.readValue(response.body(), OpenRouterError::class.java)
            throw OpenRouterException(error.error.message)
        }

        return mapper.readValue(response.body(), OpenRouterResponse::class.java)
    }
}
