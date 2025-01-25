package com.rooveterinary.cline4rider.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.ShowSettingsAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.rooveterinary.cline4rider.ui.tabs.HomeTab
import com.rooveterinary.cline4rider.ui.tabs.TasksTab
import com.rooveterinary.cline4rider.ui.tabs.HistoryTab
import com.rooveterinary.cline4rider.ui.tabs.TabPanel
import javax.swing.*
import java.awt.*
import javax.swing.border.EmptyBorder
import com.intellij.openapi.util.IconLoader

class ClineToolWindowContent : JPanel() {
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)
    private var currentTab = "Home"
    
    private val tabPanels = mutableMapOf<String, TabPanel>()
    
    private val tabIcons = mapOf(
        "Home" to IconLoader.getIcon("/icons/home.svg", javaClass),
        "Tasks" to IconLoader.getIcon("/icons/tasks.svg", javaClass),
        "History" to IconLoader.getIcon("/icons/history.svg", javaClass)
    )
    
    init {
        layout = BorderLayout()
        
        // Initialize tab panels
        tabPanels.apply {
            put("Home", HomeTab())
            put("Tasks", TasksTab())
            put("History", HistoryTab())
        }
        
        // Create toolbar
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            background = JBColor.background()
            border = EmptyBorder(JBUI.insets(2))
        }
        
        // Add tab buttons
        tabPanels.keys.forEach { tabName ->
            val button = createTabButton(tabName, tabIcons[tabName])
            toolbar.add(button)
        }
        
        // Add settings button
        val settingsButton = createSettingsButton()
        toolbar.add(settingsButton)
        
        // Add toolbar and content panel
        add(toolbar, BorderLayout.NORTH)
        
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
    
    private fun createTabButton(tabName: String, icon: Icon?): JButton {
        return JButton(icon).apply {
            toolTipText = tabName
            border = EmptyBorder(4, 4, 4, 4)
            isContentAreaFilled = false
            addActionListener { showTab(tabName) }
        }
    }
    
    private fun createSettingsButton(): JComponent {
        val settingsIcon = IconLoader.getIcon("/icons/settings.svg", javaClass)
        val settingsAction = ShowSettingsAction()
        return ActionButton(settingsAction, settingsAction.templatePresentation, "Cline", JBUI.size(24))
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