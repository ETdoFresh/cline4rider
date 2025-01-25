package com.rooveterinary.cline4rider.ui.tabs

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension

class TasksTab : BaseTabPanel() {
    private val chatArea: JTextArea
    private val inputField: JBTextField
    private val commandButtons: List<JButton>
    
    init {
        layout = BorderLayout()
        
        // Initialize chat area
        chatArea = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        
        // Create scrollable chat panel
        val chatScrollPane = JBScrollPane(chatArea).apply {
            preferredSize = Dimension(400, 300)
            verticalScrollBar.unitIncrement = 16
        }
        
        // Create input panel
        inputField = JBTextField().apply {
            toolTipText = "Type your message here..."
        }
        
        val inputPanel = JPanel(BorderLayout(5, 0)).apply {
            add(inputField, BorderLayout.CENTER)
            add(JButton("Send").apply {
                addActionListener {
                    val message = inputField.text
                    if (message.isNotBlank()) {
                        appendUserMessage(message)
                        inputField.text = ""
                    }
                }
            }, BorderLayout.EAST)
        }
        
        // Create command panel
        val commandPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("Commands")
        }
        
        // Initialize command buttons
        commandButtons = listOf(
            "Read File",
            "Write File",
            "Execute Command"
        ).map { commandName ->
            JButton(commandName).apply {
                alignmentX = CENTER_ALIGNMENT
                maximumSize = Dimension(150, 30)
                addActionListener {
                    onCommandClick(commandName)
                }
                commandPanel.add(this)
                commandPanel.add(Box.createVerticalStrut(5))
            }
        }
        
        // Add all components to the main panel
        add(chatScrollPane, BorderLayout.CENTER)
        add(inputPanel, BorderLayout.SOUTH)
        add(commandPanel, BorderLayout.EAST)
    }
    
    private fun appendUserMessage(message: String) {
        chatArea.append("\nUser: $message")
        chatArea.caretPosition = chatArea.document.length
    }
    
    private fun onCommandClick(commandName: String) {
        chatArea.append("\nExecuting command: $commandName...")
        chatArea.caretPosition = chatArea.document.length
    }
    
    override fun onActivate() {
        inputField.requestFocusInWindow()
    }
    
    override fun onDeactivate() {
        // Save any pending chat history or state
    }
}