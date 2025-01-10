package com.etdofresh.cline4rider.api.anthropic

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val prompt: String,
    val max_tokens_to_sample: Int,
    val temperature: Double = 0.7,
    val top_p: Double? = null,
    val stop_sequences: List<String>? = null
)

@Serializable
data class ChatCompletionResponse(
    val completion: String,
    val stop_reason: String,
    val model: String
)

@Serializable
data class ErrorResponse(
    val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val type: String,
    val message: String
)
