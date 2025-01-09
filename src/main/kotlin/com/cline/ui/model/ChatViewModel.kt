package com.cline.ui.model

import com.cline.ClineMessageListener
import com.cline.ClineTopics
import com.cline.model.ClineMessage
import com.cline.model.MessageType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class ChatViewModel(private val project: Project) : ClineMessageListener, Disposable {
    private val logger = Logger.getInstance(ChatViewModel::class.java)
    private val messages = CopyOnWriteArrayList<ClineMessage>()
    private val listeners = CopyOnWriteArrayList<(List<ClineMessage>) -> Unit>()
    private val connection = ApplicationManager.getApplication().messageBus.connect()
    private var currentTaskId: String? = null

    init {
        connection.subscribe(ClineTopics.CLINE_MESSAGES, this)
        logger.info("ChatViewModel initialized for project: ${project.name}")
    }

    override fun onMessageReceived(message: ClineMessage) {
        when (message.type) {
            MessageType.TASK_REQUEST -> {
                currentTaskId = message.metadata["taskId"] ?: generateTaskId()
                messages.add(message)
            }
            MessageType.TASK_COMPLETE -> {
                messages.add(message)
                currentTaskId = null
            }
            MessageType.ERROR -> {
                logger.warn("Error message received: ${message.content}")
                messages.add(message)
            }
            else -> messages.add(message)
        }
        notifyListeners()
    }

    private fun generateTaskId(): String {
        return "task_${System.currentTimeMillis()}"
    }

    fun addListener(listener: (List<ClineMessage>) -> Unit) {
        listeners.add(listener)
        listener(messages.toList()) // Initial state
    }

    fun removeListener(listener: (List<ClineMessage>) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        val currentMessages = messages.toList()
        listeners.forEach { it(currentMessages) }
    }

    override fun dispose() {
        connection.disconnect()
        listeners.clear()
        messages.clear()
    }

    fun sendMessage(content: String) {
        val taskId = generateTaskId()
        val message = ClineMessage(
            type = MessageType.TASK_REQUEST,
            content = content,
            metadata = mapOf(
                "taskId" to taskId,
                "timestamp" to System.currentTimeMillis().toString(),
                "project" to project.name
            )
        )
        
        logger.info("Sending message with taskId: $taskId")
        ApplicationManager.getApplication().messageBus
            .syncPublisher(ClineTopics.CLINE_MESSAGES)
            .onMessageReceived(message)
    }

    fun getCurrentTask(): String? = currentTaskId

    fun clearMessages() {
        messages.clear()
        notifyListeners()
    }

    fun getMessages(): List<ClineMessage> = messages.toList()
}
