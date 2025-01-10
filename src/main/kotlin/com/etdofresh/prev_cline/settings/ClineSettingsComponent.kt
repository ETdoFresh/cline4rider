package com.cline.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class ClineSettingsComponent {
    private val apiKeyField = JBPasswordField()
    private val modelField = JBTextField()
    private val temperatureField = JBTextField()
    private val maxTokensField = JBTextField()
    private val mainPanel: JPanel

    init {
        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("OpenAI API Key: "), apiKeyField, true)
            .addLabeledComponent(JBLabel("Model: "), modelField, true)
            .addLabeledComponent(JBLabel("Temperature: "), temperatureField, true)
            .addLabeledComponent(JBLabel("Max Tokens: "), maxTokensField, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun getPanel(): JComponent = mainPanel

    fun getApiKey(): String = String(apiKeyField.password)
    fun setApiKey(apiKey: String) {
        apiKeyField.text = apiKey
    }

    fun getModel(): String = modelField.text
    fun setModel(model: String) {
        modelField.text = model
    }

    fun getTemperature(): Double = temperatureField.text.toDoubleOrNull() ?: 0.7
    fun setTemperature(temperature: Double) {
        temperatureField.text = temperature.toString()
    }

    fun getMaxTokens(): Int = maxTokensField.text.toIntOrNull() ?: 2048
    fun setMaxTokens(maxTokens: Int) {
        maxTokensField.text = maxTokens.toString()
    }
}
