package com.rooveterinary.cline4rider.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.rooveterinary.cline4rider.ui.tabs.HomeTab
import com.rooveterinary.cline4rider.ui.tabs.TasksTab
import com.rooveterinary.cline4rider.ui.tabs.HistoryTab
import com.rooveterinary.cline4rider.ui.tabs.TabPanel
import javax.swing.*
import java.awt.*
import javax.swing.border.EmptyBorder

class ClineToolWindowContent : JPanel() {
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)
    private var currentTab = "Home"
    
    private val tabPanels = mutableMapOf<String, TabPanel>()
    
    init {
        layout = BorderLayout()
        
        // Initialize tab panels
        tabPanels.apply {
            put("Home", HomeTab())
            put("Tasks", TasksTab())
            put("History", HistoryTab())
        }
        
        // Add content panels
        contentPanel.apply {
            tabPanels.forEach { (name, panel) ->
                add(panel.getPanel(), name)
            }
        }
        
        // Add content panel to main panel
        add(contentPanel, BorderLayout.CENTER)
        
        // Show default tab
        showTab("Home")
    }
    
    fun getCurrentTab(): String = currentTab
    
    fun showTab(tabName: String) {
        if (tabPanels.containsKey(tabName)) {
            // Deactivate current tab
            tabPanels[currentTab]?.onDeactivate()
            
            // Show new tab
            cardLayout.show(contentPanel, tabName)
            currentTab = tabName
            
            // Activate new tab
            tabPanels[tabName]?.onActivate()
            
            // Ensure the panel is visible and properly laid out
            tabPanels[tabName]?.getPanel()?.let { panel ->
                panel.revalidate()
                panel.repaint()
            }
        }
    }
}