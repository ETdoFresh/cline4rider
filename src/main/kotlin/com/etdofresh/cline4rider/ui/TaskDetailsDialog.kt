package com.etdofresh.cline4rider.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Dimension
import javax.swing.*

class TaskDetailsDialog(
    project: Project,
    private val content: String,
    private val completion: String?,
    private val status: String,
    private val tokens: String?,
    private val timestamp: String
) : DialogWrapper(project) {

    init {
        title = "Task Details"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10)).apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()

            // Task request
            add(JPanel(BorderLayout()).apply {
                background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                add(JLabel("Request:").apply {
                    font = JBUI.Fonts.create("Segoe UI", 12)
                    foreground = JBUI.CurrentTheme.Label.foreground()
                }, BorderLayout.NORTH)
                add(JBScrollPane(JTextArea(content).apply {
                    isEditable = false
                    lineWrap = true
                    wrapStyleWord = true
                    font = JBUI.Fonts.create("JetBrains Mono", 12)
                    background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                    foreground = JBUI.CurrentTheme.Label.foreground()
                    rows = 5
                    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                }).apply {
                    border = BorderFactory.createEmptyBorder()
                    viewport.background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                }, BorderLayout.CENTER)
            }, BorderLayout.NORTH)

            // Response (if available)
            completion?.let { response ->
                add(JPanel(BorderLayout()).apply {
                    background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                    add(JLabel("Response:").apply {
                        font = JBUI.Fonts.create("Segoe UI", 12)
                        foreground = JBUI.CurrentTheme.Label.foreground()
                    }, BorderLayout.NORTH)
                    add(JBScrollPane(JTextArea(response).apply {
                        isEditable = false
                        lineWrap = true
                        wrapStyleWord = true
                        font = JBUI.Fonts.create("JetBrains Mono", 12)
                        background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                        foreground = JBUI.CurrentTheme.Label.foreground()
                        rows = 10
                        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                    }).apply {
                        border = BorderFactory.createEmptyBorder()
                        viewport.background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                    }, BorderLayout.CENTER)
                }, BorderLayout.CENTER)
            }

            // Metadata
            add(JPanel(BorderLayout()).apply {
                background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                    background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
                    add(JLabel("Status: ").apply {
                        font = JBUI.Fonts.create("Segoe UI", 12)
                        foreground = JBUI.CurrentTheme.Label.foreground()
                    })
                    add(JLabel(status.replaceFirstChar { it.uppercase() }).apply {
                        font = JBUI.Fonts.create("Segoe UI", 12)
                        foreground = when (status) {
                            "completed" -> java.awt.Color(76, 175, 80)
                            "error" -> java.awt.Color(255, 82, 82)
                            else -> JBUI.CurrentTheme.Label.foreground()
                        }
                    })
                    tokens?.let {
                        add(JLabel(" | Tokens: $it").apply {
                            font = JBUI.Fonts.create("Segoe UI", 12)
                            foreground = JBUI.CurrentTheme.Label.foreground()
                        })
                    }
                    add(JLabel(" | Time: $timestamp").apply {
                        font = JBUI.Fonts.create("Segoe UI", 12)
                        foreground = JBUI.CurrentTheme.Label.foreground()
                    })
                }, BorderLayout.SOUTH)
            }, BorderLayout.SOUTH)
        }

        return JBScrollPane(panel).apply {
            preferredSize = Dimension(600, 400)
            border = BorderFactory.createEmptyBorder()
        }
    }
}
