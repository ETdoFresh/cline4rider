package com.etdofresh.cline4rider.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.ContentFactory

class ClineToolWindowFactory : ToolWindowFactory {
    companion object {
        private var instance: ClineToolWindow? = null
        
        fun getInstance(): ClineToolWindow? = instance
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        instance = ClineToolWindow(project, toolWindow)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(instance!!.getContent(), "", false)
        toolWindow.contentManager.addContent(content)

        // Add actions to the toolbar
        if (toolWindow is ToolWindowEx) {
            val actionManager = ActionManager.getInstance()
            val group = actionManager.getAction("Cline.Toolbar") as? DefaultActionGroup
            if (group != null) {
                val actions = group.getChildren(null)
                toolWindow.setTitleActions(*actions)
            }
        }
    }
}
