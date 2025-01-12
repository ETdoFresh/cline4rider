package com.etdofresh.cline4rider.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.jgoodies.forms.layout.FormLayout
import com.jgoodies.forms.layout.CellConstraints
import javax.swing.JComponent
import javax.swing.JPanel

class ClineSettingsConfigurable(private val project: Project) : Configurable {
    private lateinit var apiKeyField: JBPasswordField
    private lateinit var providerField: com.intellij.openapi.ui.ComboBox<ClineSettings.Provider>
    private lateinit var modelField: JBTextField
    private lateinit var temperatureField: JBTextField
    private lateinit var maxTokensField: JBTextField
    private lateinit var openRouterBaseUrlField: JBTextField
    private lateinit var openRouterBaseUrlLabel: JBLabel

    override fun createComponent(): JComponent {
        val panel = JPanel()
        panel.layout = FormLayout(
            "right:pref, 4dlu, fill:pref:grow",
            "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref"
        )
        
        val cc = CellConstraints()
        
        // Provider
        panel.add(JBLabel("Provider:"), cc.xy(1, 1))
        providerField = com.intellij.openapi.ui.ComboBox(ClineSettings.Provider.values()).apply {
            selectedItem = ClineSettings.Provider.OPENAI
        }
        panel.add(providerField, cc.xy(3, 1))
        
        // API Key
        panel.add(JBLabel("API Key:"), cc.xy(1, 3))
        apiKeyField = JBPasswordField().apply {
            emptyText.text = "Enter your API key"
        }
        panel.add(apiKeyField, cc.xy(3, 3))
        
        // Model
        panel.add(JBLabel("Model:"), cc.xy(1, 5))
        modelField = JBTextField().apply {
            emptyText.text = "gpt-3.5-turbo"
        }
        panel.add(modelField, cc.xy(3, 5))
        
        // Temperature
        panel.add(JBLabel("Temperature:"), cc.xy(1, 7))
        temperatureField = JBTextField().apply {
            emptyText.text = "0.7"
        }
        panel.add(temperatureField, cc.xy(3, 7))
        
        // Max Tokens
        panel.add(JBLabel("Max Tokens:"), cc.xy(1, 9))
        maxTokensField = JBTextField().apply {
            emptyText.text = "2048"
        }
        panel.add(maxTokensField, cc.xy(3, 9))

        // OpenRouter Base URL
        openRouterBaseUrlLabel = JBLabel("OpenRouter Base URL:").apply {
            isVisible = false
        }
        panel.add(openRouterBaseUrlLabel, cc.xy(1, 11))
        openRouterBaseUrlField = JBTextField().apply {
            emptyText.text = "https://openrouter.ai/api/v1"
            isVisible = false
        }
        panel.add(openRouterBaseUrlField, cc.xy(3, 11))

        // Add provider change listener
        providerField.addActionListener {
            val isOpenRouter = providerField.selectedItem == ClineSettings.Provider.OPENROUTER
            openRouterBaseUrlLabel.isVisible = isOpenRouter
            openRouterBaseUrlField.isVisible = isOpenRouter
        }
        
        return panel
    }

    override fun isModified(): Boolean {
        val settings = ClineSettings.getInstance(project)
        val currentApiKey = settings.getApiKey() ?: ""
        val newApiKey = String(apiKeyField.password)
        
        return try {
            newApiKey != currentApiKey ||
                    providerField.selectedItem != settings.state.provider ||
                    modelField.text != settings.state.model ||
                    temperatureField.text != settings.state.temperature.toString() ||
                    maxTokensField.text != settings.state.maxTokens.toString() ||
                    (providerField.selectedItem == ClineSettings.Provider.OPENROUTER &&
                            openRouterBaseUrlField.text != settings.state.openRouterBaseUrl)
        } catch (e: Exception) {
            // If there's an error accessing the API key, consider the form modified
            true
        }
    }

    override fun apply() {
        val settings = ClineSettings.getInstance(project)
        
        // Save API key in background
        val apiKey = String(apiKeyField.password)
        if (apiKey.isNotEmpty()) {
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
                settings.setApiKey(apiKey)
            }
        }
        
        // Update other settings
        settings.state.provider = providerField.selectedItem as ClineSettings.Provider
        settings.state.model = modelField.text
        settings.state.temperature = temperatureField.text.toDoubleOrNull() ?: 0.7
        settings.state.maxTokens = maxTokensField.text.toIntOrNull() ?: 2048
        if (settings.state.provider == ClineSettings.Provider.OPENROUTER) {
            settings.state.openRouterBaseUrl = openRouterBaseUrlField.text.takeIf { it.isNotEmpty() }
                ?: "https://openrouter.ai/api/v1"
        }
    }

    override fun reset() {
        val settings = ClineSettings.getInstance(project)
        
        // Reset API key in background
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            val apiKey = settings.getApiKey()
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                apiKeyField.text = apiKey ?: ""
            }
        }
        
        // Reset other fields
        providerField.selectedItem = settings.state.provider
        modelField.text = settings.state.model
        temperatureField.text = settings.state.temperature.toString()
        maxTokensField.text = settings.state.maxTokens.toString()
        openRouterBaseUrlField.text = settings.state.openRouterBaseUrl
        
        // Update visibility
        val isOpenRouter = settings.state.provider == ClineSettings.Provider.OPENROUTER
        openRouterBaseUrlLabel.isVisible = isOpenRouter
        openRouterBaseUrlField.isVisible = isOpenRouter
    }

    override fun getDisplayName() = "Cline4Rider"
}
