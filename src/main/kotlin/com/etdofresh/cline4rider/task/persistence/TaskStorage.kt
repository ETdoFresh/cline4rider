package com.etdofresh.cline4rider.task.persistence

import com.etdofresh.cline4rider.task.model.ClineTask
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class TaskStorage(private val project: Project) {
    private val tasks = mutableListOf<ClineTask>()

    companion object {
        fun getInstance(project: Project): TaskStorage = project.service()
    }

    fun getTasks(): List<ClineTask> = tasks.toList()

    fun saveTasks(newTasks: List<ClineTask>) {
        tasks.clear()
        tasks.addAll(newTasks)
    }
}
