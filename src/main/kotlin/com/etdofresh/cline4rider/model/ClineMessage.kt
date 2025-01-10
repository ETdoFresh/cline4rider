package com.etdofresh.cline4rider.model

data class ClineMessage(
    val role: Role,
    val content: String,
    val timestamp: Long
) {
    enum class Role {
        SYSTEM,
        USER,
        ASSISTANT
    }
}
