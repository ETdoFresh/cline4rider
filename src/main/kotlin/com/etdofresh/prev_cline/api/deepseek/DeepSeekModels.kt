package com.cline.api.deepseek

data class DeepSeekRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null,
    val stream: Boolean = false
)

data class DeepSeekMessage(
    val role: String,
    val content: String
)

data class DeepSeekResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<DeepSeekChoice>,
    val usage: DeepSeekUsage
)

data class DeepSeekChoice(
    val index: Int,
    val message: DeepSeekMessage,
    val finish_reason: String?
)

data class DeepSeekUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class DeepSeekError(
    val error: DeepSeekErrorDetails
)

data class DeepSeekErrorDetails(
    val message: String,
    val type: String,
    val code: String? = null
)
