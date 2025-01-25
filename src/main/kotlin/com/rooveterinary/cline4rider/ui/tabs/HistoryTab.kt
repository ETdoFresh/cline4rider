package com.rooveterinary.cline4rider.ui.tabs

import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import javax.swing.*
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import java.awt.FlowLayout
import java.awt.Dimension

class HistoryTab : BaseTabPanel() {
    private val sessionListModel = DefaultListModel<String>()
    private val sessionList: JBList<String>
    private val detailsArea: JTextArea
    
    init {
        layout = BorderLayout(0, 5)
        
        // Create toolbar
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("Clear History").apply {
                addActionListener {
                    onClearHistory()
                }
            })
            add(JButton("Export").apply {
                addActionListener {
                    onExport()
                }
            })
        }
        add(toolbar, BorderLayout.NORTH)
        
        // Create session list
        sessionList = JBList(sessionListModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addListSelectionListener {
                if (!it.valueIsAdjusting) {
                    onSessionSelected()
                }
            }
        }
        
        // Add some placeholder items
        sessionListModel.addElement("Session 1 - Code Review Task")
        sessionListModel.addElement("Session 2 - Architecture Discussion")
        sessionListModel.addElement("Session 3 - Bug Investigation")
        
        // Create split pane for list and details
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            topComponent = JBScrollPane(sessionList)
            
            // Create details panel
            detailsArea = JTextArea().apply {
                isEditable = false
                lineWrap = true
                wrapStyleWord = true
                text = "Select a session to view details"
            }
            
            bottomComponent = JPanel(BorderLayout()).apply {
                border = BorderFactory.createTitledBorder("Session Details")
                add(JBScrollPane(detailsArea), BorderLayout.CENTER)
                preferredSize = Dimension(-1, 150)
            }
            
            resizeWeight = 0.7
        }
        
        add(splitPane, BorderLayout.CENTER)
    }
    
    private fun onClearHistory() {
        val confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to clear all history?",
            "Confirm Clear History",
            JOptionPane.YES_NO_OPTION
        )
        if (confirm == JOptionPane.YES_OPTION) {
            sessionListModel.clear()
            detailsArea.text = "Select a session to view details"
        }
    }
    
    private fun onExport() {
        // Will implement export functionality in future
        JOptionPane.showMessageDialog(
            this,
            "Export functionality will be implemented in a future update",
            "Not Implemented",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    private fun onSessionSelected() {
        val selected = sessionList.selectedValue
        if (selected != null) {
            detailsArea.text = "Loading details for: $selected...\n\n" +
                             "This will show the full conversation and actions " +
                             "from the selected session in a future update."
        } else {
            detailsArea.text = "Select a session to view details"
        }
    }
    
    override fun onActivate() {
        // Refresh session list when tab becomes active
        // Will implement actual refresh in future
    }
    
    override fun onDeactivate() {
        // Save any pending changes or state
    }
}