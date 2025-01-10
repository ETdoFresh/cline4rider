package com.cline.ui.model

import com.cline.api.AnthropicClient
import com.cline.model.ClineMessage
import com.cline.persistence.ChatHistory
import com.cline.settings.ClineSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*

@Service(Service.Level.PROJECT)
class ChatViewModel(private val project: Project) : Disposable {
    private val settings = ClineSettings.getInstance()
    private val client = AnthropicClient(settings)
    private val chatHistory = ChatHistory.getInstance(project)
    private val messageListeners = mutableListOf<(List<ClineMessage>) -> Unit>()
    private val stateListeners = mutableListOf<(Boolean) -> Unit>()
    private var isProcessing = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        fun getInstance(project: Project): ChatViewModel = project.service()
    }

    fun addMessageListener(listener: (List<ClineMessage>) -> Unit) {
        messageListeners.add(listener)
        listener(chatHistory.getMessages())
    }

    fun addStateListener(listener: (Boolean) -> Unit) {
        stateListeners.add(listener)
        listener(isProcessing)
    }

    fun getMessages(): List<ClineMessage> = chatHistory.getMessages()

    fun clearMessages() {
        chatHistory.clearMessages()
        notifyMessageListeners()
    }

    fun sendMessage(content: String) {
        val userMessage = ClineMessage(
            role = "user",
            content = content,
            timestamp = System.currentTimeMillis()
        )
        chatHistory.addMessage(userMessage)
        notifyMessageListeners()

        setProcessing(true)
        coroutineScope.launch {
            try {
                val response = client.sendMessage(content)
                val assistantMessage = ClineMessage(
                    role = "assistant",
                    content = response,
                    timestamp = System.currentTimeMillis()
                )
                chatHistory.addMessage(assistantMessage)
                notifyMessageListeners()
            } catch (e: Exception) {
                val errorMessage = ClineMessage(
                    role = "system",
                    content = "Error: ${e.message}",
                    timestamp = System.currentTimeMillis()
                )
                chatHistory.addMessage(errorMessage)
                notifyMessageListeners()
            } finally {
                setProcessing(false)
            }
        }
    }

    private fun notifyMessageListeners() {
        val currentMessages = chatHistory.getMessages()
        messageListeners.forEach { it(currentMessages) }
    }

    private fun setProcessing(processing: Boolean) {
        isProcessing = processing
        stateListeners.forEach { it(processing) }
    }

    override fun dispose() {
        coroutineScope.cancel()
        messageListeners.clear()
        stateListeners.clear()
    }
}
