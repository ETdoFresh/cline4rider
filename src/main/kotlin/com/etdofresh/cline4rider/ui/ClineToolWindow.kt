package com.etdofresh.cline4rider.ui

import com.etdofresh.cline4rider.model.ClineMessage
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.awt.Component
import java.awt.Dialog
import java.awt.Frame
import java.awt.Window
import com.etdofresh.cline4rider.persistence.ChatHistory
import java.io.File

class ClineToolWindow(private val project: Project, private val toolWindow: ToolWindow) {
    companion object {
        private val DEFAULT_SYSTEM_PROMPT = """You are Cline, an AI coding assistant. Your role is to help developers write, modify, and understand code. You should:
            |
            |1. Provide clear, concise explanations
            |2. Write efficient, well-documented code
            |3. Follow best practices and design patterns
            |4. Consider performance, maintainability, and readability
            |5. Explain your reasoning when making significant decisions
            |
            |When writing code:
            |- Use appropriate language idioms and conventions
            |- Include necessary error handling
            |- Follow the project's existing style
            |- Keep code modular and testable
            |
            |Remember to:
            |- Ask for clarification when requirements are unclear
            |- Suggest improvements when appropriate
            |- Explain complex concepts in simple terms
            |- Consider security implications
            |
            |Your responses should be professional, accurate, and focused on solving the developer's problems efficiently.""".trimMargin()
    }
    
    private val viewModel = ChatViewModel.getInstance(project)
    private val tabbedPane = JBTabbedPane()
    private var lastSelectedTab = 0
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
    private val inputArea = JTextArea().apply {
        rows = 3
        columns = 50
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        background = Color(51, 51, 51)
        foreground = Color(220, 220, 220)
        caretColor = Color(220, 220, 220)
        isEditable = true
        isEnabled = true
        isFocusable = true
        
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
        
        // Create and store home panel reference
        homePanel = createHomePanel()
        
        // Setup tabs with icons
        tabbedPane.apply {
            addTab("Home", AllIcons.Nodes.HomeFolder, homePanel)
            addTab("Chat", AllIcons.Nodes.Folder, mainChatPanel)
            addTab("History", AllIcons.Vcs.History, createHistoryPanel())
            addTab("Settings", AllIcons.General.Settings, JPanel())
            
            addChangeListener { e ->
                val tabbedPane = e.source as JBTabbedPane
                val newIndex = tabbedPane.selectedIndex
                if (newIndex == 3) { // Settings tab
                    SwingUtilities.invokeLater {
                        tabbedPane.selectedIndex = lastSelectedTab // Return to previous tab
                        com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(
                            project,
                            "Cline4Rider"
                        )
                    }
                } else {
                    lastSelectedTab = newIndex
                }
            }
            
            val tabsPanel = JPanel(BorderLayout()).apply {
                add(tabbedPane, BorderLayout.CENTER)
            }
            
            contentPanel.add(tabsPanel, BorderLayout.CENTER)
        }
    }

    private fun createMainChatPanel(): JPanel {
        // Create main panel with BorderLayout
        val mainPanel = JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
        }
        
        // Create chat area panel that will contain messages
        val chatAreaPanel = JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
        }
        
        // Enable smooth scrolling
        UIManager.put("ScrollBar.smoothScrolling", true)
        val scrollPane = JBScrollPane(chatPanel).apply {
            border = BorderFactory.createEmptyBorder()
            viewport.background = Color(45, 45, 45)
            verticalScrollBar.unitIncrement = 16
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }
        
        // Add scroll pane to chat area
        chatAreaPanel.add(scrollPane, BorderLayout.CENTER)
        
        // Create input panel with fixed height
        val inputPanel = createInputPanel()
        
        // Add components to main panel
        mainPanel.add(chatAreaPanel, BorderLayout.CENTER)
        mainPanel.add(inputPanel, BorderLayout.SOUTH)
        
        return mainPanel
    }

    private fun createInputPanel(): JPanel {
        // Create a new text area for this specific panel
        val localInputArea = JTextArea().apply {
            rows = 3
            columns = 50
            background = Color(51, 51, 51)
            foreground = Color(220, 220, 220)
            caretColor = Color(220, 220, 220)
            isEditable = true
            isEnabled = true
            isFocusable = true
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        // Create the scroll pane with the new text area
        val inputScrollPane = JBScrollPane(localInputArea).apply {
            border = BorderFactory.createLineBorder(Color(60, 60, 60))
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        // Create button panel
        val buttonPanel = JPanel().apply {
            background = Color(45, 45, 45)
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
        }

        // Create local buttons
        val localSendButton = JButton("Send").apply {
            background = Color(60, 60, 60)
            foreground = Color(220, 220, 220)
            isEnabled = true
        }

        val localClearButton = JButton("Clear").apply {
            background = Color(60, 60, 60)
            foreground = Color(220, 220, 220)
            isEnabled = true
        }

        // Add buttons to panel
        buttonPanel.add(Box.createHorizontalGlue())
        buttonPanel.add(localClearButton)
        buttonPanel.add(Box.createHorizontalStrut(5))
        buttonPanel.add(localSendButton)

        // Create main panel
        val inputPanel = JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        // Add components to main panel
        inputPanel.add(inputScrollPane, BorderLayout.CENTER)
        inputPanel.add(buttonPanel, BorderLayout.SOUTH)

        // Set up input area key bindings
        localInputArea.inputMap.put(KeyStroke.getKeyStroke("ENTER"), "send")
        localInputArea.inputMap.put(KeyStroke.getKeyStroke("shift ENTER"), "newline")
        localInputArea.actionMap.put("send", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                val message = localInputArea.text.trim()
                if (message.isNotEmpty() && !viewModel.isProcessing()) {
                    sendMessage(message)
                    localInputArea.text = ""
                }
            }
        })
        localInputArea.actionMap.put("newline", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                localInputArea.insert("\n", localInputArea.caretPosition)
            }
        })

        // Add button listeners
        localSendButton.addActionListener {
            val message = localInputArea.text.trim()
            if (message.isNotEmpty() && !viewModel.isProcessing()) {
                sendMessage(message)
                localInputArea.text = ""
            }
        }

        localClearButton.addActionListener {
            localInputArea.text = ""
        }
        
        val inputArea = inputScrollPane.viewport.view as? JTextArea
        val sendButton = buttonPanel.components.find { it is JButton && it.text == "Send" } as? JButton
        
        inputArea?.inputMap?.put(KeyStroke.getKeyStroke("ENTER"), "send")
        inputArea?.inputMap?.put(KeyStroke.getKeyStroke("shift ENTER"), "newline")
        inputArea?.actionMap?.put("send", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                val message = inputArea?.text?.trim()
                if (message != null && message.isNotEmpty() && !viewModel.isProcessing()) {
                    sendMessage(message)
                    inputArea.text = ""
                }
            }
        })
        inputArea?.actionMap?.put("newline", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                inputArea?.insert("\n", inputArea.caretPosition)
            }
        })
        
        sendButton?.addActionListener {
            val message = inputArea?.text?.trim()
            if (message != null && message.isNotEmpty() && !viewModel.isProcessing()) {
                sendMessage(message)
                inputArea.text = ""
            }
        }
        
        return inputPanel
    }

    private var historyOffset = 0
    private lateinit var historyContent: JPanel
    
    private fun refreshHistory() {
        historyOffset = 0  // Reset offset when refreshing
        historyContent.removeAll()  // Clear existing content
        
        val conversations = viewModel.getRecentConversations(historyOffset)
        conversations.forEach { conversation: ChatHistory.Conversation ->
            // Only add conversations that have at least one message
            if (conversation.messages.isNotEmpty()) {
                val conversationPanel = createConversationPanel(conversation)
                historyContent.add(conversationPanel)
                historyContent.add(Box.createVerticalStrut(5))
            }
        }
        
        if (historyContent.components.isEmpty()) {
            historyContent.add(JLabel("No conversation history", SwingConstants.CENTER).apply {
                foreground = Color(150, 150, 150)
                font = font.deriveFont(font.size2D - 1f)
            })
        }
        
        historyOffset = conversations.size  // Update offset with new size
        historyContent.revalidate()
        historyContent.repaint()
    }

    private fun createHistoryPanel(): JPanel {
        historyContent = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(45, 45, 45)
        }
        
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
        
        val loadMoreButton = JButton("Load More...").apply {
            background = Color(60, 60, 60)
            foreground = Color(220, 220, 220)
            isVisible = false
            alignmentX = Component.CENTER_ALIGNMENT
        }
        
        loadMoreButton.addActionListener {
            refreshHistory()
            loadMoreButton.isVisible = viewModel.hasMoreConversations(historyOffset)
        }
        
        scrollPane.viewport.view = historyContent
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(loadMoreButton, BorderLayout.SOUTH)
        
        // Initial load after components are initialized
        refreshHistory()
        loadMoreButton.isVisible = viewModel.hasMoreConversations(historyOffset)
        
        return panel
    }
    
    private fun createConversationPanel(conversation: ChatHistory.Conversation): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            background = Color(51, 51, 51)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }
        
        val timestamp = Instant.ofEpochMilli(conversation.timestamp)
            .atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")
        
        // Find first USER message
        val firstUserMessage = conversation.messages.firstOrNull { it.role == "USER" }
        
        // Main content panel
        val contentPanel = JPanel(BorderLayout()).apply {
            background = Color(51, 51, 51)
            border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
        }
        
        // Left side with message content
        val messagePanel = JPanel(BorderLayout()).apply {
            background = Color(51, 51, 51)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    viewModel.loadConversation(conversation.id)
                    tabbedPane.selectedIndex = 1 // Switch to chat tab
                }
                override fun mouseEntered(e: MouseEvent) {
                    background = Color(60, 60, 60)
                }
                override fun mouseExited(e: MouseEvent) {
                    background = Color(51, 51, 51)
                }
            })
        }
        
        // Header with timestamp
        val headerPanel = JPanel(BorderLayout()).apply {
            background = Color(51, 51, 51)
            border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        }
        
        val timestampLabel = JLabel(formatter.format(timestamp)).apply {
            foreground = Color(150, 150, 150)
            font = font.deriveFont(font.size2D - 1f)
        }
        
        headerPanel.add(timestampLabel, BorderLayout.WEST)
        
        // Message content
        if (firstUserMessage != null) {
            val content = JTextArea(firstUserMessage.content).apply {
                background = Color(60, 60, 60)
                foreground = Color(220, 220, 220)
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            }
            
            messagePanel.add(headerPanel, BorderLayout.NORTH)
            messagePanel.add(content, BorderLayout.CENTER)
        }
        
        // Delete button
        val deleteButton = JButton(AllIcons.Actions.Cancel).apply {
            background = Color(51, 51, 51)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            toolTipText = "Delete conversation"
            addActionListener {
                val confirm = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to delete this conversation?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
                )
                if (confirm == JOptionPane.YES_OPTION) {
                    viewModel.deleteConversationById(conversation.id)
                    refreshHistory()
                }
            }
        }
        
        
        contentPanel.add(messagePanel, BorderLayout.CENTER)
        contentPanel.add(deleteButton, BorderLayout.EAST)
        
        panel.add(contentPanel, BorderLayout.CENTER)
        
        return panel
    }

    private fun createTasksPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
            add(JLabel("Tasks View - Coming Soon", SwingConstants.CENTER))
        }
    }

    private fun createHomePanel(): JPanel {
        val panel = JPanel().apply {
            background = Color(45, 45, 45)
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        }

        // Create config buttons panel
        val configButtonsPanel = JPanel().apply {
            background = Color(45, 45, 45)
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
            
            add(Box.createHorizontalGlue())
            add(JButton(".clinesystemprompt").apply {
                background = Color(51, 102, 153)
                foreground = Color.WHITE
                addActionListener {
                    openConfigEditor(".clinesystemprompt", viewModel.getSystemPrompt() ?: "")
                }
            })
            add(Box.createHorizontalStrut(10))
            add(JButton(".clinerules").apply {
                background = Color(51, 102, 153)
                foreground = Color.WHITE
                addActionListener {
                    openConfigEditor(".clinerules", readFileContent(".clinerules"))
                }
            })
            add(Box.createHorizontalGlue())
        }

        // Add config buttons first
        panel.add(configButtonsPanel)
            
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
        val historyPanel = JPanel().apply {
            background = Color(45, 45, 45)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            
            // Add section header
            add(JLabel("Recent History", SwingConstants.LEFT).apply {
                foreground = Color(200, 200, 200)
                font = font.deriveFont(font.size2D + 1f)
                border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
            })
            
            // Get conversations with messages until we have 3 valid ones
            val validConversations = viewModel.getRecentConversations()
                .filter { it.messages.any { msg -> msg.role == "USER" } }
                .take(3)

            if (validConversations.isEmpty()) {
                add(JLabel("No recent history", SwingConstants.LEFT).apply {
                    foreground = Color(150, 150, 150)
                    font = font.deriveFont(font.size2D - 1f)
                })
            } else {
                validConversations.forEach { conversation ->
                    val firstUserMessage = conversation.messages.first { it.role == "USER" }
                    val messagePanel = JPanel(BorderLayout()).apply {
                        background = Color(51, 51, 51)
                        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        addMouseListener(object : MouseAdapter() {
                            override fun mouseClicked(e: MouseEvent) {
                                viewModel.loadConversation(conversation.id)
                                tabbedPane.selectedIndex = 1 // Switch to chat tab
                            }
                            override fun mouseEntered(e: MouseEvent) {
                                background = Color(60, 60, 60)
                            }
                            override fun mouseExited(e: MouseEvent) {
                                background = Color(51, 51, 51)
                            }
                        })
                    }
                    
                    val content = JTextArea(firstUserMessage.content).apply {
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
        
        panel.add(welcomePanel)
        panel.add(historyPanel)
        
        // Add input panel for new task
        val homeInputPanel = createInputPanel().apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
        
        val homeInputArea = homeInputPanel.components.find { it is JBScrollPane }?.let { it as JBScrollPane }?.viewport?.view as? JTextArea
        val homeSendButton = homeInputPanel.components.find { it is JPanel }?.let { it as JPanel }?.components?.find { it is JButton && it.text == "Send" } as? JButton
        
        homeInputArea?.inputMap?.put(KeyStroke.getKeyStroke("ENTER"), "send")
        homeInputArea?.inputMap?.put(KeyStroke.getKeyStroke("shift ENTER"), "newline")
        homeInputArea?.actionMap?.put("send", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                val message = homeInputArea?.text?.trim()
                if (message != null && message.isNotEmpty() && !viewModel.isProcessing()) {
                    viewModel.createNewTask()
                    sendMessage(message)
                    homeInputArea.text = ""
                }
            }
        })
        homeInputArea?.actionMap?.put("newline", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                homeInputArea?.insert("\n", homeInputArea.caretPosition)
            }
        })
        
        homeSendButton?.addActionListener {
            val message = homeInputArea?.text?.trim()
            if (message != null && message.isNotEmpty() && !viewModel.isProcessing()) {
                viewModel.createNewTask()
                sendMessage(message)
                homeInputArea.text = ""
            }
        }
        
        panel.add(homeInputPanel)
        return panel
    }

    private lateinit var homePanel: JPanel
    
    private fun refreshHomePanel() {
        val panel = JPanel().apply {
            background = Color(45, 45, 45)
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        }
        
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
        val historyPanel = JPanel().apply {
            background = Color(45, 45, 45)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            
            // Add section header
            add(JLabel("Recent History", SwingConstants.LEFT).apply {
                foreground = Color(200, 200, 200)
                font = font.deriveFont(font.size2D + 1f)
                border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
            })
            
            // Get conversations with messages until we have 3 valid ones
            val validConversations = viewModel.getRecentConversations()
                .filter { it.messages.any { msg -> msg.role == "USER" } }
                .take(3)

            if (validConversations.isEmpty()) {
                add(JLabel("No recent history", SwingConstants.LEFT).apply {
                    foreground = Color(150, 150, 150)
                    font = font.deriveFont(font.size2D - 1f)
                })
            } else {
                validConversations.forEach { conversation ->
                    val firstUserMessage = conversation.messages.first { it.role == "USER" }
                    val messagePanel = JPanel(BorderLayout()).apply {
                        background = Color(51, 51, 51)
                        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        addMouseListener(object : MouseAdapter() {
                            override fun mouseClicked(e: MouseEvent) {
                                viewModel.loadConversation(conversation.id)
                                tabbedPane.selectedIndex = 1 // Switch to chat tab
                            }
                            override fun mouseEntered(e: MouseEvent) {
                                background = Color(60, 60, 60)
                            }
                            override fun mouseExited(e: MouseEvent) {
                                background = Color(51, 51, 51)
                            }
                        })
                    }
                    
                    val content = JTextArea(firstUserMessage.content).apply {
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
        
        panel.add(welcomePanel)
        panel.add(historyPanel)
        
        // Add input panel for new task
        val homeInputPanel = createInputPanel().apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
        
        val homeInputArea = homeInputPanel.components.find { it is JBScrollPane }?.let { it as JBScrollPane }?.viewport?.view as? JTextArea
        val homeSendButton = homeInputPanel.components.find { it is JPanel }?.let { it as JPanel }?.components?.find { it is JButton && it.text == "Send" } as? JButton
        
        homeInputArea?.inputMap?.put(KeyStroke.getKeyStroke("ENTER"), "send")
        homeInputArea?.inputMap?.put(KeyStroke.getKeyStroke("shift ENTER"), "newline")
        homeInputArea?.actionMap?.put("send", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                val message = homeInputArea.text?.trim()
                if (message != null && message.isNotEmpty() && !viewModel.isProcessing()) {
                    viewModel.createNewTask()
                    sendMessage(message)
                    homeInputArea.text = ""
                }
            }
        })
        homeInputArea?.actionMap?.put("newline", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                homeInputArea.insert("\n", homeInputArea.caretPosition)
            }
        })
        
        homeSendButton?.addActionListener {
            val message = homeInputArea?.text?.trim()
            if (message != null && message.isNotEmpty() && !viewModel.isProcessing()) {
                viewModel.createNewTask()
                sendMessage(message)
                homeInputArea.text = ""
            }
        }
        
        panel.add(homeInputPanel)
        
        // Create config buttons panel
        val configButtonsPanel = JPanel().apply {
            background = Color(45, 45, 45)
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
            
            add(Box.createHorizontalGlue())
            add(JButton(".clinesystemprompt").apply {
                background = Color(51, 102, 153)
                foreground = Color.WHITE
                addActionListener {
                    openConfigEditor(".clinesystemprompt", viewModel.getSystemPrompt() ?: "")
                }
            })
            add(Box.createHorizontalStrut(10))
            add(JButton(".clinerules").apply {
                background = Color(51, 102, 153)
                foreground = Color.WHITE
                addActionListener {
                    openConfigEditor(".clinerules", readFileContent(".clinerules"))
                }
            })
            add(Box.createHorizontalGlue())
        }

        panel.add(configButtonsPanel, 0)

        homePanel.removeAll()
        homePanel.add(panel)
        homePanel.revalidate()
        homePanel.repaint()
    }
    
    private fun setupListeners() {
        viewModel.addMessageListener { messages ->
            SwingUtilities.invokeLater {
                refreshMessages()
            }
        }
        
        viewModel.addHistoryListener { conversations ->
            SwingUtilities.invokeLater {
                // Refresh history panel
                refreshHistory()
                
                // Refresh home panel
                homePanel.removeAll()
                homePanel.add(createHomePanel())
                
                // Revalidate and repaint all components
                homePanel.revalidate()
                homePanel.repaint()
                historyContent.revalidate()
                historyContent.repaint()
                tabbedPane.revalidate()
                tabbedPane.repaint()
                
                // Force layout update
                contentPanel.validate()
                contentPanel.repaint()
            }
        }

        viewModel.addStateListener { isProcessing ->
            SwingUtilities.invokeLater {
                sendButton.isEnabled = !isProcessing
                inputArea.isEnabled = !isProcessing
                
                // Ensure the components are enabled when not processing
                if (!isProcessing) {
                    sendButton.isEnabled = true
                    inputArea.isEnabled = true
                }
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
        SwingUtilities.invokeLater {
            chatPanel.removeAll()

            // Get all messages and process them
            val messages = viewModel.getMessages()
            messages.forEach { message ->
                addMessageToUI(message)
            }

            // Batch UI updates
            chatPanel.revalidate()
            chatPanel.repaint()
            
            // Smooth auto-scroll with animation
            val vertical = chatPanel.parent.parent as? JScrollPane
            if (vertical != null) {
                val scrollBar = vertical.verticalScrollBar
                val targetValue = scrollBar.maximum
                
                // Only animate if we're not already at the bottom
                if (scrollBar.value != targetValue) {
                    var timer: Timer? = null
                    timer = Timer(8) { // ~120 FPS for smoother animation
                        val currentValue = scrollBar.value
                        val step = (targetValue - currentValue) / 6 // Gentler interpolation
                        if (step > 0) {
                            scrollBar.value = currentValue + step.toInt().coerceAtLeast(1)
                            if (scrollBar.value >= targetValue - 1) {
                                timer?.stop()
                                scrollBar.value = targetValue // Ensure we reach exact target
                            }
                        } else {
                            timer?.stop()
                        }
                    }.apply {
                        isRepeats = true
                        start()
                    }
                }
            }
        }
    }

    private fun addMessageToUI(message: ClineMessage, animate: Boolean = true) {
        val timestamp = Instant.ofEpochMilli(message.timestamp)
            .atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        
        // Create message panel with fade-in effect
        val messagePanel = JPanel(BorderLayout()).apply {
            background = Color(45, 45, 45)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            if (animate && message.role == ClineMessage.Role.ASSISTANT) {
                // Start with a slightly different background color for fade-in effect
                background = Color(45, 45, 45).brighter()
                Timer(16) { // Fade in over ~300ms
                    background = Color(45, 45, 45)
                    revalidate()
                    repaint()
                }.apply {
                    initialDelay = 50 // Small delay before fade-in
                    isRepeats = false
                    start()
                }
            }
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
            
            // Add subtle transition effect for assistant messages
            if (animate && message.role == ClineMessage.Role.ASSISTANT) {
                background = Color(51, 51, 51).brighter()
                Timer(50) {
                    background = Color(51, 51, 51)
                    revalidate()
                    repaint()
                }.apply {
                    isRepeats = false
                    start()
                }
            }
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

    private fun openConfigEditor(fileName: String, currentContent: String) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            try {
                com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
                    // Ensure the file exists and has content
                    val configFile = File(project.basePath, fileName)
                    val fileContent = when {
                        configFile.exists() -> configFile.readText()
                        fileName == ".clinesystemprompt" -> DEFAULT_SYSTEM_PROMPT
                        else -> currentContent
                    }
                    
                    // Write content if file doesn't exist or content is different
                    if (!configFile.exists() || configFile.readText() != fileContent) {
                        configFile.writeText(fileContent)
                        if (fileName == ".clinesystemprompt") {
                            viewModel.setSystemPrompt(fileContent)
                        }
                    }

                    // Refresh and find the virtual file
                    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(configFile)
                    
                    if (virtualFile != null) {
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)
                    } else {
                        Messages.showErrorDialog(
                            project,
                            "Failed to open $fileName in editor",
                            "Error Opening File"
                        )
                    }
                }
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    project,
                    "Error opening file: ${e.message}",
                    "Error"
                )
            }
        }
    }

    private fun readFileContent(fileName: String): String {
        return try {
            com.intellij.openapi.application.ApplicationManager.getApplication()
                .runReadAction<String> {
                    val file = File(project.basePath, fileName)
                    if (file.exists()) {
                        file.readText()
                    } else {
                        ""
                    }
                }
        } catch (e: Exception) {
            ""
        }
    }

    private fun saveConfigFile(fileName: String, content: String) {
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            try {
                File(project.basePath, fileName).writeText(content)
                if (fileName == ".clinesystemprompt") {
                    viewModel.setSystemPrompt(content)
                }
                
                // Refresh the virtual file system
                val file = File(project.basePath, fileName)
                com.intellij.openapi.application.ApplicationManager.getApplication().runReadAction {
                    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                }
            } catch (e: Exception) {
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "Failed to save $fileName: ${e.message}",
                        "Error"
                    )
                }
            }
        }
    }

    fun getContent() = contentPanel
}
