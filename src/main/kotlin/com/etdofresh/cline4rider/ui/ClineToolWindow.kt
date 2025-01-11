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
import java.awt.Component
import com.etdofresh.cline4rider.persistence.ChatHistory

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
            addTab("Home", AllIcons.Nodes.HomeFolder, createHomePanel())
            addTab("Chat", AllIcons.Nodes.Folder, mainChatPanel)
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
        val panel = JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }
        
        val scrollPane = JBScrollPane().apply {
            border = BorderFactory.createEmptyBorder()
            viewport.background = Color(45, 45, 45)
            verticalScrollBar.unitIncrement = 16
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }
        
        val historyContent = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(45, 45, 45)
        }
        
        val loadMoreButton = JButton("Load More...").apply {
            background = Color(60, 60, 60)
            foreground = Color(220, 220, 220)
            isVisible = false
            alignmentX = Component.CENTER_ALIGNMENT
        }
        
        var offset = 0
        
        fun refreshHistory() {
            val conversations = viewModel.getRecentConversations(offset)
            if (conversations.isNotEmpty()) {
            conversations.forEach { conversation: ChatHistory.Conversation ->
                    val conversationPanel = createConversationPanel(conversation)
                    historyContent.add(conversationPanel)
                    historyContent.add(Box.createVerticalStrut(5))
                }
                offset += conversations.size
                loadMoreButton.isVisible = viewModel.hasMoreConversations(offset)
            }
            historyContent.revalidate()
            historyContent.repaint()
        }
        
        loadMoreButton.addActionListener {
            refreshHistory()
        }
        
        // Initial load
        refreshHistory()
        
        scrollPane.viewport.view = historyContent
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(loadMoreButton, BorderLayout.SOUTH)
        
        return panel
    }
    
    private fun createConversationPanel(conversation: ChatHistory.Conversation): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            background = Color(51, 51, 51)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
        
        val timestamp = Instant.ofEpochMilli(conversation.timestamp)
            .atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")
        
        val headerPanel = JPanel(BorderLayout()).apply {
            background = Color(51, 51, 51)
            border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        }
        
        val timestampLabel = JLabel(formatter.format(timestamp)).apply {
            foreground = Color(150, 150, 150)
            font = font.deriveFont(font.size2D - 1f)
        }
        
        headerPanel.add(timestampLabel, BorderLayout.WEST)
        
        val contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(51, 51, 51)
        }
        
        conversation.messages.take(3).forEach { message ->
            val messagePanel = JPanel(BorderLayout()).apply {
                background = Color(60, 60, 60)
                border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            }
            
            val content = JTextArea().apply {
                text = message.content
                background = Color(60, 60, 60)
                foreground = Color(220, 220, 220)
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            }
            
            messagePanel.add(content, BorderLayout.CENTER)
            contentPanel.add(messagePanel)
            contentPanel.add(Box.createVerticalStrut(5))
        }
        
        if (conversation.messages.size > 3) {
            val moreLabel = JLabel("...${conversation.messages.size - 3} more messages").apply {
                foreground = Color(150, 150, 150)
                font = font.deriveFont(font.size2D - 1f)
                alignmentX = Component.RIGHT_ALIGNMENT
            }
            contentPanel.add(moreLabel)
        }
        
        panel.add(headerPanel, BorderLayout.NORTH)
        panel.add(contentPanel, BorderLayout.CENTER)
        
        return panel
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

    private fun createHomePanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            
            // Add welcome message
            val welcomePanel = JPanel(BorderLayout()).apply {
                background = Color(45, 45, 45)
                border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
                add(JLabel("Welcome to Cline for Rider!", SwingConstants.CENTER).apply {
                    foreground = Color(220, 220, 220)
                    font = font.deriveFont(font.size2D + 2f)
                })
            }
            
            // Add recent history section
            val historyPanel = JPanel(BorderLayout()).apply {
                background = Color(45, 45, 45)
                border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                
                // Add section header
                add(JLabel("Recent History", SwingConstants.LEFT).apply {
                    foreground = Color(200, 200, 200)
                    font = font.deriveFont(font.size2D + 1f)
                    border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
                })
                
                // Get last 3 messages
                val messages = viewModel.getMessages().takeLast(3)
                if (messages.isEmpty()) {
                    add(JLabel("No recent history", SwingConstants.LEFT).apply {
                        foreground = Color(150, 150, 150)
                        font = font.deriveFont(font.size2D - 1f)
                    })
                } else {
                    messages.forEach { message ->
                        val messagePanel = JPanel(BorderLayout()).apply {
                            background = Color(51, 51, 51)
                            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                        }
                        
                        val content = JTextArea(message.content).apply {
                            background = Color(51, 51, 51)
                            foreground = Color(220, 220, 220)
                            lineWrap = true
                            wrapStyleWord = true
                            isEditable = false
                            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                        }
                        
                        messagePanel.add(content, BorderLayout.CENTER)
                        add(messagePanel)
                        add(Box.createVerticalStrut(5))
                    }
                }
            }
            
            add(welcomePanel)
            add(historyPanel)
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
        if (viewModel.isProcessing()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Please wait for the current message to finish processing",
                "Processing",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }
        
        // Check if API key is configured
        if (viewModel.getApiKey().isNullOrBlank()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Please configure your API key in Settings | Tools | Cline",
                "API Key Required",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        
        try {
            viewModel.sendMessage(content)
            // Switch to chat tab to show the conversation
            tabbedPane.selectedIndex = 1
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Failed to send message: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
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
