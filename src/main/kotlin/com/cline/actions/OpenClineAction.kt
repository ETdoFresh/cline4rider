package com.cline.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

class OpenClineAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Cline") ?: return
        
        if (!toolWindow.isVisible) {
            toolWindow.show {
                // Focus the input field when the tool window opens
                val content = toolWindow.contentManager.getContent(0)
                (content?.component as? com.cline.ui.ClineToolWindow)?.focusInput()
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
