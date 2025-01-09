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
    var apiEndpoint: String = "https://openrouter.ai/api/v1/chat/completions"
    var model: String = "openai/gpt-4o-mini"
    var maxTokens: Int = 128000
    var temperature: Double = 0.0
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
