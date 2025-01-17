package com.etdofresh.cline4rider.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.options.ShowSettingsUtil

class SettingsAction : AnAction("Settings", "Open Cline settings", AllIcons.General.Settings) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Cline4Rider")
    }

    override fun update(e: AnActionEvent) {
        e.presentation.apply {
            isEnabled = e.project != null
            isVisible = true
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}