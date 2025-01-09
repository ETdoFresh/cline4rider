package com.cline.startup

import com.cline.notifications.ClineNotifier
import com.cline.settings.ClineSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class ClineStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        // Ensure we're running in the correct thread and application is ready
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(
            Runnable {
                if (project.isDisposed) return@Runnable
                
                val settings = ClineSettings.getInstance()
                
                if (settings.apiKey.isEmpty()) {
                    ClineNotifier.notifyErrorWithAction(
                        project,
                        "Please configure your API key to start using Cline.",
                        "Welcome to Cline"
                    )
                }
            },
            com.intellij.openapi.application.ModalityState.NON_MODAL
        )
    }
}
