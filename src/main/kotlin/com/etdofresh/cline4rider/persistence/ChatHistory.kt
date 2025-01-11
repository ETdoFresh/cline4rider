package com.etdofresh.cline4rider.persistence

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.model.ClineMessage.Role
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

@State(
    name = "ChatHistory",
    storages = [Storage("cline-chat-history.xml")]
)
class ChatHistory : PersistentStateComponent<ChatHistory> {
    @XCollection
    private var messages: MutableList<SerializableMessage> = mutableListOf()

    companion object {
        fun getInstance(project: Project): ChatHistory = project.service()
    }

    fun addMessage(message: ClineMessage) {
        messages.add(SerializableMessage.fromClineMessage(message))
    }

    fun getMessages(): List<ClineMessage> {
        return messages.map { it.toClineMessage() }
    }

    fun clearMessages() {
        messages.clear()
    }

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
