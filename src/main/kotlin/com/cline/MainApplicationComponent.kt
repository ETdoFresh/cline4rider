package com.cline

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger

@Service(Service.Level.APP)
class MainApplicationComponent {

    companion object {
        private val logger = Logger.getInstance(MainApplicationComponent::class.java)
        
        fun getInstance(): MainApplicationComponent {
            return ApplicationManager.getApplication().getService(MainApplicationComponent::class.java)
        }
    }

    init {
        logger.info("Cline for Rider initialized")
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            ClineTopics.CLINE_MESSAGES,
            ClineMessageHandler()
        )
    }
}
