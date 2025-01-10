package com.cline.api.openai

data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null,
    val stream: Boolean = false
)

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage
)

data class OpenAIChoice(
    val index: Int,
    val message: OpenAIMessage,
    val finish_reason: String?
)

data class OpenAIUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class OpenAIError(
    val error: OpenAIErrorDetails
)

data class OpenAIErrorDetails(
    val message: String,
    val type: String,
    val code: String? = null
)
