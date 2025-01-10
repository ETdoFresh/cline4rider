package com.cline.api.openaicompatible

data class OpenAICompatibleRequest(
    val model: String,
    val messages: List<OpenAICompatibleMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null,
    val stream: Boolean = false
)

data class OpenAICompatibleMessage(
    val role: String,
    val content: String
)

data class OpenAICompatibleResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAICompatibleChoice>,
    val usage: OpenAICompatibleUsage
)

data class OpenAICompatibleChoice(
    val index: Int,
    val message: OpenAICompatibleMessage,
    val finish_reason: String?
)

data class OpenAICompatibleUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class OpenAICompatibleError(
    val error: OpenAICompatibleErrorDetails
)

data class OpenAICompatibleErrorDetails(
    val message: String,
    val type: String,
    val code: String? = null
)
