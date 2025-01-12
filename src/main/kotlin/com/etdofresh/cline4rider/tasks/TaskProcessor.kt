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

    fun parseToolFromResponse(response: String): Tool? {
        return try {
            // Extract task content if present
            val taskContent = extractTaskContent(response)
            val contentToProcess = taskContent ?: response

            // Extract tool from the response
            toolParser.parseToolFromResponse(contentToProcess)
        } catch (e: Exception) {
            logger.error("Failed to parse tool from response", e)
            null
        }
    }

    fun processAssistantResponse(response: String): String? {
        return try {
            // Parse tool from response
            val tool = parseToolFromResponse(response) ?: return null

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
