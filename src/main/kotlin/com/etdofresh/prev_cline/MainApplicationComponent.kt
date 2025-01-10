package com.cline

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
                override fun onTaskRequest(message: com.cline.model.ClineMessage) {
                    // Handle task request
                }
            }
        )
    }
}
