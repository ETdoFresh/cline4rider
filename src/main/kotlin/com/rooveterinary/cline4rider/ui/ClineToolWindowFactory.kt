package com.rooveterinary.cline4rider.ui

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
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

        // Create navigation actions
        val tabs = listOf("Home", "Tasks", "History")
        tabs.forEach { tabName ->
            val action = object : ToggleAction(tabName, "", null) {
                init {
                    templatePresentation.apply {
                        text = tabName
                    }
                }

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
                    e.presentation.isEnabledAndVisible = true
                }
            }
            navGroup.add(action)
        }

        // Set Home as default selected
        mainContent.showTab("Home")

        // Add navigation group to tool window title
        toolWindow.setTitleActions(listOf(navGroup))
    }
}