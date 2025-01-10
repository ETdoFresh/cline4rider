package com.etdofresh.cline4rider.model

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val stop: List<String>? = null
)

data class ChatMessage(
    val role: Role,
    val content: String
)

data class ChatCompletionResponse(
    val id: String,
    val choices: List<ChatCompletionChoice>,
    val usage: ChatUsage?
)

data class ChatCompletionChoice(
    val message: ChatMessage,
    val finishReason: String?
)

data class ChatUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

enum class Role {
    USER,
    ASSISTANT,
    SYSTEM
}
