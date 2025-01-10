package com.etdofresh.cline4rider.task

import com.etdofresh.cline4rider.task.model.ClineTask
import com.etdofresh.cline4rider.task.persistence.TaskStorage
import com.etdofresh.cline4rider.model.ClineMessage
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class TaskManager(private val project: Project) {
    private val storage = TaskStorage.getInstance(project)
    private val tasks = mutableListOf<ClineTask>()
    private val listeners = mutableListOf<(List<ClineTask>) -> Unit>()

    init {
        tasks.addAll(storage.getTasks())
    }

    companion object {
        fun getInstance(project: Project): TaskManager = project.service()
    }

    fun addTask(title: String, description: String): ClineTask {
        val task = ClineTask(
            title = title,
            description = description,
            createdAt = System.currentTimeMillis()
        )
        tasks.add(task)
        storage.saveTasks(tasks)
        notifyListeners()
        return task
    }

    fun getTasks(): List<ClineTask> = tasks.toList()

    fun getTask(index: Int): ClineTask? = tasks.getOrNull(index)

    fun updateTask(task: ClineTask) {
        val index = tasks.indexOfFirst { it == task }
        if (index != -1) {
            tasks[index] = task
            storage.saveTasks(tasks)
            notifyListeners()
        }
    }

    fun deleteTask(task: ClineTask) {
        if (tasks.remove(task)) {
            storage.saveTasks(tasks)
            notifyListeners()
        }
    }

    fun addListener(listener: (List<ClineTask>) -> Unit) {
        listeners.add(listener)
        listener(tasks)
    }

    fun removeListener(listener: (List<ClineTask>) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        val currentTasks = tasks.toList()
        listeners.forEach { it(currentTasks) }
    }
}
