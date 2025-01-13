package com.etdofresh.cline4rider.persistence

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.model.ClineMessage.Role
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

@State(
    name = "ChatHistory",
    storages = [Storage("cline-chat-history.xml")]
)
class ChatHistory : PersistentStateComponent<ChatHistory> {
    @XCollection
    private var conversations: MutableList<Conversation> = mutableListOf()
    private var currentConversationId: String? = null

    companion object {
        private const val PAGE_SIZE = 10
        fun getInstance(project: Project): ChatHistory = project.service()
    }

    @Tag("conversation")
    class Conversation {
        @get:Attribute
        var id: String = ""
        
        @get:Attribute
        var timestamp: Long = 0
        
        @get:XCollection(style = XCollection.Style.v2)
        var messages: MutableList<SerializableMessage> = mutableListOf()

        constructor() // Required for serialization

        constructor(id: String, timestamp: Long) {
            this.id = id
            this.timestamp = timestamp
        }
    }

    fun startNewConversation(): String {
        val conversationId = System.currentTimeMillis().toString()
        currentConversationId = conversationId
        val conversation = Conversation(conversationId, System.currentTimeMillis())
        conversations.add(conversation)
        return conversationId
    }

    fun addMessage(conversationId: String, message: ClineMessage) {
        conversations.find { it.id == conversationId }?.let { conversation ->
            when {
                // For assistant messages, always try to update the last message if it's from assistant
                message.role == ClineMessage.Role.ASSISTANT && 
                conversation.messages.isNotEmpty() && 
                conversation.messages.last().role == "ASSISTANT" -> {
                    // Update existing assistant message with all properties
                    val updatedMessage = SerializableMessage.fromClineMessage(message)
                    conversation.messages[conversation.messages.lastIndex] = updatedMessage
                }
                
                // For non-assistant messages or if last message isn't from assistant, add as new
                else -> {
                    // Only add if it's not a duplicate (check role and content)
                    val isDuplicate = conversation.messages.any { msg -> 
                        msg.role == message.role.toString() && 
                        msg.content.filterIsInstance<SerializableContent.Text>().map { it.text } == 
                        message.content.filterIsInstance<ClineMessage.Content.Text>().map { it.text }
                    }
                    if (!isDuplicate) {
                        val newMessage = SerializableMessage.fromClineMessage(message)
                        conversation.messages.add(newMessage)
                    }
                }
            }
            // Update conversation timestamp to latest message
            conversation.timestamp = message.timestamp
            // Sort conversations by timestamp
            conversations.sortByDescending { it.timestamp }
        }
    }

    override fun getState(): ChatHistory {
        return this
    }

    override fun loadState(state: ChatHistory) {
        // Clear existing conversations
        conversations.clear()
        conversations.addAll(state.conversations)
        currentConversationId = state.currentConversationId
        
        // Sort conversations after loading
        conversations.sortByDescending { it.timestamp }
    }

    fun saveState() {
        try {
            // Use a background task to avoid UI freezes
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    // Use invokeLater to ensure we're on the EDT when saving settings
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        try {
                            com.intellij.openapi.application.ApplicationManager.getApplication().saveSettings()
                        } catch (e: Exception) {
                            // Log error but don't rethrow to avoid crashing the UI
                            com.intellij.openapi.diagnostic.Logger.getInstance(ChatHistory::class.java)
                                .warn("Failed to save chat history", e)
                        }
                    }
                } catch (e: Exception) {
                    // Log error but don't rethrow to avoid crashing the UI
                    com.intellij.openapi.diagnostic.Logger.getInstance(ChatHistory::class.java)
                        .warn("Failed to save chat history", e)
                }
            }
        } catch (e: Exception) {
            // Log error but don't rethrow to avoid crashing the UI
            com.intellij.openapi.diagnostic.Logger.getInstance(ChatHistory::class.java)
                .warn("Failed to save chat history", e)
        }
    }

    fun getConversationMessages(conversationId: String): List<ClineMessage> {
        return conversations.find { it.id == conversationId }?.let { conversation ->
            conversation.messages.map { it.toClineMessage() }
        } ?: emptyList()
    }

    fun getRecentConversations(offset: Int = 0): List<Conversation> {
        // Ensure conversations are sorted by timestamp
        conversations.sortByDescending { it.timestamp }
        return conversations.drop(offset).take(PAGE_SIZE)
    }

    fun hasMoreConversations(offset: Int): Boolean {
        return conversations.size > offset + PAGE_SIZE
    }

    fun clearConversation(conversationId: String) {
        conversations.find { it.id == conversationId }?.messages?.clear()
    }

    fun deleteConversation(conversationId: String) {
        conversations.removeIf { it.id == conversationId }
        if (conversationId == currentConversationId) {
            currentConversationId = null
        }
    }

    fun deleteMessage(conversationId: String, timestamp: Long) {
        conversations.find { it.id == conversationId }?.let { conversation ->
            conversation.messages.removeIf { it.timestamp == timestamp }
            // If conversation is empty after deletion, remove it
            if (conversation.messages.isEmpty()) {
                deleteConversation(conversationId)
            }
            saveState()
        }
    }

    @Tag("message")
    class SerializableMessage {
        @get:Attribute
        var role: String = ""
        
        @get:XCollection(style = XCollection.Style.v2, elementTypes = [SerializableContent.Text::class, SerializableContent.ImageUrl::class])
        var content: MutableList<SerializableContent> = mutableListOf()

        
        @get:Attribute
        var timestamp: Long = 0
        
        @get:XCollection(style = XCollection.Style.v2, elementTypes = [SerializableToolCall::class])
        var toolCalls: MutableList<SerializableToolCall> = mutableListOf()

        @get:Attribute
        var cost: Double? = null

        @get:Attribute
        var cacheDiscount: Double? = null

        companion object {
            fun fromClineMessage(message: ClineMessage): SerializableMessage {
                return SerializableMessage().apply {
                    role = message.role.toString()
                    content = message.content.map { content ->
                        when (content) {
                            is ClineMessage.Content.Text -> SerializableContent.Text(
                                text = content.text,
                                type = content.type,
                                cacheControl = content.cacheControl?.let { SerializableContent.CacheControl(it.type) }
                            )
                            is ClineMessage.Content.ImageUrl -> SerializableContent.ImageUrl(
                                imageUrl = SerializableContent.ImageUrlData(content.imageUrl.url),
                                type = content.type
                            )
                        }
                    }.toMutableList()
                    timestamp = message.timestamp
                    toolCalls = message.toolCalls.map { 
                        SerializableToolCall.fromToolCall(it) 
                    }.toMutableList()
                    cost = message.cost
                    cacheDiscount = message.cacheDiscount
                }
            }
        }

        fun toClineMessage(): ClineMessage {
            val convertedContent = content.map { content ->
                when (content) {
                    is SerializableContent.Text -> ClineMessage.Content.Text(
                        text = content.text,
                        type = content.type,
                        cacheControl = content.cacheControl?.let { ClineMessage.CacheControl(it.type) }
                    )
                    is SerializableContent.ImageUrl -> ClineMessage.Content.ImageUrl(
                        imageUrl = ClineMessage.ImageUrlData(content.imageUrl.url),
                        type = content.type
                    )
                }
            }

            return ClineMessage(
                role = ClineMessage.Role.valueOf(role.uppercase()),
                content = convertedContent,
                timestamp = timestamp,
                toolCalls = toolCalls.map { it.toToolCall() },
                cost = cost,
                cacheDiscount = cacheDiscount
            )
        }

        @Tag("toolCall")
        class SerializableToolCall {
            @get:Attribute
            var id: String = ""
            
            @get:Attribute
            var name: String = ""
            
            @get:Attribute
            var arguments: String = ""
            
            @get:Attribute
            var output: String? = null

            companion object {
                fun fromToolCall(toolCall: ClineMessage.ToolCall): SerializableToolCall {
                    return SerializableToolCall().apply {
                        id = toolCall.id
                        name = toolCall.name
                        arguments = toolCall.arguments
                        output = toolCall.output
                    }
                }
            }

            fun toToolCall(): ClineMessage.ToolCall {
                return ClineMessage.ToolCall(
                    id = id,
                    name = name,
                    arguments = arguments,
                    output = output
                )
            }
        }
    }

    @Tag("content")
    sealed class SerializableContent(@get:Attribute("type") open var type: String) {
        companion object {
            const val TYPE_TEXT = "text"
            const val TYPE_IMAGE_URL = "image_url"
        }
        @Tag("text")
        class Text(
            @get:Attribute
            var text: String = "",
            
            override var type: String = TYPE_TEXT,
            
            @get:Attribute
            var cacheControl: CacheControl? = null
        ) : SerializableContent(type)

        @Tag("imageUrl")
        class ImageUrl(
            @get:Attribute
            var imageUrl: ImageUrlData = ImageUrlData(),
            
            override var type: String = TYPE_IMAGE_URL
        ) : SerializableContent(type)

        @Tag("imageUrlData")
        class ImageUrlData(
            @get:Attribute
            var url: String = ""
        )

        @Tag("cacheControl")
        class CacheControl(
            @get:Attribute
            var type: String = ""
        )
    }
}
