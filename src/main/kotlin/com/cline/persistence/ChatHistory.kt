package com.cline.persistence

import com.cline.model.ClineMessage
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "ClineChatHistory",
    storages = [Storage("cline_chat_history.xml")]
)
@Service(Service.Level.PROJECT)
class ChatHistory : PersistentStateComponent<ChatHistory> {
    // Public properties for serialization
    var messages: MutableList<SerializableMessage> = mutableListOf()
        private set
    var recentTasks: MutableList<SerializableMessage> = mutableListOf()
        private set

    override fun getState(): ChatHistory = this

    override fun loadState(state: ChatHistory) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun addMessage(message: ClineMessage) {
        messages.add(SerializableMessage.fromClineMessage(message))
        if (messages.size > MAX_MESSAGES) {
            messages.removeAt(0)
        }
    }

    fun addRecentTask(message: ClineMessage) {
        val taskId = message.metadata["taskId"]
        if (taskId != null) {
            val existingIndex = recentTasks.indexOfFirst { 
                it.metadata["taskId"] == taskId 
            }
            
            if (existingIndex != -1) {
                recentTasks[existingIndex] = SerializableMessage.fromClineMessage(message)
            } else {
                recentTasks.add(0, SerializableMessage.fromClineMessage(message))
                if (recentTasks.size > MAX_RECENT_TASKS) {
                    recentTasks.removeAt(recentTasks.size - 1)
                }
            }
        } else {
            recentTasks.add(0, SerializableMessage.fromClineMessage(message))
            if (recentTasks.size > MAX_RECENT_TASKS) {
                recentTasks.removeAt(recentTasks.size - 1)
            }
        }
    }

    fun getAllMessages(): List<ClineMessage> = messages.map { it.toClineMessage() }
    fun getAllRecentTasks(): List<ClineMessage> = recentTasks.map { it.toClineMessage() }

    fun clear() {
        messages.clear()
        recentTasks.clear()
    }

    companion object {
        private const val MAX_MESSAGES = 100
        private const val MAX_RECENT_TASKS = 10
    }
}

data class SerializableMessage(
    var type: String = "",
    var content: String = "",
    var metadata: Map<String, String> = emptyMap()
) {
    companion object {
        fun fromClineMessage(message: ClineMessage): SerializableMessage =
            SerializableMessage(
                type = message.type.name,
                content = message.content,
                metadata = message.metadata
            )
    }

    fun toClineMessage(): ClineMessage =
        ClineMessage(
            type = com.cline.model.MessageType.valueOf(type),
            content = content,
            metadata = metadata
        )
}
