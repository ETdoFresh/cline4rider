package com.cline.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class ClineSettingsConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: ClineSettingsComponent? = null
    private val settings = ClineSettings.getInstance(project)

    override fun getDisplayName(): String = "Cline"

    override fun createComponent(): JComponent {
        settingsComponent = ClineSettingsComponent()
        return settingsComponent!!.getPanel()
    }

    override fun isModified(): Boolean {
        val component = settingsComponent ?: return false
        val currentSettings = settings.getState()

        return component.getModel() != currentSettings.model ||
                component.getTemperature() != currentSettings.temperature ||
                component.getMaxTokens() != currentSettings.maxTokens ||
                component.getApiKey() != (settings.getApiKey() ?: "")
    }

    override fun apply() {
        val component = settingsComponent ?: return
        val currentSettings = settings.getState()

        currentSettings.model = component.getModel()
        currentSettings.temperature = component.getTemperature()
        currentSettings.maxTokens = component.getMaxTokens()
        
        // Store API key securely
        component.getApiKey().let { apiKey ->
            if (apiKey.isNotEmpty()) {
                settings.setApiKey(apiKey)
            }
        }
    }

    override fun reset() {
        val component = settingsComponent ?: return
        val currentSettings = settings.getState()

        component.setModel(currentSettings.model)
        component.setTemperature(currentSettings.temperature)
        component.setMaxTokens(currentSettings.maxTokens)
        component.setApiKey(settings.getApiKey() ?: "")
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
