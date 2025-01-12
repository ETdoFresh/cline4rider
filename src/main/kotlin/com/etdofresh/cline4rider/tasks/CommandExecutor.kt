package com.etdofresh.cline4rider.tasks

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.editor.Document

class CommandExecutor(private val project: Project) {
    private val logger = Logger.getInstance(CommandExecutor::class.java)

    fun executeCommand(tool: Tool): Boolean {
        return when (tool.name) {
            "write_to_file" -> handleWriteToFile(tool.parameters)
            "read_file" -> handleReadFile(tool.parameters)
            "replace_in_file" -> handleReplaceInFile(tool.parameters)
            "list_files" -> handleListFiles(tool.parameters)
            "search_files" -> handleSearchFiles(tool.parameters)
            "list_code_definition_names" -> handleListCodeDefinitions(tool.parameters)
            "execute_command" -> handleExecuteCommand(tool.parameters)
            else -> {
                logger.warn("Unsupported tool: ${tool.name}")
                false
            }
        }
    }

    private fun handleWriteToFile(params: Map<String, String>): Boolean {
        val path = params["path"] ?: return false
        val content = params["content"] ?: return false

        return try {
            WriteCommandAction.runWriteCommandAction(project) {
                val file = createOrGetFile(path)
                val document = FileDocumentManager.getInstance().getDocument(file)
                    ?: throw IllegalStateException("Could not get document for file: $path")
                document.setText(content)
                FileDocumentManager.getInstance().saveDocument(document)
            }
            true
        } catch (e: Exception) {
            logger.error("Failed to write to file: $path", e)
            false
        }
    }

    private fun handleReadFile(params: Map<String, String>): Boolean {
        val path = params["path"] ?: return false
        
        return try {
            val file = LocalFileSystem.getInstance().findFileByPath(path)
                ?: throw IllegalStateException("File not found: $path")
            val document = FileDocumentManager.getInstance().getDocument(file)
                ?: throw IllegalStateException("Could not get document for file: $path")
            // Here you would typically send the content somewhere or process it
            true
        } catch (e: Exception) {
            logger.error("Failed to read file: $path", e)
            false
        }
    }

    private fun handleReplaceInFile(params: Map<String, String>): Boolean {
        val path = params["path"] ?: return false
        val diff = params["diff"] ?: return false

        return try {
            WriteCommandAction.runWriteCommandAction(project) {
                val file = LocalFileSystem.getInstance().findFileByPath(path)
                    ?: throw IllegalStateException("File not found: $path")
                val document = FileDocumentManager.getInstance().getDocument(file)
                    ?: throw IllegalStateException("Could not get document for file: $path")
                
                // Process the diff and apply changes
                // This is a simplified version - you'll need to implement proper diff parsing
                document.setText(processDiff(document.text, diff))
                FileDocumentManager.getInstance().saveDocument(document)
            }
            true
        } catch (e: Exception) {
            logger.error("Failed to replace in file: $path", e)
            false
        }
    }

    private fun handleListFiles(params: Map<String, String>): Boolean {
        // Implement file listing logic
        return true
    }

    private fun handleSearchFiles(params: Map<String, String>): Boolean {
        // Implement file search logic
        return true
    }

    private fun handleListCodeDefinitions(params: Map<String, String>): Boolean {
        // Implement code definition listing logic
        return true
    }

    private fun handleExecuteCommand(params: Map<String, String>): Boolean {
        // Implement command execution logic
        return true
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

    private fun processDiff(originalContent: String, diff: String): String {
        return diffProcessor.processDiff(originalContent, diff)
    }
}
