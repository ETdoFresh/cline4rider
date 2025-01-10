package com.etdofresh.cline4rider.api.anthropic

import kotlinx.serialization.Serializable

@Serializable
data class CompleteRequest(
    val prompt: String,
    val model: String,
    val max_tokens_to_sample: Int,
    val temperature: Double = 0.7,
    val top_p: Double? = null,
    val stop_sequences: List<String>? = null
)

@Serializable
data class CompleteResponse(
    val completion: String,
    val stop_reason: String,
    val model: String,
    val stop: String? = null,
    val log_id: String
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
