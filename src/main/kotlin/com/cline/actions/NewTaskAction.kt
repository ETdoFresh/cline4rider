package com.cline.actions

import com.cline.ui.ClineToolWindow
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

class NewTaskAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Focus the Cline tool window and input area
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Cline")
        toolWindow?.show {
            // Get the content and focus the input
            toolWindow.contentManager.selectedContent?.let { content ->
                (content.component as? ClineToolWindow)?.focusInput()
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
