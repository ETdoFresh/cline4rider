package com.etdofresh.cline4rider.ui.model

import com.etdofresh.cline4rider.api.ApiProvider
import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.model.ClineMessage.Role
import com.etdofresh.cline4rider.persistence.ChatHistory
import com.etdofresh.cline4rider.settings.ClineSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*

@Service(Service.Level.PROJECT)
class ChatViewModel(private val project: Project) {
    private val logger = Logger.getInstance(ChatViewModel::class.java)
    private val apiProvider = project.getService(ApiProvider::class.java)
    private val settings = ClineSettings.getInstance(project)
    private val chatHistory = project.getService(ChatHistory::class.java)
    private val messageListeners = mutableListOf<(List<ClineMessage>) -> Unit>()
    private val stateListeners = mutableListOf<(Boolean) -> Unit>()
    private val historyListeners = mutableListOf<(List<ChatHistory.Conversation>) -> Unit>()
    private var isProcessing = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var currentConversationId: String? = null
    private var isFirstUserMessage = true
    private val welcomeMessage = ClineMessage(
        ClineMessage.Role.SYSTEM,
        "Welcome to Cline for Rider! How can I assist you today?",
        System.currentTimeMillis()
    )

    private fun startNewConversation() {
        currentConversationId = chatHistory.startNewConversation()
        if (!isFirstUserMessage) {
            // Only save welcome message to history if this isn't the first conversation
            chatHistory.addMessage(currentConversationId!!, welcomeMessage)
        }
        notifyMessageListeners()
    }

    init {
        startNewConversation()
    }

    fun addMessageListener(listener: (List<ClineMessage>) -> Unit) {
        messageListeners.add(listener)
        listener(getMessages())
    }

    fun addStateListener(listener: (Boolean) -> Unit) {
        stateListeners.add(listener)
        listener(isProcessing)
    }

    fun addHistoryListener(listener: (List<ChatHistory.Conversation>) -> Unit) {
        historyListeners.add(listener)
        listener(getRecentConversations())
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || isProcessing) return

        if (isFirstUserMessage) {
            // For the first user message, we need to save both welcome message and user message
            isFirstUserMessage = false
            chatHistory.addMessage(currentConversationId!!, welcomeMessage)
        }

        val userMessage = ClineMessage(Role.USER, content, System.currentTimeMillis())
        chatHistory.addMessage(currentConversationId!!, userMessage)
        notifyMessageListeners()

        isProcessing = true
        notifyStateListeners()

        coroutineScope.launch {
            try {
                val apiKey = settings.getApiKey()
                if (apiKey.isNullOrBlank()) {
                    handleError("API key not configured. Please set it in Settings | Tools | Cline.")
                    return@launch
                }

                val result = apiProvider.sendMessages(getMessages(), apiKey, settings.state.provider, settings)
                result.fold(
                    onSuccess = { response ->
                        chatHistory.addMessage(currentConversationId!!, response)
                        notifyMessageListeners()
                    },
                    onFailure = { error ->
                        handleError("Failed to get response: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                handleError("Error processing message: ${e.message}")
            } finally {
                isProcessing = false
                notifyStateListeners()
            }
        }
    }

    private fun handleError(errorMessage: String) {
        logger.error(errorMessage)
        chatHistory.addMessage(currentConversationId!!, 
            ClineMessage(Role.SYSTEM, errorMessage, System.currentTimeMillis()))
        notifyMessageListeners()
    }

    fun clearMessages() {
        currentConversationId?.let {
            chatHistory.clearConversation(it)
            isFirstUserMessage = true
            startNewConversation()
        }
    }

    fun getMessages(): List<ClineMessage> {
        val messages = currentConversationId?.let {
            chatHistory.getConversationMessages(it)
        } ?: emptyList()

        // If this is the first conversation and no messages have been saved yet,
        // return just the welcome message for display purposes
        return if (isFirstUserMessage && messages.isEmpty()) {
            listOf(welcomeMessage)
        } else {
            messages
        }
    }

    fun getRecentConversations(offset: Int = 0): List<ChatHistory.Conversation> {
        return chatHistory.getRecentConversations(offset)
    }

    fun hasMoreConversations(offset: Int): Boolean {
        return chatHistory.hasMoreConversations(offset)
    }

    private fun notifyMessageListeners() {
        messageListeners.forEach { it(getMessages()) }
    }

    private fun notifyHistoryListeners() {
        historyListeners.forEach { it(getRecentConversations()) }
    }

    private fun notifyStateListeners() {
        stateListeners.forEach { it(isProcessing) }
    }

    fun isProcessing(): Boolean = isProcessing

    fun getApiKey(): String? {
        return settings.getApiKey()
    }

    fun createNewTask() {
        // Start a new conversation with first message state reset
        isFirstUserMessage = true
        startNewConversation()
    }

    fun loadConversation(conversationId: String) {
        logger.info("Loading conversation: $conversationId")
        currentConversationId = conversationId
        // When loading an existing conversation, it's never the first message
        isFirstUserMessage = false
        logger.info("Current messages: ${getMessages()}")
        notifyMessageListeners()
        notifyHistoryListeners()
        logger.info("Finished loading conversation")
    }

    fun getCurrentConversationId(): String? = currentConversationId

    fun deleteConversationById(conversationId: String) {
        chatHistory.deleteConversation(conversationId)
        if (conversationId == currentConversationId) {
            startNewConversation()
            notifyMessageListeners()
        }
        notifyHistoryListeners()
        // Also notify home panel to refresh its recent history
        historyListeners.forEach { it(getRecentConversations()) }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ChatViewModel =
            project.getService(ChatViewModel::class.java)
    }
}
