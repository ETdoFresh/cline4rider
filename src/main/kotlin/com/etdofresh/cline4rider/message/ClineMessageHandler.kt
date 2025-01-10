package com.etdofresh.cline4rider.message

import com.etdofresh.cline4rider.model.ClineMessage
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

@Service(Service.Level.PROJECT)
class ClineMessageHandler(private val project: Project) {
    companion object {
        val TASK_REQUEST = Topic.create("Cline Task Request", TaskRequestListener::class.java)
        val TASK_COMPLETE = Topic.create("Cline Task Complete", TaskCompleteListener::class.java)
        val TASK_ERROR = Topic.create("Cline Task Error", TaskErrorListener::class.java)
    }
}

interface TaskRequestListener {
    fun onTaskRequest(message: ClineMessage)
}

interface TaskCompleteListener {
    fun onTaskComplete(message: ClineMessage)
}

interface TaskErrorListener {
    fun onTaskError(message: ClineMessage, error: String)
}
