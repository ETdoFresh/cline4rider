package com.rooveterinary.cline4rider.ui.tabs

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import javax.swing.*
import java.awt.BorderLayout
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Insets

class HomeTab : BaseTabPanel() {
    private val modeSelector: JComboBox<String>
    private val modelSelector: JComboBox<String>
    private val newsArea: JTextArea
    private val inputField: JBTextField
    
    init {
        layout = BorderLayout()
        
        // Create main content panel
        val contentPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(5, 5, 5, 5)
        }
        
        // Mode selector
        gbc.gridx = 0
        gbc.gridy = 0
        contentPanel.add(JLabel("Mode:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 1.0
        modeSelector = JComboBox(arrayOf("Code", "Architect", "Ask")).apply {
            addActionListener {
                // Will handle mode changes in future implementations
            }
        }
        contentPanel.add(modeSelector, gbc)
        
        // Model selector
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        contentPanel.add(JLabel("Model:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 1.0
        modelSelector = JComboBox(arrayOf("GPT-4", "Claude")).apply {
            addActionListener {
                // Will handle model changes in future implementations
            }
        }
        contentPanel.add(modelSelector, gbc)
        
        // News section
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 2
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        newsArea = JTextArea().apply {
            text = "News and updates will appear here"
            isEditable = false
        }
        contentPanel.add(JBScrollPane(newsArea), gbc)
        
        // Input box
        gbc.gridy = 3
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        inputField = JBTextField("Enter your request here...").apply {
            addActionListener {
                // Will handle input in future implementations
            }
        }
        contentPanel.add(inputField, gbc)
        
        // Add content panel to tab
        add(contentPanel, BorderLayout.CENTER)
    }
    
    override fun onActivate() {
        // Refresh news content when tab becomes active
        newsArea.text = "Loading news..."
        // Will implement actual news loading in future
    }
    
    override fun onDeactivate() {
        // Save any pending changes or state
    }
}