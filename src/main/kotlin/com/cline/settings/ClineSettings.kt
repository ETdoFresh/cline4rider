package com.cline.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.net.InetSocketAddress
import java.net.Proxy

@Service(Service.Level.APP)
@State(
    name = "ClineSettings",
    storages = [Storage("cline.xml")]
)
class ClineSettings : PersistentStateComponent<ClineSettings.State> {
    private var myState = State()

    var apiKey: String
        get() = myState.apiKey
        set(value) {
            myState.apiKey = value
        }

    var model: String
        get() = myState.model
        set(value) {
            myState.model = value
        }

    var maxTokens: Int
        get() = myState.maxTokens
        set(value) {
            myState.maxTokens = value
        }

    var temperature: Double
        get() = myState.temperature
        set(value) {
            myState.temperature = value
        }

    var useProxy: Boolean
        get() = myState.useProxy
        set(value) {
            myState.useProxy = value
        }

    var proxyHost: String
        get() = myState.proxyHost
        set(value) {
            myState.proxyHost = value
        }

    var proxyPort: Int
        get() = myState.proxyPort
        set(value) {
            myState.proxyPort = value
        }

    fun getProxy(): Proxy? {
        if (!useProxy || proxyHost.isEmpty() || proxyPort <= 0 || proxyPort > 65535) {
            return null
        }
        return Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
    }

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun resetToDefaults() {
        myState = State()
    }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (apiKey.isEmpty()) {
            errors.add("API key is required")
        }

        if (maxTokens <= 0) {
            errors.add("Max tokens must be greater than 0")
        }

        if (temperature < 0.0 || temperature > 1.0) {
            errors.add("Temperature must be between 0.0 and 1.0")
        }

        if (useProxy) {
            if (proxyHost.isEmpty()) {
                errors.add("Proxy host is required when using proxy")
            }
            if (proxyPort <= 0 || proxyPort > 65535) {
                errors.add("Proxy port must be between 1 and 65535")
            }
        }

        return errors
    }

    data class State(
        var apiKey: String = "",
        var model: String = "claude-3-opus-20240229",
        var maxTokens: Int = 4096,
        var temperature: Double = 0.7,
        var useProxy: Boolean = false,
        var proxyHost: String = "",
        var proxyPort: Int = 0
    )

    companion object {
        @JvmStatic
        fun getInstance(): ClineSettings = ApplicationManager.getApplication().getService(ClineSettings::class.java)
    }
}
