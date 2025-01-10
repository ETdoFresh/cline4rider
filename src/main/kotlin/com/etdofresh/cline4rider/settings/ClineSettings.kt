package com.etdofresh.cline4rider.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "ClineSettings",
    storages = [Storage("cline.xml")]
)
class ClineSettings(private val project: Project) : PersistentStateComponent<ClineSettings.State> {
    private var myState = State()

    data class State(
        var model: String = "gpt-3.5-turbo",
        var temperature: Double = 0.7,
        var maxTokens: Int = 2048
    )

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun getApiKey(): String? {
        val credentials = PasswordSafe.instance.get(createCredentialAttributes())
        return credentials?.getPasswordAsString()
    }

    fun setApiKey(apiKey: String) {
        PasswordSafe.instance.set(createCredentialAttributes(), Credentials("cline", apiKey))
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
