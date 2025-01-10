package com.cline.task.model

import com.intellij.util.xmlb.annotations.Tag

@Tag("task")
data class ClineTask(
    var title: String = "",
    var description: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null,
    var status: TaskStatus = TaskStatus.PENDING
) {
    enum class TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
