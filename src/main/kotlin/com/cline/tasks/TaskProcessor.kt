package com.cline.tasks

import com.cline.model.ClineMessage
import com.cline.task.model.ClineTask
import com.intellij.openapi.project.Project
import java.time.Instant

class TaskProcessor(private val project: Project) {
    private val messages = mutableListOf<ClineMessage>()
    private var currentTask: ClineTask? = null

    fun processTask(task: ClineTask) {
        currentTask = task
        messages.clear()

        // Add initial system message
        addSystemMessage("""
            Processing task: ${task.title}
            Description: ${task.description}
            Created: ${Instant.ofEpochMilli(task.createdAt)}
        """.trimIndent())
    }

    fun addUserMessage(content: String) {
        messages.add(ClineMessage(
            role = "user",
            content = content,
            timestamp = System.currentTimeMillis()
        ))
    }

    fun addAssistantMessage(content: String) {
        messages.add(ClineMessage(
            role = "assistant",
            content = content,
            timestamp = System.currentTimeMillis()
        ))
    }

    fun addSystemMessage(content: String) {
        messages.add(ClineMessage(
            role = "system",
            content = content,
            timestamp = System.currentTimeMillis()
        ))
    }

    fun getMessages(): List<ClineMessage> = messages.toList()

    fun getCurrentTask(): ClineTask? = currentTask

    fun clear() {
        messages.clear()
        currentTask = null
    }
}
