package com.cline.task.ui

import com.cline.task.model.ClineTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.OnePixelSplitter
import java.awt.BorderLayout
import javax.swing.JPanel

class TaskToolWindow(project: Project, _toolWindow: ToolWindow) {
    private val contentPanel = JPanel(BorderLayout())
    private val historyPanel = TaskHistoryPanel(project)
    private var currentTask: ClineTask? = null
    private var headerPanel: TaskHeaderPanel? = null

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val splitter = OnePixelSplitter(false, 0.3f)
        splitter.firstComponent = historyPanel.getContent()
        contentPanel.add(splitter, BorderLayout.CENTER)

        historyPanel.addSelectionListener { task ->
            currentTask = task
            headerPanel = task?.let { TaskHeaderPanel(it) }
            splitter.secondComponent = headerPanel?.getContent()
            splitter.revalidate()
            splitter.repaint()
        }
    }

    private fun setupListeners() {
        // Add any additional listeners here
    }

    fun getContent() = contentPanel
}
