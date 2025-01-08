package com.cline.model

data class ClineMessage(
    val type: MessageType,
    val content: String,
    val metadata: Map<String, String> = emptyMap()
)

enum class MessageType {
    TASK_REQUEST,
    TASK_UPDATE,
    TASK_COMPLETE,
    ERROR,
    INFO
}
