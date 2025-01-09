package com.cline.model

enum class MessageType {
    TASK_REQUEST,
    TASK_COMPLETE,
    ERROR,
    INFO
}

data class ClineMessage(
    val type: MessageType,
    val content: String,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        fun createError(content: String, metadata: Map<String, String> = emptyMap()) =
            ClineMessage(MessageType.ERROR, content, metadata)

        fun createInfo(content: String, metadata: Map<String, String> = emptyMap()) =
            ClineMessage(MessageType.INFO, content, metadata)

        fun createTaskRequest(content: String, metadata: Map<String, String> = emptyMap()) =
            ClineMessage(MessageType.TASK_REQUEST, content, metadata)

        fun createTaskComplete(content: String, metadata: Map<String, String> = emptyMap()) =
            ClineMessage(MessageType.TASK_COMPLETE, content, metadata)
    }
}
