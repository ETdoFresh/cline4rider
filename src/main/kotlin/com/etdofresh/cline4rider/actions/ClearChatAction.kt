package com.etdofresh.cline4rider.actions

import com.etdofresh.cline4rider.ui.model.ChatViewModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class ClearChatAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val viewModel = project.service<ChatViewModel>()
        viewModel.clearMessages()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null && project.service<ChatViewModel>().getMessages().isNotEmpty()
    }
}
