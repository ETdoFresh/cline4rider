package com.cline.ui

import com.cline.model.ClineMessage
import com.cline.model.MessageType
import com.cline.ui.model.ChatViewModel
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ClineToolWindow(private val project: Project) : JPanel(BorderLayout()) {
    private val viewModel = ChatViewModel(project)
    private val chatArea = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
        font = JBUI.Fonts.create("JetBrains Mono", 12)
        addHyperlinkListener { e ->
            if (e.eventType == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                val url = e.url?.toString() ?: return@addHyperlinkListener
                if (url.startsWith("cline://showTask/")) {
                    val taskId = url.removePrefix("cline://showTask/")
                    showTaskDetails(taskId)
                } else {
                    com.intellij.ide.BrowserUtil.browse(e.url)
                }
            }
        }
    }
    
    private val inputArea = JTextArea(3, 50).apply {
        lineWrap = true
        wrapStyleWord = true
        font = JBUI.Fonts.create("JetBrains Mono", 13)
        background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
        foreground = JBUI.CurrentTheme.Label.foreground()
        caretColor = JBUI.CurrentTheme.Label.foreground()
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        )
    }

    private val clearButton = JButton("Clear Chat").apply {
        font = JBUI.Fonts.create("Segoe UI", 12)
        background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
        foreground = JBUI.CurrentTheme.Label.foreground()
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        )
        isFocusPainted = false
        addActionListener {
            viewModel.clearMessages()
        }
    }

    private val submitButton = JButton("Submit").apply {
        font = JBUI.Fonts.create("Segoe UI", 12)
        background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
        foreground = JBUI.CurrentTheme.Label.foreground()
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        )
        isFocusPainted = false
        addActionListener {
            submitMessage()
        }
    }

    private fun getWelcomeMessage(): String {
        val recentTasksHtml = viewModel.getRecentTasks()
            .take(5)
            .joinToString("\n") { task ->
                val status = task.metadata["status"] ?: "pending"
                val statusColor = when (status) {
                    "completed" -> "#4CAF50"
                    "error" -> "#FF5252"
                    else -> "#888888"
                }
                val statusDot = "●"
                val timestamp = task.metadata["timestamp"]?.let {
                    java.time.Instant.ofEpochMilli(it.toLong())
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                } ?: ""

                """
                <div class='recent-task' onclick='showTask("${task.metadata["taskId"]}")'>
                    <div style='display: flex; justify-content: space-between; align-items: center;'>
                        <span style='color: $statusColor'>$statusDot</span>
                        <span class='task-time'>$timestamp</span>
                    </div>
                    <div style='margin: 4px 0;'>${task.content.take(100)}${if (task.content.length > 100) "..." else ""}</div>
                    ${task.metadata["tokens"]?.let { "<div class='task-tokens'>Tokens: $it</div>" } ?: ""}
                    ${task.metadata["completion"]?.let { 
                        """
                        <div style='margin-top: 8px; padding-top: 8px; border-top: 1px solid #383838;'>
                            <div style='color: #888; font-size: 0.9em;'>Response:</div>
                            <div style='margin-top: 4px;'>${it.take(100)}${if (it.length > 100) "..." else ""}</div>
                        </div>
                        """.trimIndent()
                    } ?: ""}
                </div>
                """.trimIndent()
            }

        return """
            <div style='margin: 20px; color: #A9B7C6;'>
                <h2>What can I do for you?</h2>
                <p>Thanks to Claude 3.5 Sonnet's agentic coding capabilities, I can handle complex software development tasks step-by-step.</p>
                <p>With tools that let me create & edit files, explore complex projects, use the browser, and execute terminal commands 
                (after you grant permission), I can assist you in ways that go beyond code completion or tech support.</p>
                <p>I can even use MCP to create new tools and extend my own capabilities.</p>
                <div style='margin-top: 20px;'>
                    <h3 style='color: #A9B7C6;'>Recent Tasks</h3>
                    <div id='recentTasks'>
                        $recentTasksHtml
                    </div>
                </div>
            </div>
        """.trimIndent()
    }

    init {
        setupUI()
        setupListeners()
        
        // Defer welcome message display until application is ready
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater({
            if (project.isDisposed) return@invokeLater
            updateChatDisplay(emptyList(), showWelcome = true)
        }, project.disposed)
    }

    private val settingsButton = JButton("⚙").apply {
        font = JBUI.Fonts.create("Segoe UI", 14)
        background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
        foreground = JBUI.CurrentTheme.Label.foreground()
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        )
        isFocusPainted = false
        toolTipText = "Settings"
        addActionListener {
            val dialog = com.cline.settings.ClineSettingsDialog(project)
            if (dialog.showAndGet()) {
                // Settings were updated, refresh the UI if needed
                updateChatDisplay(viewModel.getMessages())
            }
        }
    }

    private fun setupUI() {
        SwingUtilities.invokeLater {
            background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
            
            // Create all panels first
            val toolbar = JPanel(BorderLayout())
            val chatPanel = JPanel(BorderLayout())
            val inputPanel = JPanel(BorderLayout())
            val inputWrapper = JPanel(BorderLayout())
            val buttonsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
            
            // Configure toolbar
            toolbar.apply {
                background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
                )
                add(settingsButton, BorderLayout.EAST)
            }
            
            // Configure chat panel
            chatPanel.apply {
                background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                val scrollPane = JBScrollPane(chatArea).apply {
                    border = BorderFactory.createEmptyBorder()
                    viewport.background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                }
                add(scrollPane, BorderLayout.CENTER)
            }
            
            // Configure input area components
            inputWrapper.apply {
                background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                val scrollPane = JBScrollPane(inputArea).apply {
                    border = BorderFactory.createEmptyBorder()
                    viewport.background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                }
                add(scrollPane, BorderLayout.CENTER)
                add(JLabel("Type your task here (@ to add context)...").apply {
                    foreground = JBUI.CurrentTheme.Label.disabledForeground()
                    font = JBUI.Fonts.create("Segoe UI", 12)
                    border = BorderFactory.createEmptyBorder(0, 2, 4, 0)
                }, BorderLayout.NORTH)
            }
            
            // Configure buttons panel
            buttonsPanel.apply {
                background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                add(clearButton)
                add(submitButton)
            }
            
            // Configure input panel
            inputPanel.apply {
                background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)
                )
                add(inputWrapper, BorderLayout.CENTER)
                add(buttonsPanel, BorderLayout.SOUTH)
            }
            
            // Add all components to the main panel
            add(toolbar, BorderLayout.NORTH)
            add(chatPanel, BorderLayout.CENTER)
            add(inputPanel, BorderLayout.SOUTH)
            
            // Initial UI state
            updateUIState(false)
            revalidate()
            repaint()
        }
    }

    private fun setupListeners() {
        // Listen for chat updates
        viewModel.addMessageListener { messages ->
            updateChatDisplay(messages)
            if (!viewModel.isProcessing()) {
                clearButton.isEnabled = messages.isNotEmpty()
            }
        }

        // Listen for processing state changes
        viewModel.addStateListener { isProcessing ->
            updateUIState(isProcessing)
        }

        // Handle input submission
        inputArea.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) {}
            override fun keyReleased(e: KeyEvent?) {}
            
            override fun keyPressed(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ENTER && e.isShiftDown) {
                    e.consume()
                    submitMessage()
                }
            }
        })

        // Update submit button state based on input
        inputArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateSubmitButton()
            override fun removeUpdate(e: DocumentEvent?) = updateSubmitButton()
            override fun changedUpdate(e: DocumentEvent?) = updateSubmitButton()
        })
    }

    private fun submitMessage() {
        val message = inputArea.text.trim()
        if (message.isNotEmpty()) {
            // Disable input and show loading state
            inputArea.isEnabled = false
            submitButton.isEnabled = false
            clearButton.isEnabled = false
            submitButton.text = "Processing..."

            viewModel.sendMessage(message)
            inputArea.text = ""
        }
    }

    private fun updateUIState(isProcessing: Boolean) {
        inputArea.isEnabled = !isProcessing
        submitButton.isEnabled = !isProcessing && inputArea.text.trim().isNotEmpty()
        clearButton.isEnabled = !isProcessing && viewModel.getMessages().isNotEmpty()
        submitButton.text = if (isProcessing) "Processing..." else "Submit"
        
        if (!isProcessing) {
            inputArea.requestFocusInWindow()
        }
    }

    private fun updateSubmitButton() {
        submitButton.isEnabled = inputArea.text.trim().isNotEmpty()
    }

    fun focusInput() {
        inputArea.requestFocusInWindow()
    }

    private fun updateChatDisplay(messages: List<ClineMessage>, showWelcome: Boolean = false) {
        if (!com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread) {
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater({
                if (project.isDisposed) return@invokeLater
                updateChatDisplay(messages, showWelcome)
            }, project.disposed)
            return
        }
        val htmlContent = StringBuilder()
        htmlContent.append("""
            <html>
            <head>
                <script>
                    function showTask(taskId) {
                        // Use Java callback to show task details
                        window.location.href = "cline://showTask/" + taskId;
                    }
                </script>
                <style>
                    body { 
                        margin: 10px; 
                        font-family: "JetBrains Mono", monospace;
                        background-color: #1E1E1E;
                    }
                    .message { 
                        padding: 12px 16px; 
                        margin: 8px 0; 
                        border-radius: 6px;
                        line-height: 1.4;
                    }
                    .message pre { 
                        margin: 8px 0; 
                        white-space: pre-wrap;
                        background-color: rgba(0, 0, 0, 0.1);
                        padding: 8px;
                        border-radius: 4px;
                    }
                    .message code { 
                        font-family: "JetBrains Mono", monospace;
                        font-size: 12px;
                    }
                    .timestamp { 
                        font-size: 0.85em; 
                        color: #888; 
                        float: right;
                        margin-left: 8px;
                    }
                    .message-header {
                        display: flex;
                        align-items: center;
                        margin-bottom: 8px;
                    }
                    .message-icon {
                        margin-right: 8px;
                        font-size: 16px;
                    }
                    .message-sender {
                        font-weight: bold;
                        color: #A9B7C6;
                    }
                    .recent-task { 
                        background-color: #2D2D2D; 
                        padding: 12px; 
                        margin: 8px 0; 
                        border-radius: 6px;
                        cursor: pointer;
                        border: 1px solid #383838;
                    }
                    .recent-task:hover { 
                        background-color: #353535; 
                        border-color: #454545;
                    }
                    .task-time { 
                        color: #888; 
                        font-size: 0.9em;
                    }
                    .task-tokens { 
                        color: #888; 
                        font-size: 0.85em;
                        float: right;
                        background: rgba(255, 255, 255, 0.1);
                        padding: 2px 6px;
                        border-radius: 4px;
                    }
                </style>
            </head>
            <body>
        """.trimIndent())

        if (showWelcome && messages.isEmpty()) {
            htmlContent.append(getWelcomeMessage())
        }
        
        messages.forEach { message ->
            val (backgroundColor, textColor, icon) = when (message.type) {
                MessageType.TASK_REQUEST -> Triple("#2B2B2B", "#A9B7C6", "💭")
                MessageType.TASK_COMPLETE -> Triple("#2D3B3D", "#A9B7C6", "🤖")
                MessageType.ERROR -> Triple("#3D2829", "#FF6B68", "⚠️")
                else -> Triple("#2B2B2B", "#A9B7C6", "ℹ️")
            }
            
            val timestamp = message.metadata["timestamp"]?.let { 
                java.time.Instant.ofEpochMilli(it.toLong())
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
            } ?: ""

            htmlContent.append("""
                <div class='message' style='background-color: $backgroundColor; color: $textColor;'>
                    <div class='message-header'>
                        <span class='message-icon'>$icon</span>
                        <span class='message-sender'>${if (message.type == MessageType.TASK_REQUEST) "You" else "Cline"}</span>
                        <span class='timestamp'>$timestamp</span>
                    </div>
                    <div class='message-content'>
                        <pre><code>${
                            message.content
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\n", "<br/>")
                                .replace("📁", "📁 ")
                                .replace("📄", "📄 ")
                                .let { content ->
                                    // Add syntax highlighting for code blocks
                                    if (content.contains("```")) {
                                        content.replace(
                                            Regex("```(\\w*)\\s*([\\s\\S]*?)```"),
                                            "<div class='code-block' style='background: rgba(0,0,0,0.2); padding: 8px; border-radius: 4px; margin: 8px 0;'><div class='code-header' style='color: #888; font-size: 0.9em; margin-bottom: 4px;'>\$1</div>\$2</div>"
                                        )
                                    } else {
                                        content
                                    }
                                }
                        }</code></pre>
                    </div>
                    ${
                        message.metadata["tokens"]?.let { tokens ->
                            "<div class='task-tokens'>Tokens: $tokens</div>"
                        } ?: ""
                    }
                </div>
            """.trimIndent())
        }
        
        htmlContent.append("</body></html>")
        
        // Update the chat area and scroll to bottom
        chatArea.text = htmlContent.toString()
        SwingUtilities.invokeLater {
            val doc = chatArea.document
            chatArea.caretPosition = doc.length
        }
    }

    private fun showTaskDetails(taskId: String) {
        val task = viewModel.getRecentTasks().find { it.metadata["taskId"] == taskId } ?: return
        val completion = task.metadata["completion"]
        val status = task.metadata["status"] ?: "pending"
        val tokens = task.metadata["tokens"]
        val timestamp = task.metadata["timestamp"]?.let {
            java.time.Instant.ofEpochMilli(it.toLong())
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } ?: ""

        TaskDetailsDialog(
            project = project,
            content = task.content,
            completion = completion,
            status = status,
            tokens = tokens,
            timestamp = timestamp
        ).show()
    }

    // No need for getContent() as the class itself is the content panel
}
