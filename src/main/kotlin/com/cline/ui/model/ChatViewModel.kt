package com.cline.ui.model

import com.cline.api.AnthropicClient
import com.cline.api.Message
import com.cline.model.ClineMessage
import com.cline.model.MessageType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class ChatViewModel(private val project: Project) : Disposable {
    private val logger = Logger.getInstance(ChatViewModel::class.java)
    private val messages = CopyOnWriteArrayList<ClineMessage>()
    private val recentTasks = CopyOnWriteArrayList<ClineMessage>()
    private val messageListeners = CopyOnWriteArrayList<(List<ClineMessage>) -> Unit>()
    private val stateListeners = CopyOnWriteArrayList<(Boolean) -> Unit>()
    private val apiClient = AnthropicClient.getInstance()
    private val chatHistory = com.intellij.openapi.components.service<com.cline.persistence.ChatHistory>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentTaskId: String? = null

    init {
        logger.info("ChatViewModel initialized for project: ${project.name}")
        
        // Load persisted messages
        messages.addAll(chatHistory.getAllMessages())
        recentTasks.addAll(chatHistory.getAllRecentTasks())
        
        // Notify listeners of initial state
        notifyListeners()
    }

    fun sendMessage(content: String) {
        // Check API settings first
        if (apiClient.settings.apiKey.isEmpty()) {
            com.cline.notifications.ClineNotifier.notifyErrorWithAction(
                project,
                "Please configure your API key in the settings before using Cline.",
                "API Key Required"
            )
            return
        }

        val taskId = generateTaskId()
        val userMessage = ClineMessage(
            type = MessageType.TASK_REQUEST,
            content = content,
            metadata = mapOf(
                "taskId" to taskId,
                "timestamp" to System.currentTimeMillis().toString(),
                "project" to project.name
            )
        )

        currentTaskId = taskId
        messages.add(userMessage)
        chatHistory.addMessage(userMessage)
        addToRecentTasks(userMessage)
        notifyListeners()

        coroutineScope.launch {
            val apiMessages = messages.map { Message(
                role = if (it.type == MessageType.TASK_REQUEST) "user" else "assistant",
                content = it.content
            )}

            when (val result = apiClient.sendMessage(apiMessages)) {
                is com.cline.api.ApiResult.Success -> {
                    val response = result.data
                    val assistantMessage = ClineMessage(
                        type = MessageType.TASK_COMPLETE,
                        content = response.content.joinToString("") { it.text },
                        metadata = mapOf(
                            "taskId" to taskId,
                            "timestamp" to System.currentTimeMillis().toString(),
                            "tokens" to "${response.usage.input_tokens + response.usage.output_tokens}",
                            "model" to response.model
                        )
                    )
                    
                    withContext(Dispatchers.Main) {
                        messages.add(assistantMessage)
                        chatHistory.addMessage(assistantMessage)
                        updateRecentTaskStatus(taskId, assistantMessage)
                        currentTaskId = null
                        notifyListeners()
                    }
                }
                is com.cline.api.ApiResult.Error -> {
                    val errorMessage = ClineMessage(
                        type = MessageType.ERROR,
                        content = result.message,
                        metadata = mapOf(
                            "taskId" to taskId,
                            "timestamp" to System.currentTimeMillis().toString(),
                            "errorCode" to (result.code?.toString() ?: "unknown")
                        )
                    )
                    
                    withContext(Dispatchers.Main) {
                        messages.add(errorMessage)
                        chatHistory.addMessage(errorMessage)
                        updateRecentTaskStatus(taskId, errorMessage)
                        currentTaskId = null
                        notifyListeners()

                        // Show notification for API errors
                        when (result.code) {
                            401 -> {
                                com.cline.notifications.ClineNotifier.notifyErrorWithAction(
                                    project,
                                    "Invalid API key. Please check your settings.",
                                    "Authentication Error"
                                )
                            }
                            429 -> {
                                com.cline.notifications.ClineNotifier.notifyWarning(
                                    project,
                                    "Rate limit exceeded. Please try again later.",
                                    "Rate Limit"
                                )
                            }
                            else -> {
                                com.cline.notifications.ClineNotifier.notifyError(
                                    project,
                                    result.message,
                                    "API Error"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun generateTaskId(): String = "task_${System.currentTimeMillis()}"

    private fun addToRecentTasks(message: ClineMessage) {
        recentTasks.add(0, message)
        chatHistory.addRecentTask(message)
        if (recentTasks.size > MAX_RECENT_TASKS) {
            recentTasks.removeAt(recentTasks.size - 1)
        }
    }

    private fun updateRecentTaskStatus(taskId: String?, completionMessage: ClineMessage) {
        taskId?.let { id ->
            val taskIndex = recentTasks.indexOfFirst { it.metadata["taskId"] == id }
            if (taskIndex != -1) {
                val task = recentTasks[taskIndex]
                val updatedMetadata = task.metadata.toMutableMap().apply {
                    put("completion", completionMessage.content)
                    put("status", if (completionMessage.type == MessageType.ERROR) "error" else "completed")
                    put("completionTime", System.currentTimeMillis().toString())
                }
                val updatedTask = task.copy(metadata = updatedMetadata)
                recentTasks[taskIndex] = updatedTask
                chatHistory.addRecentTask(updatedTask)
            }
        }
    }

    fun clearMessages() {
        messages.clear()
        recentTasks.clear()
        chatHistory.clear()
        notifyListeners()
    }

    fun addMessageListener(listener: (List<ClineMessage>) -> Unit) {
        messageListeners.add(listener)
        listener(messages.toList()) // Initial state
    }

    fun removeMessageListener(listener: (List<ClineMessage>) -> Unit) {
        messageListeners.remove(listener)
    }

    fun addStateListener(listener: (Boolean) -> Unit) {
        stateListeners.add(listener)
        listener(currentTaskId != null) // Initial state
    }

    fun removeStateListener(listener: (Boolean) -> Unit) {
        stateListeners.remove(listener)
    }

    private fun notifyListeners() {
        val currentMessages = messages.toList()
        messageListeners.forEach { it(currentMessages) }
        stateListeners.forEach { it(currentTaskId != null) }
    }

    override fun dispose() {
        coroutineScope.cancel()
        messageListeners.clear()
        stateListeners.clear()
        messages.clear()
        recentTasks.clear()
        // Don't clear persistent storage on dispose
    }

    fun isProcessing(): Boolean = currentTaskId != null

    fun getCurrentTask(): String? = currentTaskId

    fun getMessages(): List<ClineMessage> = messages.toList()
    
    fun getRecentTasks(): List<ClineMessage> = recentTasks.toList()

    companion object {
        private const val MAX_RECENT_TASKS = 10
    }
}
