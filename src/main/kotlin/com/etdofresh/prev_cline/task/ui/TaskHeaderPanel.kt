package com.cline.task.ui

import com.cline.task.model.ClineTask
import com.intellij.ui.dsl.builder.*
import javax.swing.JPanel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TaskHeaderPanel(private val task: ClineTask, private val isEditable: Boolean = false) {
    private val panel: JPanel = panel {
        row {
            label("Title:")
            textField()
                .text(task.title)
                .enabled(isEditable)
                .resizableColumn()
        }
        row {
            label("Created:")
            val timestamp = Instant.ofEpochMilli(task.createdAt)
                .atZone(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            label(formatter.format(timestamp))
        }
        row {
            label("Description:")
            textArea()
                .text(task.description)
                .enabled(isEditable)
                .rows(3)
                .resizableColumn()
        }
        row {
            label("Status:")
            label(task.status.name)
        }
        if (task.completedAt != null) {
            row {
                label("Completed:")
                val timestamp = Instant.ofEpochMilli(task.completedAt!!)
                    .atZone(ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                label(formatter.format(timestamp))
            }
        }
    }

    fun getContent(): JPanel = panel
}
