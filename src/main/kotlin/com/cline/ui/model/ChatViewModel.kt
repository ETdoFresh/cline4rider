package com.cline.ui.model

import com.cline.ClineMessageListener
import com.cline.ClineTopics
import com.cline.model.ClineMessage
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.util.concurrent.CopyOnWriteArrayList

class ChatViewModel(private val project: Project) : ClineMessageListener, Disposable {
    private val messages = CopyOnWriteArrayList<ClineMessage>()
    private val listeners = CopyOnWriteArrayList<(List<ClineMessage>) -> Unit>()
    private val connection = ApplicationManager.getApplication().messageBus.connect()

    init {
        connection.subscribe(ClineTopics.CLINE_MESSAGES, this)
    }

    override fun onMessageReceived(message: ClineMessage) {
        messages.add(message)
        notifyListeners()
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
        val message = ClineMessage(
            type = com.cline.model.MessageType.TASK_REQUEST,
            content = content
        )
        ApplicationManager.getApplication().messageBus
            .syncPublisher(ClineTopics.CLINE_MESSAGES)
            .onMessageReceived(message)
    }
}
