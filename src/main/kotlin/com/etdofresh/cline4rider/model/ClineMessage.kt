package com.etdofresh.cline4rider.model

import java.time.Instant

data class ClineMessage(
    val role: Role,
    val content: String,
    val timestamp: Long = Instant.now().toEpochMilli(),
    val messageType: MessageType = MessageType.NORMAL
) {
    enum class Role {
        USER,
        ASSISTANT,
        SYSTEM
    }

    enum class MessageType {
        NORMAL,
        ERROR,
        WARNING,
        INFO
    }
}
