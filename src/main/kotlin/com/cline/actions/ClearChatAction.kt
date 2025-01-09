package com.cline.actions

import com.cline.ui.model.ChatViewModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware

class ClearChatAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        project.service<ChatViewModel>().clearMessages()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val hasMessages = project.service<ChatViewModel>().getMessages().isNotEmpty()
            e.presentation.isEnabled = hasMessages
        } else {
            e.presentation.isEnabled = false
        }
    }
}
