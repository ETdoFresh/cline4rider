package com.cline.ui

import com.cline.model.ClineMessage
import com.cline.ui.model.ChatViewModel
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JPanel
import javax.swing.JTextArea

class ClineToolWindow(private val project: Project) : JPanel(BorderLayout()) {
    private val viewModel = ChatViewModel(project)
    private val chatArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }
    private val inputArea = JTextArea(3, 50).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Add chat display area
        add(JBScrollPane(chatArea), BorderLayout.CENTER)
        
        // Add input area
        add(JBScrollPane(inputArea), BorderLayout.SOUTH)
    }

    private fun setupListeners() {
        // Listen for chat updates
        viewModel.addListener { messages ->
            updateChatDisplay(messages)
        }

        // Handle input submission
        inputArea.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) {}
            override fun keyReleased(e: KeyEvent?) {}
            
            override fun keyPressed(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ENTER && e.isShiftDown) {
                    e.consume()
                    val message = inputArea.text.trim()
                    if (message.isNotEmpty()) {
                        viewModel.sendMessage(message)
                        inputArea.text = ""
                    }
                }
            }
        })
    }

    private fun updateChatDisplay(messages: List<ClineMessage>) {
        val displayText = messages.joinToString("\n\n") { message ->
            when (message.type) {
                com.cline.model.MessageType.TASK_REQUEST -> "You: ${message.content}"
                com.cline.model.MessageType.TASK_COMPLETE -> "Cline: ${message.content}"
                else -> "${message.type}: ${message.content}"
            }
        }
        chatArea.text = displayText
        chatArea.caretPosition = chatArea.document.length
    }

    fun getContent() = this
}
