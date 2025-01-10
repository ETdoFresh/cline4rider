package com.etdofresh.cline4rider.ui

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.ui.model.ChatViewModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.builder.text
import java.awt.BorderLayout
import javax.swing.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClineToolWindow(project: Project, _toolWindow: ToolWindow) {
    private val viewModel = ChatViewModel.getInstance(project)
    private val contentPanel = JPanel(BorderLayout())
    private val chatPanel = JPanel()
    private val inputArea = JTextArea(3, 50)
    private val sendButton = JButton("Send")
    private val clearButton = JButton("Clear")

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        chatPanel.layout = BoxLayout(chatPanel, BoxLayout.Y_AXIS)
        val scrollPane = JBScrollPane(chatPanel)

        val inputPanel = JPanel(BorderLayout())
        inputPanel.add(JBScrollPane(inputArea), BorderLayout.CENTER)

        val buttonPanel = JPanel()
        buttonPanel.add(sendButton)
        buttonPanel.add(clearButton)
        inputPanel.add(buttonPanel, BorderLayout.EAST)

        contentPanel.add(scrollPane, BorderLayout.CENTER)
        contentPanel.add(inputPanel, BorderLayout.SOUTH)

        clearButton.addActionListener {
            viewModel.clearMessages()
            chatPanel.removeAll()
            chatPanel.revalidate()
            chatPanel.repaint()
        }

        sendButton.addActionListener {
            val message = inputArea.text.trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                inputArea.text = ""
            }
        }

        // Load initial messages
        refreshMessages()
    }

    private fun setupListeners() {
        viewModel.addMessageListener { messages ->
            SwingUtilities.invokeLater {
                refreshMessages()
            }
        }

        viewModel.addStateListener { isProcessing ->
            SwingUtilities.invokeLater {
                sendButton.isEnabled = !isProcessing
                inputArea.isEnabled = !isProcessing
            }
        }
    }

    private fun sendMessage(content: String) {
        viewModel.sendMessage(content)
    }

    private fun refreshMessages() {
        chatPanel.removeAll()

        viewModel.getMessages().forEach { message ->
            addMessageToUI(message)
        }

        chatPanel.revalidate()
        chatPanel.repaint()
    }

    private fun addMessageToUI(message: ClineMessage) {
        val messagePanel = panel {
            row {
                val timestamp = Instant.ofEpochMilli(message.timestamp)
                    .atZone(ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                label("[${formatter.format(timestamp)}] ${message.role}:")
            }
            row {
                textArea()
                    .text(message.content)
                    .rows(3)
                    .resizableColumn()
                    .enabled(false)
            }
            row {
                cell(JSeparator())
            }
        }

        chatPanel.add(messagePanel)
    }

    fun getContent() = contentPanel
}
