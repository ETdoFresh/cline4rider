package com.etdofresh.cline4rider.model

data class ClineMessage(
    val content: String,
    val timestamp: Long,
    val messageType: MessageType
) {
    enum class MessageType {
        TASK_REQUEST,
        TASK_COMPLETE,
        TASK_ERROR
    }
}
