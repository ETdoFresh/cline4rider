package com.cline.api

class AnthropicException(
    message: String,
    val statusCode: Int? = null,
    val responseBody: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    override fun toString(): String {
        return buildString {
            append("AnthropicException: $message")
            if (statusCode != null) append(" (Status: $statusCode)")
            if (responseBody != null) append("\nResponse: $responseBody")
            if (cause != null) append("\nCaused by: ${cause.message}")
        }
    }
}
