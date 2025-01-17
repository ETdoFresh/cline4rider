package com.etdofresh.cline4rider.actions

import com.etdofresh.cline4rider.ui.ClineToolWindow
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.wm.ToolWindowManager

class HomeAction : AnAction("Home", "Switch to home view", AllIcons.Actions.Menu_open) {
    override fun actionPerformed(e: AnActionEvent) {
        val clineToolWindow = com.etdofresh.cline4rider.ui.ClineToolWindowFactory.getInstance() ?: return
        clineToolWindow.showHomeView()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.apply {
            isEnabled = e.project != null
            isVisible = true
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}