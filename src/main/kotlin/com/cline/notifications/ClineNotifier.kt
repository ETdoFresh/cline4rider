package com.cline.notifications

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object ClineNotifier {
    private const val GROUP_ID = "Cline Notifications"

    fun notifyError(project: Project?, content: String, title: String = "Cline Error") {
        notify(project, content, title, NotificationType.ERROR)
    }

    fun notifyWarning(project: Project?, content: String, title: String = "Cline Warning") {
        notify(project, content, title, NotificationType.WARNING)
    }

    fun notifyInfo(project: Project?, content: String, title: String = "Cline") {
        notify(project, content, title, NotificationType.INFORMATION)
    }

    private fun notify(
        project: Project?,
        content: String,
        title: String,
        type: NotificationType,
        configure: ((com.intellij.notification.Notification) -> Unit)? = null
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, type)
            .apply { configure?.invoke(this) }
            .notify(project)
    }

    fun notifyErrorWithAction(
        project: Project?,
        content: String,
        title: String = "Cline Error"
    ) {
        notify(project, content, title, NotificationType.ERROR) { notification ->
            notification.addAction(com.cline.utils.SettingsHelper.createConfigureAction(project ?: return@notify))
        }
    }
}
