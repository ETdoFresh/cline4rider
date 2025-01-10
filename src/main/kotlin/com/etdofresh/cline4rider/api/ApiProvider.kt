package com.etdofresh.cline4rider.api

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.message.ClineMessageHandler
import com.etdofresh.cline4rider.api.openai.OpenAIClient
import com.etdofresh.cline4rider.api.anthropic.AnthropicClient
import com.etdofresh.cline4rider.api.deepseek.DeepSeekClient
import com.etdofresh.cline4rider.api.openrouter.OpenRouterClient
import com.etdofresh.cline4rider.api.openaicompatible.OpenAICompatibleClient

class ApiProvider(private val messageHandler: ClineMessageHandler) {
    private val clients = mutableMapOf<String, Any>()
    
    init {
        registerClient("openai", OpenAIClient())
        registerClient("anthropic", AnthropicClient())
        registerClient("deepseek", DeepSeekClient())
        registerClient("openrouter", OpenRouterClient())
        registerClient("openaicompatible", OpenAICompatibleClient())
    }

    fun registerClient(name: String, client: Any) {
        clients[name] = client
    }

    fun getClient(name: String): Any? {
        return clients[name]
    }

    fun processMessage(message: ClineMessage) {
        // Message processing logic
    }
}
