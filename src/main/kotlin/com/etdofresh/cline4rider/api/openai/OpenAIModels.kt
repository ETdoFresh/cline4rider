package com.etdofresh.cline4rider.api.openai

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null,
    val top_p: Double? = null,
    val frequency_penalty: Double? = null,
    val presence_penalty: Double? = null
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
)

@Serializable
data class Message(
    val role: Role,
    val content: String
)

@Serializable
enum class Role {
    system, user, assistant
}

@Serializable
data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

@Serializable
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)
