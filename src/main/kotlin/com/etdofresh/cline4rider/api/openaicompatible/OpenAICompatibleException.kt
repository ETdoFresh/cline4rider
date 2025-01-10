package com.etdofresh.cline4rider.api.openaicompatible

class OpenAICompatibleException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
