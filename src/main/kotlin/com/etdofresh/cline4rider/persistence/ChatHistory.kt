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
        println("DEBUG: Created new conversation with ID: $conversationId")
        return conversationId
    }

    fun addMessage(conversationId: String, message: ClineMessage) {
        println("DEBUG: Adding/Updating message to conversation $conversationId: ${message.content}")
        conversations.find { it.id == conversationId }?.let { conversation ->
            when {
                // For assistant messages, always try to update the last message if it's from assistant
                message.role == ClineMessage.Role.ASSISTANT && 
                conversation.messages.isNotEmpty() && 
                conversation.messages.last().role == "ASSISTANT" -> {
                    // Update existing assistant message
                    conversation.messages[conversation.messages.lastIndex] = SerializableMessage.fromClineMessage(message)
                    println("DEBUG: Updated existing assistant message")
                }
                
                // For non-assistant messages or if last message isn't from assistant, add as new
                else -> {
                    // Only add if it's not a duplicate (check role and content)
                    val isDuplicate = conversation.messages.any { 
                        it.role == message.role.toString() && it.content == message.content 
                    }
                    if (!isDuplicate) {
                        conversation.messages.add(SerializableMessage.fromClineMessage(message))
                        println("DEBUG: Added new message")
                    } else {
                        println("DEBUG: Skipped duplicate message")
                    }
                }
            }
            // Update conversation timestamp to latest message
            conversation.timestamp = message.timestamp
            // Sort conversations by timestamp
            conversations.sortByDescending { it.timestamp }
            println("DEBUG: Message operation successful. Total messages in conversation: ${conversation.messages.size}")
        } ?: println("DEBUG: Conversation not found: $conversationId")
    }

    override fun getState(): ChatHistory {
        println("DEBUG: Getting state. Total conversations: ${conversations.size}")
        conversations.forEach { conversation ->
            println("DEBUG: - Conversation ${conversation.id}: ${conversation.messages.size} messages")
            conversation.messages.forEach { message ->
                println("DEBUG: -- Message: ${message.role} - ${message.content.take(50)}...")
            }
        }
        return this
    }

    override fun loadState(state: ChatHistory) {
        println("DEBUG: Loading state. Incoming conversations: ${state.conversations.size}")
        state.conversations.forEach { conversation ->
            println("DEBUG: - Loading conversation ${conversation.id}: ${conversation.messages.size} messages")
        }
        
        // Clear existing conversations
        conversations.clear()
        
        // Copy all conversations from state
        conversations.addAll(state.conversations)
        currentConversationId = state.currentConversationId
        
        // Sort conversations after loading
        conversations.sortByDescending { it.timestamp }
        
        println("DEBUG: State loaded. Current conversations: ${conversations.size}")
        conversations.forEach { conversation ->
            println("DEBUG: - Loaded conversation ${conversation.id}: ${conversation.messages.size} messages")
            conversation.messages.forEach { message ->
                println("DEBUG: -- Message: ${message.role} - ${message.content.take(50)}...")
            }
        }
    }

    fun saveState() {
        println("DEBUG: Saving state with ${conversations.size} conversations")
        try {
            if (ApplicationManager.getApplication().isDispatchThread) {
                // If we're on EDT, schedule the save on a background thread
                ApplicationManager.getApplication().executeOnPooledThread {
                    ApplicationManager.getApplication().invokeAndWait {
                        ApplicationManager.getApplication().saveSettings()
                    }
                }
            } else {
                // If we're already on a background thread, invoke directly
                ApplicationManager.getApplication().invokeAndWait {
                    ApplicationManager.getApplication().saveSettings()
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Error saving state: ${e.message}")
        }
    }

    fun getConversationMessages(conversationId: String): List<ClineMessage> {
        return conversations.find { it.id == conversationId }?.messages
            ?.map { it.toClineMessage() } ?: emptyList()
    }

    fun getRecentConversations(offset: Int = 0): List<Conversation> {
        println("DEBUG: Getting recent conversations. Total: ${conversations.size}, Offset: $offset")
        // Ensure conversations are sorted by timestamp
        conversations.sortByDescending { it.timestamp }
        val result = conversations.drop(offset).take(PAGE_SIZE)
        println("DEBUG: Returning ${result.size} conversations")
        result.forEach { conversation ->
            println("DEBUG: Conversation ${conversation.id} has ${conversation.messages.size} messages")
            conversation.messages.forEach { message ->
                println("DEBUG: - Message: ${message.role} - ${message.content.take(50)}...")
            }
        }
        return result
    }

    fun hasMoreConversations(offset: Int): Boolean {
        val hasMore = conversations.size > offset + PAGE_SIZE
        println("DEBUG: Checking for more conversations. Total: ${conversations.size}, Offset: $offset, HasMore: $hasMore")
        return hasMore
    }

    fun clearConversation(conversationId: String) {
        println("DEBUG: Clearing conversation: $conversationId")
        conversations.find { it.id == conversationId }?.messages?.clear()
    }

    fun deleteConversation(conversationId: String) {
        println("DEBUG: Deleting conversation: $conversationId")
        conversations.removeIf { it.id == conversationId }
        if (conversationId == currentConversationId) {
            currentConversationId = null
        }
    }


    @Tag("message")
    class SerializableMessage {
        @get:Attribute
        var role: String = ""
        
        @get:Attribute
        var content: String = ""
        
        @get:Attribute
        var timestamp: Long = 0
        
        @get:XCollection(style = XCollection.Style.v2)
        var toolCalls: MutableList<SerializableToolCall> = mutableListOf()

        companion object {
            fun fromClineMessage(message: ClineMessage): SerializableMessage {
                return SerializableMessage().apply {
                    role = message.role.toString()
                    content = message.content
                    timestamp = message.timestamp
                    toolCalls = message.toolCalls.map { 
                        SerializableToolCall.fromToolCall(it) 
                    }.toMutableList()
                }
            }
        }

        fun toClineMessage(): ClineMessage {
            return ClineMessage(
                role = ClineMessage.Role.valueOf(role.uppercase()),
                content = content,
                timestamp = timestamp,
                toolCalls = toolCalls.map { it.toToolCall() }
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
}
