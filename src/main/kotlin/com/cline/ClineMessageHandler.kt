package com.cline

import com.cline.model.ClineMessage
import com.cline.model.MessageType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger

class ClineMessageHandler : ClineMessageListener {
    private val logger = Logger.getInstance(ClineMessageHandler::class.java)

    override fun onMessageReceived(message: ClineMessage) {
        logger.info("Received Cline message: ${message.type} - ${message.content}")
        
        when (message.type) {
            MessageType.TASK_REQUEST -> handleTaskRequest(message)
            MessageType.TASK_UPDATE -> broadcastMessage(message)
            MessageType.TASK_COMPLETE -> broadcastMessage(message)
            MessageType.ERROR -> handleError(message)
            MessageType.INFO -> broadcastMessage(message)
        }
    }

    private fun handleTaskRequest(message: ClineMessage) {
        // For now, just echo back a completion message
        // This will be replaced with actual task handling in Phase 3
        val response = ClineMessage(
            type = MessageType.TASK_COMPLETE,
            content = "Received your request: ${message.content}"
        )
        broadcastMessage(response)
    }

    private fun handleError(message: ClineMessage) {
        logger.error(message.content)
        broadcastMessage(message)
    }

    private fun broadcastMessage(message: ClineMessage) {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(ClineTopics.CLINE_MESSAGES)
                .onMessageReceived(message)
        }
    }
}
