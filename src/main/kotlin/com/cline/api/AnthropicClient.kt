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
    private val gson = Gson().newBuilder()
        .setLenient()
        .registerTypeAdapter(AnthropicResponse::class.java, object : com.google.gson.JsonDeserializer<AnthropicResponse> {
            override fun deserialize(
                json: com.google.gson.JsonElement,
                typeOfT: java.lang.reflect.Type,
                context: com.google.gson.JsonDeserializationContext
            ): AnthropicResponse {
                try {
                    if (json.isJsonObject) {
                        return context.deserialize(json, AnthropicResponse::class.java)
                    } else {
                        logger.error("Unexpected response format: $json")
                        throw com.google.gson.JsonParseException("Response is not a JSON object")
                    }
                } catch (e: Exception) {
                    logger.error("Failed to deserialize response", e)
                    throw e
                }
            }
        })
        .create()
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
            val requestBody = try {
                gson.toJson(request)
            } catch (e: Exception) {
                logger.error("Failed to serialize request", e)
                return ApiResult.Error("Failed to prepare request: ${e.message}")
            }
            
            logger.info("Sending request to Anthropic API: $requestBody")
            
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(settings.apiEndpoint))
                .header("Content-Type", "application/json")
                .header("x-api-key", settings.apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            val responseBody = response.body()
            
            if (response.statusCode() != 200) {
                logger.warn("Anthropic API error (${response.statusCode()}): $responseBody")
            } else {
                logger.info("Anthropic API response (${response.statusCode()}): $responseBody")
            }

            when (response.statusCode()) {
                200 -> try {
                    if (responseBody.isBlank()) {
                        throw AnthropicException("Empty response from API", response.statusCode(), responseBody)
                    }
                    ApiResult.Success(gson.fromJson(responseBody, AnthropicResponse::class.java))
                } catch (e: com.google.gson.JsonSyntaxException) {
                    logger.error("Failed to parse API response", e)
                    throw AnthropicException(
                        "Invalid JSON response from API",
                        response.statusCode(),
                        responseBody,
                        e
                    )
                } catch (e: Exception) {
                    when (e) {
                        is AnthropicException -> throw e
                        else -> {
                            logger.error("Unexpected error parsing API response", e)
                            throw AnthropicException(
                                "Failed to process API response: ${e.message}",
                                response.statusCode(),
                                responseBody,
                                e
                            )
                        }
                    }
                }
                401 -> throw AnthropicException(
                    "Invalid API key. Please check your settings.",
                    response.statusCode(),
                    responseBody
                )
                429 -> throw AnthropicException(
                    "Rate limit exceeded. Please try again later.",
                    response.statusCode(),
                    responseBody
                )
                500 -> throw AnthropicException(
                    "Server error. Please try again later.",
                    response.statusCode(),
                    responseBody
                )
                else -> throw AnthropicException(
                    "Unexpected error from Anthropic API",
                    response.statusCode(),
                    responseBody
                )
            }
        } catch (e: Exception) {
            logger.warn("Error in Anthropic API communication", e)
            return when (e) {
                is AnthropicException -> ApiResult.Error(e.message ?: "Unknown error", e.statusCode)
                else -> ApiResult.Error("Failed to communicate with Anthropic API: ${e.message}")
            }
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
