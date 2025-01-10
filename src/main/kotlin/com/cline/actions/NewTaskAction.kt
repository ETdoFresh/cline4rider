package com.cline.actions

import com.cline.task.TaskManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager

class NewTaskAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val taskManager = TaskManager.getInstance(project)

        val title = Messages.showInputDialog(
            project,
            "Enter task title:",
            "New Task",
            Messages.getQuestionIcon()
        ) ?: return

        val description = Messages.showMultilineInputDialog(
            project,
            "Enter task description:",
            "New Task",
            "",
            Messages.getQuestionIcon(),
            null
        ) ?: return

        if (title.isNotEmpty()) {
            taskManager.addTask(title, description)

            // Open the Tasks window if it's not already visible
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("Cline Tasks")
            if (toolWindow != null && !toolWindow.isVisible) {
                toolWindow.show()
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null
    }
}
