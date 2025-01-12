package com.etdofresh.cline4rider.tasks

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.etdofresh.cline4rider.model.ClineMessage

class TaskProcessor(private val project: Project) {
    private val logger = Logger.getInstance(TaskProcessor::class.java)
    private val toolParser = ToolParser()
    private val commandExecutor = CommandExecutor(project)

    fun processAssistantResponse(response: ClineMessage): Boolean {
        return try {
            // Extract text content from the message
            val textContent = response.content
                .filterIsInstance<ClineMessage.Content.Text>()
                .joinToString("\n") { it.text }

            // Extract tool from the response
            val tool = toolParser.parseToolFromResponse(textContent)
                ?: return false // No tool found in response

            // Execute the command
            commandExecutor.executeCommand(tool)
        } catch (e: Exception) {
            logger.error("Failed to process assistant response", e)
            false
        }
    }

    fun processAssistantResponse(response: String): Boolean {
        return try {
            // Extract tool from the response
            val tool = toolParser.parseToolFromResponse(response)
                ?: return false // No tool found in response

            // Execute the command
            commandExecutor.executeCommand(tool)
        } catch (e: Exception) {
            logger.error("Failed to process assistant response", e)
            false
        }
    }
}
