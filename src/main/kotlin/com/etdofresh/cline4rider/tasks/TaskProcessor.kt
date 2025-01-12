package com.etdofresh.cline4rider.tasks

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.etdofresh.cline4rider.model.ClineMessage

class TaskProcessor(private val project: Project) {
    private val logger = Logger.getInstance(TaskProcessor::class.java)
    private val toolParser = ToolParser()
    private val commandExecutor = CommandExecutor(project)

    fun processAssistantResponse(response: ClineMessage): String? {
        return try {
            // Extract text content from the message
            val textContent = response.content
                .filterIsInstance<ClineMessage.Content.Text>()
                .joinToString("\n") { it.text }

            processAssistantResponse(textContent)
        } catch (e: Exception) {
            logger.error("Failed to process assistant response", e)
            null
        }
    }

    fun processAssistantResponse(response: String): String? {
        return try {
            // Extract tool from the response
            val tool = toolParser.parseToolFromResponse(response) ?: return null

            // Execute the command
            val result = commandExecutor.executeCommand(tool)
            if (!result.success) return null

            // Format response based on tool type and result
            formatToolResponse(tool.name, tool.parameters, result)
        } catch (e: Exception) {
            logger.error("Failed to process assistant response", e)
            null
        }
    }

    private fun formatToolResponse(toolName: String, params: Map<String, String>, result: CommandExecutor.CommandResult): String {
        val path = params["path"]
        val baseResponse = when (toolName) {
            "read_file" -> "[read_file for '$path'] Result:"
            "write_to_file" -> "[write_to_file for '$path'] Result:"
            "replace_in_file" -> "[replace_in_file for '$path'] Result:"
            "list_files" -> "[list_files for '$path'] Result:"
            "search_files" -> {
                val regex = params["regex"]
                "[search_files for '$path' with regex '$regex'] Result:"
            }
            "list_code_definition_names" -> "[list_code_definition_names for '$path'] Result:"
            "execute_command" -> {
                val command = params["command"]
                "[execute_command for '$command'] Result:"
            }
            "attempt_completion" -> "[attempt_completion] Result:"
            else -> "[${toolName}] Result:"
        }

        return if (result.content != null) {
            "$baseResponse ${result.content}"
        } else {
            "$baseResponse Operation completed successfully"
        }
    }
}
