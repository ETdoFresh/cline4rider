package com.etdofresh.cline4rider

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger

@Service(Service.Level.APP)
class ClineApplicationService {
    companion object {
        private val LOG = Logger.getInstance(ClineApplicationService::class.java)
        
        val instance: ClineApplicationService
            get() = ApplicationManager.getApplication().service()
    }

    init {
        LOG.info("ClineApplicationService initialized")
    }
}
