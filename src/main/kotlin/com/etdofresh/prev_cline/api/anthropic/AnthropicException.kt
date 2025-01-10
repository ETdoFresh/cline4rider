package com.cline.api

class AnthropicException(
    message: String,
    val statusCode: Int? = null,
    val responseBody: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    override fun toString(): String = buildString {
        append("AnthropicException: $message")
        statusCode?.let { append(" (Status: $it)") }
        responseBody?.let { append("\nResponse: $it") }
        cause?.let { append("\nCaused by: ${it.localizedMessage ?: it.toString()}") }
    }
}
