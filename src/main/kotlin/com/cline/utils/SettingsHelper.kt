package com.cline.utils

import com.cline.settings.ClineSettingsDialog
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

object SettingsHelper {
    fun createConfigureAction(project: Project): NotificationAction {
        return NotificationAction.create("Configure Settings") { _: AnActionEvent, notification: Notification ->
            val dialog = ClineSettingsDialog(project)
            if (dialog.showAndGet()) {
                notification.expire()
            }
        }
    }
}
