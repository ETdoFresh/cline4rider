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
    private lateinit var modelField: JBTextField
    private lateinit var temperatureField: JBTextField
    private lateinit var maxTokensField: JBTextField

    override fun createComponent(): JComponent {
        val panel = JPanel()
        panel.layout = FormLayout(
            "right:pref, 4dlu, fill:pref:grow",
            "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref"
        )
        
        val cc = CellConstraints()
        
        // API Key
        panel.add(JBLabel("API Key:"), cc.xy(1, 1))
        apiKeyField = JBPasswordField().apply {
            emptyText.text = "Enter your API key"
        }
        panel.add(apiKeyField, cc.xy(3, 1))
        
        // Model
        panel.add(JBLabel("Model:"), cc.xy(1, 3))
        modelField = JBTextField().apply {
            emptyText.text = "gpt-3.5-turbo"
        }
        panel.add(modelField, cc.xy(3, 3))
        
        // Temperature
        panel.add(JBLabel("Temperature:"), cc.xy(1, 5))
        temperatureField = JBTextField().apply {
            emptyText.text = "0.7"
        }
        panel.add(temperatureField, cc.xy(3, 5))
        
        // Max Tokens
        panel.add(JBLabel("Max Tokens:"), cc.xy(1, 7))
        maxTokensField = JBTextField().apply {
            emptyText.text = "2048"
        }
        panel.add(maxTokensField, cc.xy(3, 7))
        
        return panel
    }

    override fun isModified(): Boolean {
        val settings = ClineSettings.getInstance(project)
        return apiKeyField.password.joinToString("") != settings.getApiKey() ||
                modelField.text != settings.state.model ||
                temperatureField.text != settings.state.temperature.toString() ||
                maxTokensField.text != settings.state.maxTokens.toString()
    }

    override fun apply() {
        val settings = ClineSettings.getInstance(project)
        val apiKey = apiKeyField.password.joinToString("")
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
        
        // Run API key retrieval in background
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            val apiKey = settings.getApiKey()
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                apiKeyField.text = apiKey ?: ""
                modelField.text = settings.state.model
                temperatureField.text = settings.state.temperature.toString()
                maxTokensField.text = settings.state.maxTokens.toString()
            }
        }
    }

    override fun getDisplayName() = "Cline4Rider"
}
