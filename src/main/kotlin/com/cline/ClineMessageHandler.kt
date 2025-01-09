package com.cline

import com.cline.model.ClineMessage
import com.cline.model.MessageType
import com.cline.tasks.TaskProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

@Service(Service.Level.APP)
class ClineMessageHandler : ClineMessageListener {
    private val logger = Logger.getInstance(ClineMessageHandler::class.java)
    private var currentTaskId: String? = null

    override fun onMessageReceived(message: ClineMessage) {
        logger.info("Received Cline message: ${message.type} - ${message.content} [TaskId: ${message.metadata["taskId"]}]")
        
        when (message.type) {
            MessageType.TASK_REQUEST -> handleTaskRequest(message)
            MessageType.TASK_UPDATE -> handleTaskUpdate(message)
            MessageType.TASK_COMPLETE -> handleTaskComplete(message)
            MessageType.ERROR -> handleError(message)
            MessageType.INFO -> broadcastMessage(message)
        }
    }

    private fun handleTaskRequest(message: ClineMessage) {
        val taskId = message.metadata["taskId"] ?: return
        currentTaskId = taskId

        // Get the project from metadata if available
        val projectName = message.metadata["project"]
        val project = ProjectManager.getInstance().openProjects.find { it.name == projectName }
        
        if (project == null) {
            handleError(ClineMessage(
                type = MessageType.ERROR,
                content = "No active project found",
                metadata = message.metadata
            ))
            return
        }

        try {
            // Process the task using TaskProcessor
            val processor = project.getService(TaskProcessor::class.java)
            val response = processor.processTask(taskId, message.content)
            
            // Add metadata to the response
            val enrichedResponse = response.copy(
                metadata = response.metadata + mapOf(
                    "taskId" to taskId,
                    "timestamp" to System.currentTimeMillis().toString(),
                    "project" to project.name,
                    "status" to if (response.type == MessageType.ERROR) "error" else "completed"
                )
            )
            
            broadcastMessage(enrichedResponse)
        } catch (e: Exception) {
            logger.error("Error processing task", e)
            handleError(ClineMessage(
                type = MessageType.ERROR,
                content = "Error processing task: ${e.message}",
                metadata = message.metadata
            ))
        }
    }

    private fun handleTaskUpdate(message: ClineMessage) {
        val taskId = message.metadata["taskId"]
        if (taskId == currentTaskId) {
            broadcastMessage(message)
        } else {
            logger.warn("Received update for unknown task: $taskId")
        }
    }

    private fun handleTaskComplete(message: ClineMessage) {
        val taskId = message.metadata["taskId"]
        if (taskId == currentTaskId) {
            currentTaskId = null
            broadcastMessage(message)
        } else {
            logger.warn("Received completion for unknown task: $taskId")
        }
    }

    private fun handleError(message: ClineMessage) {
        logger.error("Error in task ${message.metadata["taskId"]}: ${message.content}")
        broadcastMessage(message.copy(
            metadata = message.metadata + mapOf(
                "timestamp" to System.currentTimeMillis().toString(),
                "severity" to "error"
            )
        ))
    }

    private fun broadcastMessage(message: ClineMessage) {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(ClineTopics.CLINE_MESSAGES)
                .onMessageReceived(message)
        }
    }

    companion object {
        fun getInstance(): ClineMessageHandler {
            return ApplicationManager.getApplication().getService(ClineMessageHandler::class.java)
        }
    }
}
