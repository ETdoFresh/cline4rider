package com.cline

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener

@Service(Service.Level.APP)
class MainApplicationComponent {
    private val logger = Logger.getInstance(MainApplicationComponent::class.java)
    private val connection = ApplicationManager.getApplication().messageBus.connect()
    private val messageHandler = ClineMessageHandler.getInstance()

    init {
        logger.info("Initializing Cline for Rider")
        setupMessageBus()
        setupProjectListener()
        logger.info("Cline for Rider initialized successfully")
    }

    private fun setupMessageBus() {
        connection.subscribe(ClineTopics.CLINE_MESSAGES, messageHandler)
        logger.info("Message bus subscription completed")
    }

    private fun setupProjectListener() {
        connection.subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
            override fun projectOpened(project: Project) {
                logger.info("Project opened: ${project.name}")
                initializeProjectServices(project)
            }

            override fun projectClosed(project: Project) {
                logger.info("Project closed: ${project.name}")
            }
        })
    }

    private fun initializeProjectServices(project: Project) {
        try {
            // Initialize project-level services here when needed
            logger.info("Project services initialized for: ${project.name}")
        } catch (e: Exception) {
            logger.error("Failed to initialize project services", e)
        }
    }

    companion object {
        fun getInstance(): MainApplicationComponent {
            return ApplicationManager.getApplication().getService(MainApplicationComponent::class.java)
        }
    }
}
