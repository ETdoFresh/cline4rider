package com.etdofresh.cline4rider.api.openrouter

class OpenRouterException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
