package com.cline.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "ClineSettings",
    storages = [Storage("cline.xml")]
)
@Service
class ClineSettings : PersistentStateComponent<ClineSettings> {
    var apiKey: String = ""
    var apiEndpoint: String = "https://api.anthropic.com/v1/messages"
    var model: String = "claude-3-sonnet-20240229"
    var maxTokens: Int = 4096
    var temperature: Double = 0.7
    var systemPromptPath: String = ".clinesystemprompt"
    var rulesPath: String = ".clinerules"

    override fun getState(): ClineSettings = this

    override fun loadState(state: ClineSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): ClineSettings =
            service<ClineSettings>()
    }
}
