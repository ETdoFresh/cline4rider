package com.cline.api

import com.google.gson.annotations.SerializedName

data class Message(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
) {
    init {
        require(role in setOf("user", "assistant", "system")) {
            "Invalid role: $role. Must be one of: user, assistant, system"
        }
        require(content.isNotBlank()) {
            "Message content cannot be blank"
        }
    }
}

data class AnthropicRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<Message>,
    @SerializedName("max_tokens")
    val max_tokens: Int,
    @SerializedName("temperature")
    val temperature: Double,
    @SerializedName("system")
    val system: String? = null,
    @SerializedName("transforms")
    val transforms: List<String> = listOf("middle-out"),
    @SerializedName("route")
    val route: String = "fallback"
) {
    init {
        require(messages.isNotEmpty()) {
            "Messages list cannot be empty"
        }
        require(max_tokens > 0) {
            "max_tokens must be greater than 0"
        }
        require(temperature in 0.0..1.0) {
            "temperature must be between 0.0 and 1.0"
        }
    }
}

data class AnthropicResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("object")
    val type: String,
    @SerializedName("created")
    val created: Long,
    @SerializedName("model")
    val model: String,
    @SerializedName("choices")
    val choices: List<Choice>,
    @SerializedName("usage")
    val usage: Usage
)

data class Choice(
    @SerializedName("index")
    val index: Int,
    @SerializedName("message")
    val message: Message,
    @SerializedName("finish_reason")
    val finish_reason: String?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val input_tokens: Int,
    @SerializedName("completion_tokens")
    val output_tokens: Int,
    @SerializedName("total_tokens")
    val total_tokens: Int
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}
