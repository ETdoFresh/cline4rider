package com.etdofresh.cline4rider.model

import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.XCollection

@Tag("clineMessage")
data class ClineMessage(
    @get:Attribute
    val role: Role,
    
    @get:XCollection(style = XCollection.Style.v2)
    val content: List<Content>,
    
    @get:Attribute
    val timestamp: Long,
    
    @get:XCollection(style = XCollection.Style.v2)
    val toolCalls: List<ToolCall> = emptyList(),
    
    @get:Attribute
    val tokens: Int? = null,
    
    @get:Attribute
    val cachedTokens: Int? = null,
    
    @get:Attribute
    val cost: Double? = null,
    
    @get:Attribute
    val cacheDiscount: Double? = null,
    
    @get:Attribute
    val model: String? = null
) {
    sealed class Content {
        @Tag("text")
        data class Text(
            @get:Attribute
            val text: String,
            
            @get:Attribute
            val type: String = "text",
            
            @get:Attribute
            val cacheControl: CacheControl? = null
        ) : Content()

        @Tag("imageUrl")
        data class ImageUrl(
            @get:Attribute
            val imageUrl: ImageUrlData,
            
            @get:Attribute
            val type: String = "image_url"
        ) : Content()
    }

    @Tag("imageUrlData")
    data class ImageUrlData(
        @get:Attribute
        val url: String
    )

    @Tag("cacheControl")
    data class CacheControl(
        @get:Attribute
        val type: String
    )

    enum class Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    @Tag("toolCall")
    data class ToolCall(
        @get:Attribute
        val id: String,
        
        @get:Attribute
        val name: String,
        
        @get:Attribute
        val arguments: String,
        
        @get:Attribute
        val output: String? = null
    )
}
