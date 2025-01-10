package com.etdofresh.cline4rider.task.model

import java.io.Serializable

data class ClineTask(
    val title: String,
    val description: String,
    val createdAt: Long,
    var completed: Boolean = false
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as ClineTask
        
        if (title != other.title) return false
        if (description != other.description) return false
        if (createdAt != other.createdAt) return false
        
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
