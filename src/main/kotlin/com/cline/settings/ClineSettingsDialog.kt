package com.cline.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ClineSettingsDialog(private val project: Project) : DialogWrapper(project) {
    private val settings = ClineSettings.getInstance()
    
    private val apiKeyField = JBPasswordField().apply {
        text = settings.apiKey
    }
    
    private val apiEndpointField = JBTextField(settings.apiEndpoint).apply {
        emptyText.text = "https://api.anthropic.com/v1/messages"
    }
    
    private val modelField = JBTextField(settings.model).apply {
        emptyText.text = "claude-3-sonnet-20240229"
    }
    
    private val maxTokensField = JBTextField(settings.maxTokens.toString()).apply {
        emptyText.text = "4096"
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { validateInput() }
            override fun removeUpdate(e: DocumentEvent?) { validateInput() }
            override fun changedUpdate(e: DocumentEvent?) { validateInput() }
        })
    }
    
    private val temperatureField = JBTextField(settings.temperature.toString()).apply {
        emptyText.text = "0.7"
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { validateInput() }
            override fun removeUpdate(e: DocumentEvent?) { validateInput() }
            override fun changedUpdate(e: DocumentEvent?) { validateInput() }
        })
    }

    init {
        title = "Cline Settings"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("API Key:"), apiKeyField)
            .addLabeledComponent(JBLabel("API Endpoint:"), apiEndpointField)
            .addLabeledComponent(JBLabel("Model:"), modelField)
            .addLabeledComponent(JBLabel("Max Tokens:"), maxTokensField)
            .addLabeledComponent(JBLabel("Temperature:"), temperatureField)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return JBUI.Panels.simplePanel(panel)
            .addToTop(JLabel("Configure your Anthropic API settings here.").apply {
                font = JBUI.Fonts.smallFont()
                foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
                border = JBUI.Borders.empty(4, 0)
            })
            .apply {
                preferredSize = JBUI.size(400, 200)
            }
    }

    override fun doOKAction() {
        if (!validateInput()) {
            com.cline.notifications.ClineNotifier.notifyError(
                project,
                "Please check your settings:\n" +
                "- Max tokens must be a positive number\n" +
                "- Temperature must be between 0.0 and 1.0",
                "Invalid Settings"
            )
            return
        }

        val previousApiKey = settings.apiKey
        settings.apiKey = String(apiKeyField.password)
        settings.apiEndpoint = apiEndpointField.text.trim()
        settings.model = modelField.text.trim()
        settings.maxTokens = maxTokensField.text.trim().toIntOrNull() ?: 4096
        settings.temperature = temperatureField.text.trim().toDoubleOrNull() ?: 0.7

        // Notify if API key was added or changed
        if (previousApiKey.isEmpty() && settings.apiKey.isNotEmpty()) {
            com.cline.notifications.ClineNotifier.notifyInfo(
                project,
                "API key configured successfully. You can now use Cline.",
                "Settings Updated"
            )
        } else if (previousApiKey != settings.apiKey) {
            com.cline.notifications.ClineNotifier.notifyInfo(
                project,
                "API key updated successfully.",
                "Settings Updated"
            )
        }

        super.doOKAction()
    }

    private fun validateInput(): Boolean {
        val maxTokens = maxTokensField.text.trim().toIntOrNull()
        val temperature = temperatureField.text.trim().toDoubleOrNull()

        val isValid = maxTokens != null && maxTokens > 0 &&
                temperature != null && temperature in 0.0..1.0

        isOKActionEnabled = isValid
        return isValid
    }

    override fun getPreferredFocusedComponent(): JComponent = apiKeyField
}
