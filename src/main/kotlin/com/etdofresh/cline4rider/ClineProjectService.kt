package com.etdofresh.cline4rider

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener

@Service(Service.Level.PROJECT)
class ClineProjectService(private val project: Project) : Disposable {
    companion object {
        private val LOG = Logger.getInstance(ClineProjectService::class.java)
        
        fun getInstance(project: Project): ClineProjectService {
            return project.service()
        }
    }

    init {
        LOG.info("ClineProjectService initialized for project ${project.name}")
        project.messageBus.connect(this).subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
            override fun projectClosing(project: Project) {
                if (project == this@ClineProjectService.project) {
                    dispose()
                }
            }
        })
    }

    override fun dispose() {
        LOG.info("Disposing ClineProjectService for project ${project.name}")
        // Clean up any VFS-related resources here
    }
}
