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
                com.intellij.ide.BrowserUtil.browse(e.url)
            }
        }
    }
    
    private val inputArea = JTextArea(3, 50).apply {
        lineWrap = true
        wrapStyleWord = true
        font = JBUI.Fonts.create("JetBrains Mono", 12)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        )
    }

    private val clearButton = JButton("Clear Chat").apply {
        addActionListener {
            viewModel.clearMessages()
        }
    }

    private val submitButton = JButton("Submit").apply {
        addActionListener {
            submitMessage()
        }
    }

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Chat display area with padding
        val chatPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            add(JBScrollPane(chatArea), BorderLayout.CENTER)
        }
        add(chatPanel, BorderLayout.CENTER)
        
        // Input panel with buttons
        val inputPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(0, 5, 5, 5)
            
            // Add input area
            add(JBScrollPane(inputArea), BorderLayout.CENTER)
            
            // Add buttons panel
            val buttonsPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
                add(clearButton)
                add(submitButton)
            }
            add(buttonsPanel, BorderLayout.SOUTH)
        }
        add(inputPanel, BorderLayout.SOUTH)
    }

    private fun setupListeners() {
        // Listen for chat updates
        viewModel.addListener { messages ->
            updateChatDisplay(messages)
            clearButton.isEnabled = messages.isNotEmpty()
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
            viewModel.sendMessage(message)
            inputArea.text = ""
            inputArea.requestFocusInWindow()
        }
    }

    private fun updateSubmitButton() {
        submitButton.isEnabled = inputArea.text.trim().isNotEmpty()
    }

    fun focusInput() {
        inputArea.requestFocusInWindow()
    }

    private fun updateChatDisplay(messages: List<ClineMessage>) {
        val htmlContent = StringBuilder()
        htmlContent.append("""
            <html>
            <head>
                <style>
                    body { margin: 10px; font-family: "JetBrains Mono", monospace; }
                    .message { padding: 10px; margin: 5px 0; border-radius: 5px; }
                    .message pre { margin: 5px 0; white-space: pre-wrap; }
                    .message code { font-family: "JetBrains Mono", monospace; }
                    .timestamp { font-size: 0.8em; color: #666; float: right; }
                </style>
            </head>
            <body>
        """.trimIndent())
        
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
                    <div class='timestamp'>$timestamp</div>
                    <strong>$icon ${if (message.type == MessageType.TASK_REQUEST) "You" else "Cline"}:</strong>
                    <pre><code>${
                        message.content
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("\n", "<br/>")
                            .replace("📁", "📁 ")  // Add space after folder emoji
                            .replace("📄", "📄 ")  // Add space after file emoji
                    }</code></pre>
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

    fun getContent() = this
}
