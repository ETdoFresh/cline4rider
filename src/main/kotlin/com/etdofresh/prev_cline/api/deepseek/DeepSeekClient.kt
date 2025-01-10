package com.cline.api.deepseek

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class DeepSeekClient(private val apiKey: String) {
    private val client = HttpClient.newBuilder().build()
    private val mapper = ObjectMapper().registerKotlinModule()
    private val baseUrl = "https://api.deepseek.com/v1"

    fun chat(request: DeepSeekRequest): DeepSeekResponse {
        val jsonRequest = mapper.writeValueAsString(request)
        
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
            .build()

        val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            val error = mapper.readValue(response.body(), DeepSeekError::class.java)
            throw DeepSeekException(error.error.message)
        }

        return mapper.readValue(response.body(), DeepSeekResponse::class.java)
    }
}
