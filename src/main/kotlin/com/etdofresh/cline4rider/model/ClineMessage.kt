package com.etdofresh.cline4rider.model

data class ClineMessage(
    val role: Role,
    val content: String,
    val timestamp: Long,
    val toolCalls: List<ToolCall> = emptyList()
) {
    enum class Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    data class ToolCall(
        val id: String,
        val name: String,
        val arguments: String,
        val output: String? = null
    )
}
