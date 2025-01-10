package com.cline.api.openrouter

data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null,
    val stream: Boolean = false
)

data class OpenRouterMessage(
    val role: String,
    val content: String
)

data class OpenRouterResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<OpenRouterChoice>,
    val usage: OpenRouterUsage
)

data class OpenRouterChoice(
    val index: Int,
    val message: OpenRouterMessage,
    val finish_reason: String?
)

data class OpenRouterUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class OpenRouterError(
    val error: OpenRouterErrorDetails
)

data class OpenRouterErrorDetails(
    val message: String,
    val type: String,
    val code: String? = null
)
