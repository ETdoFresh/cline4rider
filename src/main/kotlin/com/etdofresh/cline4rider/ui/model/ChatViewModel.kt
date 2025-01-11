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

    private fun getCombinedSystemPrompt(): String {
        val systemPromptFile = File(project.basePath, ".clinesystemprompt")
        val rulesFile = File(project.basePath, ".clinerules")
        
        val systemPromptContent = if (systemPromptFile.exists()) {
            systemPromptFile.readText()
        } else {
            ""
        }
        
        val rulesContent = if (rulesFile.exists()) {
            rulesFile.readText()
        } else {
            ""
        }
        
        return if (systemPromptContent.isNotEmpty() && rulesContent.isNotEmpty()) {
            "$systemPromptContent\n$rulesContent"
        } else {
            systemPromptContent.ifEmpty { rulesContent }
        }
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
        currentConversationId = id
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

                // Create an assistant message placeholder with current timestamp
                val assistantMessage = ClineMessage(
                    role = ClineMessage.Role.ASSISTANT,
                    content = "",
                    timestamp = System.currentTimeMillis()
                )
        addMessage(assistantMessage)

        // Set processing state
        setProcessing(true)

        // Prepare messages list including combined system prompt
        val messagesToSend = mutableListOf<ClineMessage>()
        val combinedPrompt = getCombinedSystemPrompt()
        if (combinedPrompt.isNotEmpty()) {
            messagesToSend.add(ClineMessage(
                role = ClineMessage.Role.SYSTEM,
                content = combinedPrompt,
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
                var totalCost = 0.0
                var cacheDiscount = 0.0
                apiClient.sendMessages(messagesToSend) { chunk, stats ->
                    if (!errorOccurred) {
                        currentContent.append(chunk)
                        stats?.let { 
                            totalCost = it.total_cost ?: 0.0
                            cacheDiscount = it.cache_discount ?: 0.0
                        }
                        
                        // Update message with new content and timestamp on every chunk
                        ApplicationManager.getApplication().invokeLater {
                            try {
                                val updatedAssistantMessage = messages.last().copy(
                                    content = currentContent.toString(),
                                    cost = totalCost,
                                    cacheDiscount = cacheDiscount,
                                    timestamp = System.currentTimeMillis()  // Update timestamp on final chunk
                                )
                                messages[messages.size - 1] = updatedAssistantMessage
                                
                                // Update the chat history in a write action
                                ApplicationManager.getApplication().runWriteAction {
                                    chatHistory.addMessage(currentConversationId!!, updatedAssistantMessage)
                                    chatHistory.saveState()
                                }
                                
                                // Notify listeners outside the write action
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
