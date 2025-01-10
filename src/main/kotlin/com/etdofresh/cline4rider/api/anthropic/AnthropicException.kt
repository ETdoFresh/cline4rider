package com.etdofresh.cline4rider.api.anthropic

class AnthropicException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
