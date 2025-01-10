package com.cline.task.persistence

import com.cline.task.model.ClineTask
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

@State(
    name = "TaskStorage",
    storages = [Storage("cline-tasks.xml")]
)
class TaskStorage : PersistentStateComponent<TaskStorage> {
    @XCollection
    private var tasks: MutableList<SerializableTask> = mutableListOf()

    companion object {
        fun getInstance(project: Project): TaskStorage = project.service()
    }

    fun getTasks(): List<ClineTask> {
        return tasks.map { it.toClineTask() }
    }

    fun saveTasks(tasks: List<ClineTask>) {
        this.tasks = tasks.map { SerializableTask.fromClineTask(it) }.toMutableList()
    }

    override fun getState(): TaskStorage = this

    override fun loadState(state: TaskStorage) {
        XmlSerializerUtil.copyBean(state, this)
    }

    @Tag("task")
    class SerializableTask {
        var title: String = ""
        var description: String = ""
        var createdAt: Long = 0
        var completedAt: Long? = null
        var status: ClineTask.TaskStatus = ClineTask.TaskStatus.PENDING

        companion object {
            fun fromClineTask(task: ClineTask): SerializableTask {
                return SerializableTask().apply {
                    title = task.title
                    description = task.description
                    createdAt = task.createdAt
                    completedAt = task.completedAt
                    status = task.status
                }
            }
        }

        fun toClineTask(): ClineTask {
            return ClineTask(
                title = title,
                description = description,
                createdAt = createdAt,
                completedAt = completedAt,
                status = status
            )
        }
    }
}
