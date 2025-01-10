package com.cline.utils

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

object SettingsHelper {
    fun createConfigureAction(project: Project): NotificationAction {
        return NotificationAction.create("Configure Settings") { _: AnActionEvent, notification: Notification ->
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Cline")
            notification.expire()
        }
    }
}
