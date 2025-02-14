package com.etdofresh.cline4rider.api.openrouter

import kotlinx.serialization.Serializable

@Serializable
data class GenerationStatsResponse(
    val data: GenerationStats
)

@Serializable
data class GenerationStats(
    val id: String,
    val model: String,
    val streamed: Boolean,
    val generation_time: Int,
    val created_at: String,
    val tokens_prompt: Int,
    val tokens_completion: Int,
    val native_tokens_prompt: Int,
    val native_tokens_completion: Int,
    val total_cost: Double,
    val cache_discount: Double? = null,
    val origin: String? = null
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)

@Serializable
data class Message(
    val role: String,
    val content: String,
    val cache_control: CacheControl? = null
)

@Serializable
data class CacheControl(
    val type: String = "ephemeral"
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String,
    val usage: Usage? = null,
    val cache_discount: Double? = null
)

@Serializable
data class Choice(
    val message: Message,
    val finish_reason: String? = null
)

@Serializable
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

@Serializable
data class ChatCompletionChunk(
    val id: String,
    val choices: List<ChunkChoice>,
    val created: Long,
    val model: String,
    val cache_discount: Double? = null
)

@Serializable
data class ChunkChoice(
    val delta: DeltaContent,
    val finish_reason: String? = null
)

@Serializable
data class DeltaContent(
    val content: String? = null,
    val role: String? = null
)
