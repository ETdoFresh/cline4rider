package com.etdofresh.cline4rider.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.etdofresh.cline4rider.ClineProjectService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.startup.StartupManager

@com.intellij.openapi.startup.PostStartupActivity
class ClineStartupActivity : ProjectActivity {
    override suspend fun runActivityAsync(project: Project) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Initializing Cline", true) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                // Initialize project-specific services in background
                ClineProjectService.getInstance(project)
            }
        })
    }
}
