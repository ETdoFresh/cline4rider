package com.etdofresh.cline4rider.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "ClineSettings",
    storages = [Storage("cline.xml")]
)
class ClineSettings(private val project: Project) : PersistentStateComponent<ClineSettings.State> {
    private var myState = State()

    enum class Provider {
        OPENAI,
        ANTHROPIC,
        OPENROUTER,
        DEEPSEEK,
        OPENAI_COMPATIBLE
    }

    data class State(
        var provider: Provider = Provider.OPENROUTER,
        var model: String = "openai/gpt-3.5-turbo",
        var temperature: Double = 0.7,
        var maxTokens: Int = 2048,
        var openRouterBaseUrl: String = "https://openrouter.ai/api/v1"
    )

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun getApiKey(): String? {
        return try {
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread<String?> {
                PasswordSafe.instance.getPassword(createCredentialAttributes())
            }.get()
        } catch (e: Exception) {
            null
        }
    }

    fun setApiKey(apiKey: String) {
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            PasswordSafe.instance.setPassword(createCredentialAttributes(), apiKey)
        }
    }

    private fun createCredentialAttributes() = CredentialAttributes(
        generateServiceName("Cline", "apiKey")
    )

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ClineSettings =
            project.getService(ClineSettings::class.java)
    }
}
