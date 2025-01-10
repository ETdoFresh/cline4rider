package com.cline.persistence

import com.cline.model.ClineMessage
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

        companion object {
            fun fromClineMessage(message: ClineMessage): SerializableMessage {
                return SerializableMessage().apply {
                    role = message.role
                    content = message.content
                    timestamp = message.timestamp
                }
            }
        }

        fun toClineMessage(): ClineMessage {
            return ClineMessage(
                role = role,
                content = content,
                timestamp = timestamp
            )
        }
    }
}
