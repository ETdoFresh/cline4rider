package com.cline.api

data class Message(
    val role: String,
    val content: String
)

data class AnthropicRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int,
    val temperature: Double,
    val system: String? = null
)

data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    val stop_reason: String?,
    val stop_sequence: String?,
    val usage: Usage
)

data class ContentBlock(
    val type: String,
    val text: String
)

data class Usage(
    val input_tokens: Int,
    val output_tokens: Int
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}
