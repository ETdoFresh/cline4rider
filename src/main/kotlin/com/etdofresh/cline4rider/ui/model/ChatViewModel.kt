package com.etdofresh.cline4rider.ui.model

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.persistence.ChatHistory
import com.etdofresh.cline4rider.api.ApiProvider
import com.etdofresh.cline4rider.settings.ClineSettings
import com.etdofresh.cline4rider.tasks.TaskProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.io.File
import com.intellij.openapi.application.ApplicationManager

@Service(Service.Level.PROJECT)
class ChatViewModel(private val project: Project) {
    private val logger = com.intellij.openapi.diagnostic.Logger.getInstance(ChatViewModel::class.java)
    private var systemPrompt: String? = null
    private val taskProcessor = TaskProcessor(project)

    private fun extractTaskContent(response: String): String? {
        val taskStartTag = "<task>"
        val taskEndTag = "</task>"
        
        val startIndex = response.indexOf(taskStartTag)
        val endIndex = response.indexOf(taskEndTag)
        
        if (startIndex != -1 && endIndex != -1) {
            return response.substring(startIndex + taskStartTag.length, endIndex).trim()
        }
        return null
    }

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

    fun deleteMessage(timestamp: Long) {
        currentConversationId?.let { conversationId ->
            // Remove from current messages list
            messages.removeIf { it.timestamp == timestamp }
            // Remove from persistence
            chatHistory.deleteMessage(conversationId, timestamp)
            // Notify listeners
            notifyMessageListeners()
            notifyHistoryListeners()
        }
    }

    fun getApiKey(): String? {
        return ClineSettings.getInstance(project).getApiKey()
    }

    fun sendMessage(content: List<ClineMessage.Content>) {
        // Validate settings first
        val settings = ClineSettings.getInstance(project)
        if (settings.getApiKey().isNullOrEmpty()) {
            handleError(IllegalStateException("API key is not configured. Please configure your API key in Settings | Tools | Cline"))
            return
        }
        
        if (settings.state.provider != ClineSettings.Provider.OPENROUTER) {
            handleError(IllegalStateException("Provider must be set to OpenRouter in Settings | Tools | Cline"))
            return
        }

        // Add the user message to the list
        addMessage(ClineMessage(
            role = ClineMessage.Role.USER,
            content = content,
            timestamp = System.currentTimeMillis()
        ))

        // Create an assistant message placeholder with initial content
        val assistantMessage = ClineMessage(
            role = ClineMessage.Role.ASSISTANT,
            content = listOf(ClineMessage.Content.Text(text = "Processing...")),
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
                content = listOf(ClineMessage.Content.Text(text = combinedPrompt)),
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
                logger.warn("Sending message to API (truncated): ${messagesToSend.lastOrNull()?.content?.firstOrNull()?.let { if (it is ClineMessage.Content.Text) it.text.take(100) else "non-text content" }}")
                
                apiClient.sendMessages(messagesToSend) { chunk, stats ->
                    if (!errorOccurred) {
                        if (stats != null) {
                            // This is the final callback with stats
                            logger.warn("Received complete response (truncated): ${currentContent.toString().take(100)}")
                            totalCost = stats.total_cost ?: 0.0
                            cacheDiscount = stats.cache_discount ?: 0.0
                            
                            // Update UI only once with complete response
                            ApplicationManager.getApplication().invokeLater {
                                try {
                                    val updatedAssistantMessage = messages.last().copy(
                                        content = listOf(ClineMessage.Content.Text(text = currentContent.toString())),
                                        cost = totalCost,
                                        cacheDiscount = cacheDiscount,
                                        timestamp = System.currentTimeMillis(),
                                        model = ClineSettings.getInstance(project).state.model,
                                        toolCalls = emptyList()
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
                        } else {
                            // Accumulate chunks without updating UI
                            currentContent.append(chunk)
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

            // Log error instead of showing popup
            logger.warn("Chat error: ${e.message}")
        }
    }

    companion object {
        fun getInstance(project: Project): ChatViewModel = project.service()
    }
}
