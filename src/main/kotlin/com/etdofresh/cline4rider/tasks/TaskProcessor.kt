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
            // First check if this is a task completion message without a tool call
            if (containsTaskCompletion(response)) {
                // If it's a task completion message and doesn't use attempt_completion tool,
                // return error immediately
                if (!containsToolUsage(response) || !response.contains("<attempt_completion>")) {
                    return """[ERROR] Task completion detected without using the attempt_completion tool! Please retry with proper tool use.

# Reminder: Task Completion Format

When completing a task, you must use the attempt_completion tool:

<attempt_completion>
<result>
Your task completion message here...
</result>
</attempt_completion>

Always use this format to properly complete tasks.
(This is an automated message, so do not respond to it conversationally.)"""
                }
            }

            // Check if the response contains any XML-like tool usage
            if (!containsToolUsage(response)) {
                return """[ERROR] You did not use a tool in your previous response! Please retry with a tool use.

# Reminder: Instructions for Tool Use

Tool uses are formatted using XML-style tags. The tool name is enclosed in opening and closing tags, and each parameter is similarly enclosed within its own set of tags. Here's the structure:

<tool_name>
<parameter1_name>value1</parameter1_name>
<parameter2_name>value2</parameter2_name>
...
</tool_name>

For example:

<attempt_completion>
<result>
I have completed the task...
</result>
</attempt_completion>

Always adhere to this format for all tool uses to ensure proper parsing and execution.

# Next Steps

If you have completed the user's task, use the attempt_completion tool. 
If you require additional information from the user, use the ask_followup_question tool. 
Otherwise, if you have not completed the task and do not need additional information, then proceed with the next step of the task. 
(This is an automated message, so do not respond to it conversationally.)"""
            }

            // Now try to parse the tool
            val tool = parseToolFromResponse(response)
            if (tool == null) {
                logger.warn("Failed to parse tool from response")
                return "Failed to parse tool from response. Please check the XML format and try again."
            }

            // Check for required parameters based on tool type
            val missingParam = checkMissingRequiredParams(tool)
            if (missingParam != null) {
                logger.warn("Missing required parameter: $missingParam")
                return "Missing value for required parameter '${missingParam}'. Please retry with complete response."
            }

            // Execute the command
            val result = commandExecutor.executeCommand(tool)
            if (!result.success) {
                logger.warn("Command execution failed: ${result.content}")
                return result.content ?: "Command execution failed"
            }

            // For attempt_completion, return null to suppress any output
            if (tool.name == "attempt_completion") {
                return null
            }

            // Format response based on tool type and result
            formatToolResponse(tool.name, tool.parameters, result)
        } catch (e: Exception) {
            logger.error("Failed to process assistant response", e)
            null
        }
    }

    private fun containsTaskCompletion(response: String): Boolean {
        // Check for task completion message patterns
        val patterns = listOf(
            // Pattern 1: Bold title followed by description with newlines
            """\*\*[^*]+\*\*\s*\n\n.+""".toRegex(RegexOption.DOT_MATCHES_ALL),
            // Pattern 2: Bold title followed by immediate text
            """\*\*[^*]+\*\*\s*\n[^\n]+""".toRegex(),
            // Pattern 3: Just bold title followed by "complete" or "completed"
            """\*\*[^*]+\*\*\s*(?:\n.*(?:complete|completed).*)?""".toRegex(RegexOption.IGNORE_CASE),
            // Pattern 4: Just bold title
            """\*\*[^*]+\*\*""".toRegex()
        )
        
        return patterns.any { it.containsMatchIn(response) }
    }

    fun containsToolUsage(response: String): Boolean {
        val toolTags = listOf(
            "execute_command",
            "read_file",
            "write_to_file",
            "replace_in_file",
            "search_files",
            "list_files",
            "list_code_definition_names",
            "ask_followup_question",
            "attempt_completion"
        )

        // Simple check for <tool_name> pattern
        return toolTags.any { tag ->
            response.contains("<$tag>") && response.contains("</$tag>")
        }
    }

    private fun checkMissingRequiredParams(tool: Tool): String? {
        return when (tool.name) {
            "read_file", "write_to_file", "replace_in_file", "list_files", "search_files", "list_code_definition_names" -> {
                if (!tool.parameters.containsKey("path")) return "path"
                null
            }
            "write_to_file" -> {
                if (!tool.parameters.containsKey("content")) return "content"
                null
            }
            "replace_in_file" -> {
                if (!tool.parameters.containsKey("diff")) return "diff"
                null
            }
            "search_files" -> {
                if (!tool.parameters.containsKey("regex")) return "regex"
                null
            }
            "execute_command" -> {
                if (!tool.parameters.containsKey("command")) return "command"
                if (!tool.parameters.containsKey("requires_approval")) return "requires_approval"
                null
            }
            "attempt_completion" -> {
                if (!tool.parameters.containsKey("result")) return "result"
                null
            }
            "ask_followup_question" -> {
                if (!tool.parameters.containsKey("question")) return "question"
                null
            }
            "use_mcp_tool" -> {
                when {
                    !tool.parameters.containsKey("server_name") -> "server_name"
                    !tool.parameters.containsKey("tool_name") -> "tool_name"
                    !tool.parameters.containsKey("arguments") -> "arguments"
                    else -> null
                }
            }
            "access_mcp_resource" -> {
                when {
                    !tool.parameters.containsKey("server_name") -> "server_name"
                    !tool.parameters.containsKey("uri") -> "uri"
                    else -> null
                }
            }
            else -> "Invalid tool name: ${tool.name}"
        }
    }

    fun extractToolXml(response: String): String? {
        return toolParser.extractToolXml(response)
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
