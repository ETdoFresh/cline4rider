package com.etdofresh.cline4rider.api

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.settings.ClineSettings
import com.etdofresh.cline4rider.api.openrouter.OpenRouterClient
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ApiProvider(private val project: Project) {
    private val logger = Logger.getInstance(ApiProvider::class.java)

    fun sendMessages(messages: List<ClineMessage>, onChunk: ((String) -> Unit)? = null): String {
        val settings = ClineSettings.getInstance(project)
        val openRouterClient = OpenRouterClient(settings)
        return openRouterClient.sendMessages(messages, onChunk)
    }

    companion object {
        fun getClient(project: Project): ApiProvider = project.getService(ApiProvider::class.java)
    }
}
