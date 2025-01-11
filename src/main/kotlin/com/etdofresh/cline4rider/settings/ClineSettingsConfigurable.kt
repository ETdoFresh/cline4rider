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

    override fun createComponent(): JComponent {
        val panel = JPanel()
        panel.layout = FormLayout(
            "right:pref, 4dlu, fill:pref:grow",
            "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref"
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
        
        return panel
    }

    override fun isModified(): Boolean {
        val settings = ClineSettings.getInstance(project)
        
        // Check if we're on the EDT and warn if we are
        if (com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread) {
            com.intellij.openapi.diagnostic.Logger.getInstance(ClineSettingsConfigurable::class.java)
                .warn("isModified called on EDT")
        }
        
        return apiKeyField.password.joinToString("") != settings.getApiKey() ||
                providerField.selectedItem != settings.state.provider ||
                modelField.text != settings.state.model ||
                temperatureField.text != settings.state.temperature.toString() ||
                maxTokensField.text != settings.state.maxTokens.toString()
    }

    override fun apply() {
        val settings = ClineSettings.getInstance(project)
        val apiKey = apiKeyField.password.joinToString("")
        
        // Update non-sensitive settings on EDT
        settings.state.provider = providerField.selectedItem as ClineSettings.Provider
        settings.state.model = modelField.text
        settings.state.temperature = temperatureField.text.toDoubleOrNull() ?: 0.7
        settings.state.maxTokens = maxTokensField.text.toIntOrNull() ?: 2048
        
        // Run API key setting in background
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            settings.setApiKey(apiKey)
        }
    }

    override fun reset() {
        val settings = ClineSettings.getInstance(project)
        
        // Initialize UI components first
        providerField.selectedItem = settings.state.provider
        modelField.text = settings.state.model
        temperatureField.text = settings.state.temperature.toString()
        maxTokensField.text = settings.state.maxTokens.toString()
        
        // Run API key retrieval in background
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            val apiKey = settings.getApiKey()
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                apiKeyField.text = apiKey ?: ""
            }
        }
    }

    override fun getDisplayName() = "Cline4Rider"
}
