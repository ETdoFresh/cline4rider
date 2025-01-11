package com.etdofresh.cline4rider.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.etdofresh.cline4rider.ClineProjectService

class ClineStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        // Initialize project-specific services
        ClineProjectService.getInstance(project)
    }
}
