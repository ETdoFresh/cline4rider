package com.etdofresh.cline4rider.api

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.settings.ClineSettings
import com.etdofresh.cline4rider.api.openrouter.OpenRouterClient
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

data class ResponseStats(
    val total_cost: Double? = null,
    val tokens_prompt: Int? = null,
    val tokens_completion: Int? = null,
    val native_tokens_prompt: Int? = null,
    val native_tokens_completion: Int? = null,
    val cache_discount: Double? = null
)

@Service(Service.Level.PROJECT)
class ApiProvider(private val project: Project) {
    private val logger = Logger.getInstance(ApiProvider::class.java)

    private var openRouterClient: OpenRouterClient? = null

    fun sendMessages(messages: List<ClineMessage>, onChunk: ((String, ResponseStats?) -> Unit)? = null): String {
        val settings = ClineSettings.getInstance(project)
        
        try {
            // Initialize client if needed
            if (openRouterClient == null) {
                openRouterClient = OpenRouterClient(settings)
            }
            
            // Validate API key
            if (settings.getApiKey().isNullOrEmpty()) {
                throw IllegalStateException("API key is not configured. Please configure your API key in Settings | Tools | Cline")
            }
            
            // Validate provider
            if (settings.state.provider != ClineSettings.Provider.OPENROUTER) {
                throw IllegalStateException("Provider must be set to OpenRouter in Settings | Tools | Cline")
            }
            
            return openRouterClient!!.sendMessages(messages, onChunk)
        } catch (e: Exception) {
            logger.error("Failed to send message", e)
            throw e
        }
    }

    companion object {
        fun getClient(project: Project): ApiProvider = project.getService(ApiProvider::class.java)
    }
}
