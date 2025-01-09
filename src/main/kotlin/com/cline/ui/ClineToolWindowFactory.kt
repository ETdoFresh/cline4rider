package com.cline.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ClineToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(
            Runnable {
                if (project.isDisposed) return@Runnable
                
                val clineToolWindow = ClineToolWindow(project)
                val contentFactory = ContentFactory.getInstance()
                val content = contentFactory.createContent(clineToolWindow.getContent(), "", false)
                toolWindow.contentManager.addContent(content)

                // Focus the input field when the tool window is shown
                toolWindow.show { clineToolWindow.focusInput() }
            },
            com.intellij.openapi.application.ModalityState.NON_MODAL
        )
    }

    override fun shouldBeAvailable(project: Project) = true
}
