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
                    val jsonStr = json.toString()
                    if (jsonStr.trim().startsWith("<!DOCTYPE") || jsonStr.trim().startsWith("<html")) {
                        logger.error("Received HTML response instead of JSON")
                        throw com.google.gson.JsonParseException("Received HTML response instead of JSON. This usually indicates an API configuration issue.")
                    }
                    
                    if (!json.isJsonObject) {
                        logger.error("Response is not a JSON object: $json")
                        throw com.google.gson.JsonParseException("Response is not a JSON object")
                    }
                    
                    val jsonObject = json.asJsonObject
                    
                    // Manual deserialization to avoid recursion
                    val id = jsonObject.get("id").asString
                    val type = jsonObject.get("object").asString
                    val created = jsonObject.get("created").asLong
                    val model = jsonObject.get("model").asString
                    
                    val choicesArray = jsonObject.getAsJsonArray("choices")
                    val choices = choicesArray.map { choiceElement ->
                        val choiceObj = choiceElement.asJsonObject
                        val messageObj = choiceObj.getAsJsonObject("message")
                        Choice(
                            index = choiceObj.get("index").asInt,
                            message = Message(
                                role = messageObj.get("role").asString,
                                content = messageObj.get("content").asString
                            ),
                            finish_reason = choiceObj.get("finish_reason")?.asString
                        )
                    }
                    
                    val usageObj = jsonObject.getAsJsonObject("usage")
                    val usage = Usage(
                        input_tokens = usageObj.get("prompt_tokens").asInt,
                        output_tokens = usageObj.get("completion_tokens").asInt,
                        total_tokens = usageObj.get("total_tokens").asInt
                    )
                    
                    return AnthropicResponse(
                        id = id,
                        type = type,
                        created = created,
                        model = model,
                        choices = choices,
                        usage = usage
                    )
                } catch (e: Exception) {
                    when (e) {
                        is com.google.gson.JsonParseException -> throw e
                        else -> {
                            logger.error("Failed to deserialize response", e)
                            throw com.google.gson.JsonParseException("Failed to parse API response: ${e.message}", e)
                        }
                    }
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
            
            logger.info("Sending request to OpenRouter API: $requestBody")
            
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(settings.apiEndpoint))
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://github.com/ethangarcia/cline4rider")
                .header("Authorization", "Bearer ${settings.apiKey}")
                .header("X-Title", "Cline4Rider")
                .header("User-Agent", "Cline4Rider/1.0.0")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            val responseBody = response.body()
            
            // Check for HTML response before processing
            if (responseBody.trim().startsWith("<!DOCTYPE") || responseBody.trim().startsWith("<html")) {
                logger.error("Received HTML response from API")
                throw AnthropicException(
                    "Received HTML response from API. Please check your API endpoint configuration.",
                    response.statusCode(),
                    responseBody
                )
            }

            if (response.statusCode() != 200) {
                logger.warn("OpenRouter API error (${response.statusCode()}): $responseBody")
                
                // Try to parse error message from JSON response
                try {
                    val errorJson = gson.fromJson(responseBody, Map::class.java)
                    val errorMessage = (errorJson["error"] as? Map<*, *>)?.get("message") as? String
                        ?: errorJson["error"] as? String
                        ?: "Unknown error"
                    throw AnthropicException(errorMessage, response.statusCode(), responseBody)
                } catch (e: Exception) {
                    logger.warn("Failed to parse error response", e)
                }
            }

            try {
                if (responseBody.isBlank()) {
                    throw AnthropicException("Empty response from API", response.statusCode(), responseBody)
                }
                
                val response = gson.fromJson(responseBody, AnthropicResponse::class.java)
                if (response.choices.isEmpty()) {
                    throw AnthropicException(
                        "API response contained no choices",
                        200,
                        responseBody
                    )
                }
                return ApiResult.Success(response)
            } catch (e: com.google.gson.JsonSyntaxException) {
                logger.error("Failed to parse API response", e)
                throw AnthropicException(
                    "Invalid JSON response from API. Response: ${responseBody.take(500)}...",
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
        } catch (e: Exception) {
            logger.warn("Error in OpenRouter API communication", e)
            return when (e) {
                is AnthropicException -> ApiResult.Error(e.message ?: "Unknown error", e.statusCode)
                else -> ApiResult.Error("Failed to communicate with OpenRouter API: ${e.message}")
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
