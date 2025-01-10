package com.etdofresh.cline4rider

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class ClineApplicationListener : ProjectManagerListener, AppLifecycleListener {
    companion object {
        private val LOG = Logger.getInstance(ClineApplicationListener::class.java)
    }

    override fun projectOpened(project: Project) {
        LOG.info("Project opened: ${project.name}")
        // Initialize project-specific services
        ClineProjectService.getInstance(project)
    }

    override fun projectClosed(project: Project) {
        LOG.info("Project closed: ${project.name}")
        // Clean up project-specific resources
    }

    override fun appWillBeClosed(isRestart: Boolean) {
        LOG.info("Application shutting down - cleaning up resources")
        // Ensure all VFS operations are complete
    }
}
