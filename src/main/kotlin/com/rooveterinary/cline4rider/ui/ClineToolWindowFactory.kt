package com.rooveterinary.cline4rider.ui

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.options.ShowSettingsUtil
import javax.swing.ButtonGroup

class ClineToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        
        // Create main content component
        val mainContent = ClineToolWindowContent()
        
        // Add content to tool window
        val content = contentFactory.createContent(mainContent, "", false)
        toolWindow.contentManager.addContent(content)

        // Create action group for navigation
        val navGroup = object : DefaultActionGroup() {
            init {
                isPopup = false // Make it show as buttons
                templatePresentation.apply {
                    text = "" // No group label
                }
            }

            override fun update(e: AnActionEvent) {
                e.presentation.apply {
                    isEnabledAndVisible = true
                }
            }
        }

        // Load icons
        val icons = mapOf(
            "Home" to IconLoader.getIcon("/icons/home.svg", this::class.java),
            "Tasks" to IconLoader.getIcon("/icons/tasks.svg", this::class.java),
            "History" to IconLoader.getIcon("/icons/history.svg", this::class.java)
        )

        // Map of tab names to tooltips
        val tooltips = mapOf(
            "Home" to "Home",
            "Tasks" to "Current Task",
            "History" to "History"
        )

        // Create navigation actions
        val tabs = listOf("Home", "Tasks", "History")
        tabs.forEach { tabName ->
            val tooltip = tooltips[tabName] ?: tabName
            val action = object : ToggleAction(tooltip, tooltip, icons[tabName]) {
                override fun isSelected(e: AnActionEvent): Boolean {
                    return mainContent.getCurrentTab() == tabName
                }

                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    if (state) {
                        mainContent.showTab(tabName)
                    }
                }

                override fun update(e: AnActionEvent) {
                    super.update(e)
                    e.presentation.apply {
                        isEnabledAndVisible = true
                        icon = icons[tabName]
                    }
                }
            }
            navGroup.add(action)
        }

        // Add settings action
        val settingsAction = object : AnAction("Settings", "Settings", 
            IconLoader.getIcon("/icons/settings.svg", this::class.java)) {
            override fun actionPerformed(e: AnActionEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(null, "Cline4Rider")
            }

            override fun update(e: AnActionEvent) {
                e.presentation.apply {
                    isEnabledAndVisible = true
                }
            }
        }
        navGroup.addSeparator()
        navGroup.add(settingsAction)

        // Set Home as default selected
        mainContent.showTab("Home")

        // Add navigation group to tool window title
        toolWindow.setTitleActions(listOf(navGroup))
    }
}