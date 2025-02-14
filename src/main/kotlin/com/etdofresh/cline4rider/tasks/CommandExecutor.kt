package com.etdofresh.cline4rider.tasks

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.DefaultActionGroup
import javax.swing.JComponent
import javax.swing.text.JTextComponent
import java.awt.Component

class CommandExecutor(private val project: Project) {
    private val logger = Logger.getInstance(CommandExecutor::class.java)
    private var currentConsole: ConsoleView? = null
    private var currentProcessHandler: OSProcessHandler? = null

    fun executeCommand(tool: Tool): CommandResult {
        return when (tool.name) {
            "write_to_file" -> handleWriteToFile(tool.parameters)
            "read_file" -> handleReadFile(tool.parameters)
            "replace_in_file" -> handleReplaceInFile(tool.parameters)
            "list_files" -> handleListFiles(tool.parameters)
            "search_files" -> handleSearchFiles(tool.parameters)
            "list_code_definition_names" -> handleListCodeDefinitions(tool.parameters)
            "execute_command" -> handleExecuteCommand(tool.parameters)
            "attempt_completion" -> handleAttemptCompletion(tool.parameters)
            "ask_followup_question" -> handleAskFollowupQuestion(tool.parameters)
            else -> CommandResult(false, "Unsupported tool: ${tool.name}")
        }
    }

    fun getCurrentOutput(): String {
        return currentConsole?.let { console ->
            // Get text from console component
            fun findTextComponent(component: Component): JTextComponent? {
                if (component is JTextComponent) {
                    return component
                }
                if (component is JComponent) {
                    for (child in component.components) {
                        val found = findTextComponent(child)
                        if (found != null) {
                            return found
                        }
                    }
                }
                return null
            }
            
            findTextComponent(console.component)?.text ?: ""
        } ?: ""
    }

    fun isProcessRunning(): Boolean {
        return currentProcessHandler?.let { handler ->
            val isAlive = !handler.isProcessTerminated && handler.process?.isAlive == true
            val hasOutput = getCurrentOutput().isNotEmpty()
            isAlive || hasOutput
        } ?: false
    }

    private fun handleAttemptCompletion(params: Map<String, String>): CommandResult {
        // Return success with no content to prevent showing response or approve buttons
        return CommandResult(true, null)
    }

    private fun handleAskFollowupQuestion(params: Map<String, String>): CommandResult {
        val question = params["question"] ?: return CommandResult(false, "Question not provided")
        return CommandResult(true, question)
    }

    private fun handleWriteToFile(params: Map<String, String>): CommandResult {
        val path = params["path"] ?: return CommandResult(false, "File path not provided")
        val content = params["content"] ?: return CommandResult(false, "Content not provided")
        val fullPath = "${project.basePath}/$path"

        return try {
            var result: CommandResult? = null
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
                try {
                    WriteCommandAction.runWriteCommandAction(project) {
                        val file = createOrGetFile(fullPath)
                        val document = FileDocumentManager.getInstance().getDocument(file)
                            ?: throw IllegalStateException("Could not get document for file: $path")
                        document.setText(content)
                        FileDocumentManager.getInstance().saveDocument(document)
                    }
                    result = CommandResult(true, "File successfully written: $path")
                } catch (e: Exception) {
                    result = CommandResult(false, e.message)
                }
            }
            result ?: CommandResult(false, "Failed to write file")
        } catch (e: Exception) {
            logger.error("Failed to write to file: $path", e)
            CommandResult(false, e.message)
        }
    }

    data class CommandResult(
        val success: Boolean,
        val content: String? = null
    )

    private fun handleReadFile(params: Map<String, String>): CommandResult {
        val path = params["path"] ?: return CommandResult(false, "File path not provided")
        val fullPath = "${project.basePath}/$path"
        
        return try {
            var result: CommandResult? = null
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
                try {
                    val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
                        ?: throw IllegalStateException("File not found: $path")
                    val document = FileDocumentManager.getInstance().getDocument(file)
                        ?: throw IllegalStateException("Could not get document for file: $path")
                    result = CommandResult(true, document.text)
                } catch (e: Exception) {
                    result = CommandResult(false, e.message)
                }
            }
            result ?: CommandResult(false, "Failed to read file")
        } catch (e: Exception) {
            logger.error("Failed to read file: $path", e)
            CommandResult(false, e.message)
        }
    }

    private fun handleReplaceInFile(params: Map<String, String>): CommandResult {
        val path = params["path"] ?: return CommandResult(false, "File path not provided")
        val diff = params["diff"] ?: return CommandResult(false, "Diff not provided")
        val fullPath = "${project.basePath}/$path"

        return try {
            var result: CommandResult? = null
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
                try {
                    WriteCommandAction.runWriteCommandAction(project) {
                        val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
                            ?: throw IllegalStateException("File not found: $path")
                        val document = FileDocumentManager.getInstance().getDocument(file)
                            ?: throw IllegalStateException("Could not get document for file: $path")
                        
                        val diffResult = processDiff(document.text, diff)
                        if (!diffResult.success) {
                            throw IllegalStateException(diffResult.error ?: "Failed to process diff")
                        }
                        document.setText(diffResult.content)
                        FileDocumentManager.getInstance().saveDocument(document)
                    }
                    result = CommandResult(true, "File successfully updated: $path")
                } catch (e: Exception) {
                    result = CommandResult(false, e.message)
                }
            }
            result ?: CommandResult(false, "Failed to update file")
        } catch (e: Exception) {
            logger.warn("Failed to replace in file: $path - ${e.message}")
            CommandResult(false, e.message)
        }
    }

    private fun handleListFiles(params: Map<String, String>): CommandResult {
        val path = params["path"] ?: return CommandResult(false, "Path not provided")
        val recursive = params["recursive"]?.toBoolean() ?: false
        val fullPath = "${project.basePath}/$path"

        return try {
            var result: CommandResult? = null
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
                try {
                    val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
                        ?: throw IllegalStateException("Directory not found: $path")
                    
                    val files = if (recursive) {
                        collectFilesRecursively(file)
                    } else {
                        file.children.map { it.path }.toList()
                    }
                    
                    result = CommandResult(true, files.joinToString("\n"))
                } catch (e: Exception) {
                    result = CommandResult(false, e.message)
                }
            }
            result ?: CommandResult(false, "Failed to list files")
        } catch (e: Exception) {
            logger.error("Failed to list files: $path", e)
            CommandResult(false, e.message)
        }
    }

    private fun handleSearchFiles(params: Map<String, String>): CommandResult {
        val path = params["path"] ?: return CommandResult(false, "Path not provided")
        val regex = params["regex"] ?: return CommandResult(false, "Regex not provided")
        val filePattern = params["file_pattern"]
        val fullPath = "${project.basePath}/$path"

        return try {
            var result: CommandResult? = null
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
                try {
                    val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
                        ?: throw IllegalStateException("Directory not found: $path")
                    
                    // TODO: Implement file search logic
                    result = CommandResult(true, "Search completed")
                } catch (e: Exception) {
                    result = CommandResult(false, e.message)
                }
            }
            result ?: CommandResult(false, "Failed to search files")
        } catch (e: Exception) {
            logger.error("Failed to search files: $path", e)
            CommandResult(false, e.message)
        }
    }

    private fun handleListCodeDefinitions(params: Map<String, String>): CommandResult {
        val path = params["path"] ?: return CommandResult(false, "Path not provided")
        val fullPath = "${project.basePath}/$path"

        return try {
            var result: CommandResult? = null
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
                try {
                    val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
                        ?: throw IllegalStateException("Directory not found: $path")
                    
                    // TODO: Implement code definition listing logic
                    result = CommandResult(true, "Code definitions listed")
                } catch (e: Exception) {
                    result = CommandResult(false, e.message)
                }
            }
            result ?: CommandResult(false, "Failed to list code definitions")
        } catch (e: Exception) {
            logger.error("Failed to list code definitions: $path", e)
            CommandResult(false, e.message)
        }
    }

    private fun handleExecuteCommand(params: Map<String, String>): CommandResult {
        val command = params["command"] ?: return CommandResult(false, "Command not provided")
        val requiresApproval = params["requires_approval"]?.toBoolean() ?: true

        return try {
            var success = false
            // Clear previous console and process handler
            currentConsole = null
            currentProcessHandler = null
            
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
                try {
                    // Get working directory
                    val workingDir = project.basePath?.let { java.io.File(it) }
                        ?: throw IllegalStateException("Project base path not found")

                    // Create process handler
                    val processBuilder = ProcessBuilder()
                    processBuilder.directory(workingDir)
                    
                    // Set up command
                    val commandList = if (System.getProperty("os.name").lowercase().contains("windows")) {
                        listOf("cmd.exe", "/c", command)
                    } else {
                        listOf("sh", "-c", command)
                    }
                    processBuilder.command(commandList)
                    
                    // Start process
                    val process = processBuilder.start()
                    val processHandler = OSProcessHandler(process, command)
                    currentProcessHandler = processHandler
                    
                    // Create and store console
                    val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
                    val console = consoleBuilder.console
                    currentConsole = console
                    
                    // Create actions toolbar
                    val actions = DefaultActionGroup()
                    
                    // Get command name without arguments
                    val commandName = command.split(" ").first()
                    
                    // Create content descriptor
                    val descriptor = RunContentDescriptor(
                        console,
                        processHandler,
                        console.component,
                        "Cline ($commandName)",
                        null
                    )
                    
                    // Show in Run tool window
                    RunContentManager.getInstance(project).showRunContent(
                        DefaultRunExecutor.getRunExecutorInstance(),
                        descriptor
                    )
                    
                    // Start process and attach console
                    console.attachToProcess(processHandler)
                    processHandler.startNotify()
                    
                    success = true
                } catch (e: Exception) {
                    throw e
                }
            }

            if (success) {
                CommandResult(true, "Command sent to terminal: $command")
            } else {
                CommandResult(false, "Failed to send command to terminal")
            }
        } catch (e: Exception) {
            logger.error("Failed to execute command: $command", e)
            CommandResult(false, e.message)
        }
    }

    private fun collectFilesRecursively(file: VirtualFile): List<String> {
        val result = mutableListOf<String>()
        if (!file.isDirectory) {
            result.add(file.path)
        } else {
            file.children.forEach { child ->
                result.addAll(collectFilesRecursively(child))
            }
        }
        return result
    }

    private fun createOrGetFile(path: String): VirtualFile {
        LocalFileSystem.getInstance().findFileByPath(path)?.let { return it }

        // Create the file if it doesn't exist
        val parentPath = path.substringBeforeLast('/')
        val fileName = path.substringAfterLast('/')
        
        val parentDir = createDirectoryIfNeeded(parentPath)
        var newFile: VirtualFile? = null
        WriteCommandAction.runWriteCommandAction(project) {
            newFile = parentDir.createChildData(this, fileName)
        }
        return newFile ?: throw IllegalStateException("Failed to create file: $path")
    }

    private fun createDirectoryIfNeeded(path: String): VirtualFile {
        LocalFileSystem.getInstance().findFileByPath(path)?.let { return it }

        val parts = path.split('/')
        var current = LocalFileSystem.getInstance().findFileByPath(parts[0])
            ?: throw IllegalStateException("Root directory not found")

        for (i in 1 until parts.size) {
            val part = parts[i]
            val next = current.findChild(part)
            if (next == null) {
                var newDir: VirtualFile? = null
                WriteCommandAction.runWriteCommandAction(project) {
                    newDir = current.createChildDirectory(this, part)
                }
                current = newDir ?: throw IllegalStateException("Failed to create directory: ${parts.take(i + 1).joinToString("/")}")
            } else {
                current = next
            }
        }
        return current
    }

    private val diffProcessor = DiffProcessor()

    private fun processDiff(originalContent: String, diff: String): DiffProcessor.DiffResult {
        return diffProcessor.processDiff(originalContent, diff)
    }
}
