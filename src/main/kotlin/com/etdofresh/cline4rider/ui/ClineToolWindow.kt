package com.etdofresh.cline4rider.ui

import com.etdofresh.cline4rider.model.ClineMessage
import com.etdofresh.cline4rider.ui.model.ChatViewModel
import com.intellij.icons.AllIcons
import com.intellij.icons.AllIcons.Actions
import com.intellij.icons.AllIcons.Nodes
import com.intellij.icons.AllIcons.Vcs
import com.intellij.icons.AllIcons.General
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClineToolWindow(project: Project, private val toolWindow: ToolWindow) {
    private val viewModel = ChatViewModel.getInstance(project)
    private val tabbedPane = JBTabbedPane()
    private val contentPanel = JPanel(BorderLayout()).apply {
        background = Color(45, 45, 45)
        preferredSize = Dimension(JBUI.scale(400), JBUI.scale(150))
        minimumSize = Dimension(JBUI.scale(300), JBUI.scale(100))
    }
    private val chatPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(45, 45, 45)
        maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
    }
    private val inputArea = JTextArea(2, 50).apply {
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        background = Color(51, 51, 51)
        foreground = Color(220, 220, 220)
        caretColor = Color(220, 220, 220)
        
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "send")
        inputMap.put(KeyStroke.getKeyStroke("shift ENTER"), "newline")
        actionMap.put("send", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                val message = text.trim()
                if (message.isNotEmpty() && !viewModel.isProcessing()) {
                    sendMessage(message)
                    text = ""
                }
            }
        })
        actionMap.put("newline", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                insert("\n", caretPosition)
            }
        })
    }
    private val sendButton = JButton("Send").apply {
        background = Color(60, 60, 60)
        foreground = Color(220, 220, 220)
    }
    private val clearButton = JButton("Clear").apply {
        background = Color(60, 60, 60)
        foreground = Color(220, 220, 220)
    }

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Setup main chat panel
        val mainChatPanel = createMainChatPanel()
        
        // Setup tabs with icons
        tabbedPane.apply {
            addTab("Home", AllIcons.Nodes.HomeFolder, mainChatPanel)
            addTab("Current Task", AllIcons.Nodes.Folder, createTasksPanel())
            addTab("History", AllIcons.Vcs.History, createHistoryPanel())
            addTab("Settings", AllIcons.General.Settings, createSettingsPanel())
            
            val tabsPanel = JPanel(BorderLayout()).apply {
                add(tabbedPane, BorderLayout.CENTER)
            }
            
            contentPanel.add(tabsPanel, BorderLayout.CENTER)
        }
    }

    private fun createMainChatPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // Enable smooth scrolling
        UIManager.put("ScrollBar.smoothScrolling", true)
        val scrollPane = JBScrollPane(chatPanel).apply {
            border = BorderFactory.createEmptyBorder()
            viewport.background = Color(45, 45, 45)
            verticalScrollBar.unitIncrement = 16
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            preferredSize = Dimension(Int.MAX_VALUE, JBUI.scale(200))
        }

        val inputPanel = createInputPanel()
        
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(inputPanel, BorderLayout.SOUTH)
        
        return panel
    }

    private fun createInputPanel(): JPanel {
        val inputPanel = JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
        
        val inputScrollPane = JBScrollPane(inputArea).apply {
            border = BorderFactory.createLineBorder(Color(60, 60, 60))
            preferredSize = Dimension(0, JBUI.scale(40))
            minimumSize = Dimension(0, JBUI.scale(40))
        }
        
        inputPanel.add(inputScrollPane, BorderLayout.CENTER)

        val buttonPanel = JPanel().apply {
            background = Color(45, 45, 45)
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
        }
        
        buttonPanel.add(Box.createHorizontalGlue())
        buttonPanel.add(clearButton)
        buttonPanel.add(Box.createHorizontalStrut(5))
        buttonPanel.add(sendButton)
        
        inputPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        return inputPanel
    }

    private fun createHistoryPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
            add(JLabel("History View - Coming Soon", SwingConstants.CENTER))
        }
    }

    private fun createTasksPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
            add(JLabel("Tasks View - Coming Soon", SwingConstants.CENTER))
        }
    }

    private fun createSettingsPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
            add(JLabel("Settings View - Coming Soon", SwingConstants.CENTER))
        }
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
        
        // Auto-scroll to bottom
        SwingUtilities.invokeLater {
            val vertical = chatPanel.parent.parent as JScrollPane
            vertical.verticalScrollBar.value = vertical.verticalScrollBar.maximum
        }
    }

    private fun addMessageToUI(message: ClineMessage) {
        val timestamp = Instant.ofEpochMilli(message.timestamp)
            .atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        
        val messagePanel = JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        val headerPanel = JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
            border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        }

        val timestampLabel = JLabel("[${formatter.format(timestamp)}]").apply {
            foreground = Color(150, 150, 150)
            font = font.deriveFont(font.size2D - 1f)
        }
        
        val roleLabel = JLabel(" ${message.role}:").apply {
            foreground = when (message.role) {
                ClineMessage.Role.USER -> Color(100, 150, 255)
                ClineMessage.Role.ASSISTANT -> Color(100, 255, 150)
                ClineMessage.Role.TOOL -> Color(255, 200, 100)
                else -> Color(220, 220, 220)
            }
        }

        headerPanel.add(timestampLabel, BorderLayout.WEST)
        headerPanel.add(roleLabel, BorderLayout.CENTER)

        val contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(51, 51, 51)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        if (message.content.isNotEmpty()) {
            val contentArea = JTextArea(message.content).apply {
                background = Color(51, 51, 51)
                foreground = Color(220, 220, 220)
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            }
            contentPanel.add(contentArea)
        }

        message.toolCalls.forEach { toolCall ->
            val toolPanel = JPanel(BorderLayout()).apply {
                background = Color(60, 60, 60)
                border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            }

            val toolHeader = JPanel(BorderLayout()).apply {
                background = Color(60, 60, 60)
                border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
            }

            val toolNameLabel = JLabel("${toolCall.name}").apply {
                foreground = Color(200, 200, 255)
                font = font.deriveFont(font.size2D - 1f)
            }

            toolHeader.add(toolNameLabel, BorderLayout.WEST)
            toolPanel.add(toolHeader, BorderLayout.NORTH)

            val toolContent = JTextArea("${toolCall.arguments}\n\n${toolCall.output ?: ""}").apply {
                background = Color(70, 70, 70)
                foreground = Color(220, 220, 220)
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            }

            toolPanel.add(toolContent, BorderLayout.CENTER)
            contentPanel.add(toolPanel)
            contentPanel.add(Box.createVerticalStrut(5))
        }

        messagePanel.add(headerPanel, BorderLayout.NORTH)
        messagePanel.add(contentPanel, BorderLayout.CENTER)
        
        chatPanel.add(messagePanel)
        chatPanel.add(Box.createVerticalStrut(1))
    }

    fun getContent() = contentPanel
}
