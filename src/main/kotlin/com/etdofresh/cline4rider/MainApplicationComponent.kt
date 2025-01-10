package com.etdofresh.cline4rider

import com.etdofresh.cline4rider.message.ClineMessageHandler
import com.etdofresh.cline4rider.message.TaskRequestListener
import com.etdofresh.cline4rider.model.ClineMessage
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

@Service(Service.Level.PROJECT)
class MainApplicationComponent(private val project: Project) : StartupActivity {
    override fun runActivity(project: Project) {
        // Initialize project-level services
        project.messageBus.connect().subscribe(
            ClineMessageHandler.TASK_REQUEST,
            object : TaskRequestListener {
                override fun onTaskRequest(message: ClineMessage) {
                    // Handle task request
                }
            }
        )
    }
}
