package com.etdofresh.cline4rider.api.openai

class OpenAIException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
