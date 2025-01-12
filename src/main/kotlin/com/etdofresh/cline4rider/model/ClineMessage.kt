package com.etdofresh.cline4rider.model

data class ClineMessage(
    val role: Role,
    val content: List<Content>,
    val timestamp: Long,
    val toolCalls: List<ToolCall> = emptyList(),
    val tokens: Int? = null,
    val cachedTokens: Int? = null,
    val cost: Double? = null,
    val cacheDiscount: Double? = null,
    val model: String? = null
) {
    sealed class Content {
        data class Text(
            val text: String,
            val type: String = "text",
            val cacheControl: CacheControl? = null
        ) : Content()

        data class ImageUrl(
            val imageUrl: ImageUrlData,
            val type: String = "image_url"
        ) : Content()
    }

    data class ImageUrlData(
        val url: String
    )

    data class CacheControl(
        val type: String
    )

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
