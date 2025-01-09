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
    val system: String? = null
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
    @SerializedName("type")
    val type: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: List<ContentBlock>,
    @SerializedName("model")
    val model: String,
    @SerializedName("stop_reason")
    val stop_reason: String?,
    @SerializedName("stop_sequence")
    val stop_sequence: String?,
    @SerializedName("usage")
    val usage: Usage
)

data class ContentBlock(
    @SerializedName("type")
    val type: String,
    @SerializedName("text")
    val text: String
)

data class Usage(
    @SerializedName("input_tokens")
    val input_tokens: Int,
    @SerializedName("output_tokens")
    val output_tokens: Int
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}
