package com.cline.task.ui

import com.cline.task.TaskManager
import com.cline.task.model.ClineTask
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.*
import java.awt.BorderLayout
import javax.swing.*

class TaskHistoryPanel(project: Project) {
    private val taskManager = TaskManager.getInstance(project)
    private val contentPanel = JPanel(BorderLayout())
    private val taskListPanel = JPanel()
    private var selectedTask: ClineTask? = null
    private val listeners = mutableListOf<(ClineTask?) -> Unit>()

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        taskListPanel.layout = BoxLayout(taskListPanel, BoxLayout.Y_AXIS)
        val scrollPane = JBScrollPane(taskListPanel)
        contentPanel.add(scrollPane, BorderLayout.CENTER)
        refreshTasks()
    }

    private fun setupListeners() {
        taskManager.addListener { _ ->
            SwingUtilities.invokeLater {
                refreshTasks()
            }
        }
    }

    private fun refreshTasks() {
        taskListPanel.removeAll()

        taskManager.getTasks().forEach { task ->
            val taskPanel = panel {
                row {
                    label(task.title)
                    button("Select") {
                        selectedTask = task
                        notifyListeners()
                    }
                }
            }
            taskListPanel.add(taskPanel)
        }

        taskListPanel.revalidate()
        taskListPanel.repaint()
    }

    fun addSelectionListener(listener: (ClineTask?) -> Unit) {
        listeners.add(listener)
        listener(selectedTask)
    }

    fun removeSelectionListener(listener: (ClineTask?) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it(selectedTask) }
    }

    fun getContent() = contentPanel
}
