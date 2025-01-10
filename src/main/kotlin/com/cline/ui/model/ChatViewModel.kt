package com.cline.ui.model

import com.cline.api.ApiProvider
import com.cline.model.ClineMessage
import com.cline.model.Role
import com.cline.settings.ClineSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class ChatViewModel(private val project: Project) {
    private val logger = Logger.getInstance(ChatViewModel::class.java)
    private val apiProvider = project.getService(ApiProvider::class.java)
    private val settings = ClineSettings.getInstance(project)
    private val messages = CopyOnWriteArrayList<ClineMessage>()
    private val messageListeners = mutableListOf<(List<ClineMessage>) -> Unit>()
    private val stateListeners = mutableListOf<(Boolean) -> Unit>()
    private var isProcessing = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    fun addMessageListener(listener: (List<ClineMessage>) -> Unit) {
        messageListeners.add(listener)
        listener(messages.toList())
    }

    fun addStateListener(listener: (Boolean) -> Unit) {
        stateListeners.add(listener)
        listener(isProcessing)
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || isProcessing) return

        val userMessage = ClineMessage(content, Role.USER)
        messages.add(userMessage)
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

                val result = apiProvider.sendMessage(content, apiKey)
                result.fold(
                    onSuccess = { response ->
                        messages.add(response)
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
        messages.add(ClineMessage(errorMessage, Role.SYSTEM))
        notifyMessageListeners()
    }

    fun clearMessages() {
        messages.clear()
        notifyMessageListeners()
    }

    fun getMessages(): List<ClineMessage> = messages.toList()

    private fun notifyMessageListeners() {
        messageListeners.forEach { it(messages.toList()) }
    }

    private fun notifyStateListeners() {
        stateListeners.forEach { it(isProcessing) }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ChatViewModel =
            project.getService(ChatViewModel::class.java)
    }
}
