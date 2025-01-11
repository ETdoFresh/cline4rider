package com.etdofresh.cline4rider.ui.model

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.persistence.ChatHistory
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.io.File

@Service(Service.Level.PROJECT)
class ChatViewModel(private val project: Project) {
    private var systemPrompt: String? = null
    private var messages = mutableListOf<ClineMessage>()
    private var isProcessing = false
    private val messageListeners = mutableListOf<(List<ClineMessage>) -> Unit>()
    private val historyListeners = mutableListOf<(List<ChatHistory.Conversation>) -> Unit>()
    private val stateListeners = mutableListOf<(Boolean) -> Unit>()
    private val chatHistory = ChatHistory.getInstance(project)

    fun getSystemPrompt(): String? = systemPrompt

    fun setSystemPrompt(prompt: String) {
        systemPrompt = prompt
    }

    fun addMessage(message: ClineMessage) {
        messages.add(message)
        notifyMessageListeners()
    }

    fun getMessages(): List<ClineMessage> = messages.toList()

    fun clearMessages() {
        messages.clear()
        notifyMessageListeners()
    }

    fun isProcessing() = isProcessing

    fun setProcessing(processing: Boolean) {
        isProcessing = processing
        notifyStateListeners()
    }

    fun addMessageListener(listener: (List<ClineMessage>) -> Unit) {
        messageListeners.add(listener)
        listener(messages)
    }

    fun addHistoryListener(listener: (List<ChatHistory.Conversation>) -> Unit) {
        historyListeners.add(listener)
        listener(chatHistory.getRecentConversations())
    }

    fun addStateListener(listener: (Boolean) -> Unit) {
        stateListeners.add(listener)
        listener(isProcessing)
    }

    private fun notifyMessageListeners() {
        messageListeners.forEach { it(messages) }
    }

    private fun notifyHistoryListeners() {
        historyListeners.forEach { it(chatHistory.getRecentConversations()) }
    }

    private fun notifyStateListeners() {
        stateListeners.forEach { it(isProcessing) }
    }

    fun getRecentConversations(offset: Int = 0): List<ChatHistory.Conversation> {
        return chatHistory.getRecentConversations(offset)
    }

    fun hasMoreConversations(offset: Int): Boolean {
        return chatHistory.hasMoreConversations(offset)
    }

    fun loadConversation(id: String) {
        messages = chatHistory.getConversationMessages(id).toMutableList()
        notifyMessageListeners()
    }

    fun createNewTask() {
        messages.clear()
        notifyMessageListeners()
    }

    fun deleteConversationById(id: String) {
        chatHistory.deleteConversation(id)
        notifyHistoryListeners()
    }

    fun getApiKey(): String? {
        // Implementation for getting API key from settings
        return null
    }

    fun sendMessage(content: String) {
        // Add the message to the list
        addMessage(ClineMessage(
            role = ClineMessage.Role.USER,
            content = content,
            timestamp = System.currentTimeMillis()
        ))

        // Set processing state
        setProcessing(true)

        // TODO: Send message to API with system prompt if available
        // The actual implementation will depend on your API client
        // Example pseudocode:
        // apiClient.sendMessage(content, systemPrompt)

        setProcessing(false)
    }

    companion object {
        fun getInstance(project: Project): ChatViewModel = project.service()
    }
}
