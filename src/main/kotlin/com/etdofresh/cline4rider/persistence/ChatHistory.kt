package com.etdofresh.cline4rider.persistence

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.model.ClineMessage.Role
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
        @Attribute
        var id: String = ""
        @Attribute
        var timestamp: Long = 0
        @XCollection
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
        conversations.add(Conversation(conversationId, System.currentTimeMillis()))
        return conversationId
    }

    fun addMessage(conversationId: String, message: ClineMessage) {
        conversations.find { it.id == conversationId }?.messages?.add(
            SerializableMessage.fromClineMessage(message)
        )
    }

    fun getConversationMessages(conversationId: String): List<ClineMessage> {
        return conversations.find { it.id == conversationId }?.messages
            ?.map { it.toClineMessage() } ?: emptyList()
    }

    fun getRecentConversations(offset: Int = 0): List<Conversation> {
        return conversations.sortedByDescending { it.timestamp }
            .drop(offset)
            .take(PAGE_SIZE)
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

    fun getCurrentConversationId(): String? = currentConversationId

    override fun getState(): ChatHistory = this

    override fun loadState(state: ChatHistory) {
        XmlSerializerUtil.copyBean(state, this)
    }

    @Tag("message")
    class SerializableMessage {
        var role: String = ""
        var content: String = ""
        var timestamp: Long = 0
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
            var id: String = ""
            var name: String = ""
            var arguments: String = ""
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
