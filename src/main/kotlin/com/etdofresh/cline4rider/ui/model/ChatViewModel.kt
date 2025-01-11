package com.etdofresh.cline4rider.ui.model

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.persistence.ChatHistory
import com.etdofresh.cline4rider.api.ApiProvider
import com.etdofresh.cline4rider.settings.ClineSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.io.File
import com.intellij.openapi.application.ApplicationManager

@Service(Service.Level.PROJECT)
class ChatViewModel(private val project: Project) {
    private var systemPrompt: String? = null
    private var messages = mutableListOf<ClineMessage>()
    private var isProcessing = false
    private val messageListeners = mutableListOf<(List<ClineMessage>) -> Unit>()
    private val historyListeners = mutableListOf<(List<ChatHistory.Conversation>) -> Unit>()
    private val stateListeners = mutableListOf<(Boolean) -> Unit>()
    private val chatHistory = ChatHistory.getInstance(project)
    private var currentConversationId: String? = null

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
        currentConversationId = null
        notifyMessageListeners()
    }

    fun deleteConversationById(id: String) {
        chatHistory.deleteConversation(id)
        notifyHistoryListeners()
    }

    fun getApiKey(): String? {
        return ClineSettings.getInstance(project).getApiKey()
    }

    fun sendMessage(content: String) {
        // Add the user message to the list
        addMessage(ClineMessage(
            role = ClineMessage.Role.USER,
            content = content,
            timestamp = System.currentTimeMillis()
        ))

        // Create an assistant message placeholder
        val assistantMessage = ClineMessage(
            role = ClineMessage.Role.ASSISTANT,
            content = "",
            timestamp = System.currentTimeMillis()
        )
        addMessage(assistantMessage)

        // Set processing state
        setProcessing(true)

        // Prepare messages list including system prompt if available
        val messagesToSend = mutableListOf<ClineMessage>()
        systemPrompt?.let {
            messagesToSend.add(ClineMessage(
                role = ClineMessage.Role.SYSTEM,
                content = it,
                timestamp = System.currentTimeMillis()
            ))
        }
        messagesToSend.addAll(messages.dropLast(1)) // Exclude the empty assistant message

        // Get API client
        val apiClient = ApiProvider.getClient(project)

        // Start background task
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                var currentContent = StringBuilder()
                var errorOccurred = false
                
                // Create new conversation if needed and save initial messages on EDT
                ApplicationManager.getApplication().invokeAndWait {
                    ApplicationManager.getApplication().runWriteAction {
                        if (currentConversationId == null) {
                            currentConversationId = chatHistory.startNewConversation()
                        }
                        messagesToSend.forEach { message ->
                            chatHistory.addMessage(currentConversationId!!, message)
                        }
                    }
                }

                // Handle streaming responses
                apiClient.sendMessages(messagesToSend) { chunk ->
                    if (!errorOccurred) {
                        currentContent.append(chunk)
                        ApplicationManager.getApplication().invokeLater {
                            try {
                                // Update the last message (assistant's message) with accumulated content
                                val updatedAssistantMessage = messages.last().copy(content = currentContent.toString())
                                messages[messages.size - 1] = updatedAssistantMessage
                                
                                // First update the chat history in a write action
                                ApplicationManager.getApplication().runWriteAction {
                                    chatHistory.addMessage(currentConversationId!!, updatedAssistantMessage)
                                    chatHistory.saveState()
                                }
                                
                                // Then notify listeners outside the write action
                                notifyMessageListeners()
                                notifyHistoryListeners()
                            } catch (e: Exception) {
                                errorOccurred = true
                                handleError(e)
                            }
                        }
                    }
                }

                if (!errorOccurred) {
                    ApplicationManager.getApplication().invokeLater {
                        setProcessing(false)
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun handleError(e: Exception) {
        ApplicationManager.getApplication().invokeLater {
            // Remove the placeholder message on error
            if (messages.isNotEmpty() && messages.last().role == ClineMessage.Role.ASSISTANT && messages.last().content.isEmpty()) {
                messages.removeAt(messages.size - 1)
                notifyMessageListeners()
            }
            setProcessing(false)

            // Show error notification
            com.intellij.openapi.ui.Messages.showErrorDialog(
                project,
                "Error: ${e.message}",
                "Chat Error"
            )
        }
    }


    companion object {
        fun getInstance(project: Project): ChatViewModel = project.service()
    }
}
