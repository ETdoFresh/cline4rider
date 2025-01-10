package com.etdofresh.cline4rider.api.deepseek

class DeepSeekException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
