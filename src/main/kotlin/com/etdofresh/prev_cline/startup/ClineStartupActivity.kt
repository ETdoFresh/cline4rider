package com.cline.startup

import com.cline.notifications.ClineNotifier
import com.cline.settings.ClineSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class ClineStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        // Ensure we're running in the correct thread and application is ready
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(
            {
                if (project.isDisposed) return@invokeLater
                
                val settings = ClineSettings.getInstance(project)
                
                if (settings.getApiKey().isNullOrEmpty()) {
                    ClineNotifier.notifyErrorWithAction(
                        project,
                        "Please configure your API key to start using Cline.",
                        "Welcome to Cline"
                    )
                }
            },
            com.intellij.openapi.application.ModalityState.any()
        )
    }
}
