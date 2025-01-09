package com.cline.tasks

import com.cline.model.ClineMessage
import com.cline.model.MessageType
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager

@Service(Service.Level.PROJECT)
class TaskProcessor(private val project: Project) {
    private val logger = Logger.getInstance(TaskProcessor::class.java)

    fun processTask(taskId: String, content: String): ClineMessage {
        logger.info("Processing task: $taskId")
        
        // Basic command detection
        return when {
            content.startsWith("list") -> listFiles()
            content.startsWith("find") -> findInFiles(content)
            content.startsWith("help") -> showHelp()
            else -> analyzeRequest(content)
        }
    }

    private fun listFiles(): ClineMessage {
        val projectDir = project.basePath ?: return errorMessage("Project directory not found")
        val baseDir = VirtualFileManager.getInstance().findFileByUrl("file://$projectDir")
            ?: return errorMessage("Cannot access project directory")

        val fileList = StringBuilder()
        fileList.append("Project files:\n")
        
        fun listFilesRecursively(dir: com.intellij.openapi.vfs.VirtualFile, indent: String = "") {
            dir.children.sortedBy { it.name }.forEach { file ->
                fileList.append("$indent${if (file.isDirectory) "📁" else "📄"} ${file.name}\n")
                if (file.isDirectory) {
                    listFilesRecursively(file, "$indent  ")
                }
            }
        }
        
        listFilesRecursively(baseDir)
        
        return ClineMessage(
            type = MessageType.TASK_COMPLETE,
            content = fileList.toString()
        )
    }

    private fun findInFiles(query: String): ClineMessage {
        val searchTerm = query.removePrefix("find").trim()
        if (searchTerm.isEmpty()) {
            return errorMessage("Please specify what to find")
        }

        val results = StringBuilder()
        val projectDir = project.basePath ?: return errorMessage("Project directory not found")
        val baseDir = VirtualFileManager.getInstance().findFileByUrl("file://$projectDir")
            ?: return errorMessage("Cannot access project directory")

        results.append("Searching for: $searchTerm\n\n")
        
        try {
            val psiManager = PsiManager.getInstance(project)
            baseDir.children.forEach { file ->
                if (!file.isDirectory && file.extension == "kt") {
                    val content = String(file.contentsToByteArray())
                    if (content.contains(searchTerm)) {
                        val lineNumbers = content.lines().mapIndexedNotNull { index, line ->
                            if (line.contains(searchTerm)) index + 1 else null
                        }
                        results.append("📄 ${file.name} (lines: ${lineNumbers.joinToString(", ")})\n")
                        
                        // Show context for each match
                        lineNumbers.forEach { lineNum ->
                            val lines = content.lines()
                            val start = maxOf(0, lineNum - 2)
                            val end = minOf(lines.size, lineNum + 1)
                            results.append("\n    Context (line $lineNum):\n")
                            for (i in start until end) {
                                val prefix = if (i + 1 == lineNum) "  → " else "    "
                                results.append("    $prefix${lines[i].trim()}\n")
                            }
                            results.append("\n")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error searching files", e)
            return errorMessage("Error searching files: ${e.message}")
        }

        return ClineMessage(
            type = MessageType.TASK_COMPLETE,
            content = results.toString()
        )
    }

    private fun showHelp(): ClineMessage {
        return ClineMessage(
            type = MessageType.TASK_COMPLETE,
            content = """
                Available Commands:
                
                📁 File Operations:
                - list: Show project files in tree structure
                - find <text>: Search in project files with context
                
                ℹ️ General:
                - help: Show this help message
                
                💡 Tips:
                - Use 'find' to search for specific code or text
                - File tree shows folders (📁) and files (📄)
                - Search results include line numbers and context
                
                More features coming soon!
            """.trimIndent()
        )
    }

    private fun analyzeRequest(content: String): ClineMessage {
        return ClineMessage(
            type = MessageType.TASK_COMPLETE,
            content = """
                I understand you want to: $content
                
                🤖 Currently Available Features:
                
                📁 File Operations:
                - Type 'list' to see project files
                - Type 'find <text>' to search in files
                
                ℹ️ Help:
                - Type 'help' for command list
                
                💡 Tip: Try starting with 'list' to explore the project structure
                or 'help' to see all available commands.
                
                Full AI task processing coming in the next phase!
            """.trimIndent()
        )
    }

    private fun errorMessage(message: String): ClineMessage {
        return ClineMessage(
            type = MessageType.ERROR,
            content = message
        )
    }
}
