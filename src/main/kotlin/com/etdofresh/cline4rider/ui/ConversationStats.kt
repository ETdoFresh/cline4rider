package com.etdofresh.cline4rider.ui

import com.etdofresh.cline4rider.model.ClineMessage
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class ConversationStats : JPanel(BorderLayout()) {
    private val initialRequestPanel = JPanel(BorderLayout())
    private val statsPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
    private val requestLabel = JLabel("Initial Request:").apply {
        foreground = Color(180, 180, 180)
        border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
    }
    private val requestTextArea = JTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        isEditable = false
        background = Color(40, 40, 40)
        foreground = Color(220, 220, 220)
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        rows = 2
    }
    private val collapseButton = JButton("▼").apply {
        background = Color(40, 40, 40)
        foreground = Color(220, 220, 220)
        border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
    }
    private val showMoreLink = JButton("Show More").apply {
        isContentAreaFilled = false
        isBorderPainted = false
        foreground = Color(100, 150, 255)
    }
    private val tokensLabel = JLabel().apply {
        foreground = Color(180, 180, 180)
    }
    private val costLabel = JLabel().apply {
        foreground = Color(180, 180, 180)
    }
    
    private var isCollapsed = false
    private var fullText = ""
    
    init {
        background = Color(40, 40, 40)
        preferredSize = Dimension(JBUI.scale(400), JBUI.scale(60))
        minimumSize = Dimension(JBUI.scale(300), JBUI.scale(40))
        border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color(60, 60, 60))
        
        // Setup initial request panel
        initialRequestPanel.background = Color(40, 40, 40)
        val headerPanel = JPanel(BorderLayout()).apply {
            background = Color(40, 40, 40)
            add(requestLabel, BorderLayout.WEST)
            add(collapseButton, BorderLayout.CENTER)
            add(showMoreLink, BorderLayout.EAST)
        }
        initialRequestPanel.add(headerPanel, BorderLayout.NORTH)
        initialRequestPanel.add(JScrollPane(requestTextArea).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }, BorderLayout.CENTER)
        
        // Setup stats panel
        statsPanel.background = Color(40, 40, 40)
        statsPanel.add(tokensLabel)
        statsPanel.add(Box.createHorizontalStrut(10))
        statsPanel.add(costLabel)
        
        // Add components
        add(initialRequestPanel, BorderLayout.CENTER)
        add(statsPanel, BorderLayout.SOUTH)
        
        // Setup listeners
        collapseButton.addActionListener {
            toggleCollapse()
        }
        
        showMoreLink.addActionListener {
            toggleCollapse()
        }
        
        // Initially hide the show more link
        showMoreLink.isVisible = false
    }
    
    private fun toggleCollapse() {
        isCollapsed = !isCollapsed
        collapseButton.text = if (isCollapsed) "▶" else "▼"
        updateRequestText()
    }
    
    private fun updateRequestText() {
        if (fullText.length > 100 && isCollapsed) {
            requestTextArea.text = fullText.take(100) + "..."
            showMoreLink.isVisible = true
            showMoreLink.text = "Show More"
            preferredSize = Dimension(width, JBUI.scale(60))
        } else {
            requestTextArea.text = fullText
            showMoreLink.isVisible = fullText.length > 100
            showMoreLink.text = if (isCollapsed) "Show More" else "Show Less"
            preferredSize = Dimension(width, if (isCollapsed) JBUI.scale(60) else JBUI.scale(80))
        }
        revalidate()
        repaint()
    }
    
    fun updateStats(messages: List<ClineMessage>) {
        // Find first user message
        val firstUserMessage = messages.firstOrNull { it.role == ClineMessage.Role.USER }
        
        // Update initial request
        if (firstUserMessage != null) {
            fullText = firstUserMessage.content
            updateRequestText()
        }
        
        // Calculate total tokens (assuming 4 chars per token as a rough estimate)
        val totalTokens = messages.sumOf { it.content.length } / 4
        tokensLabel.text = when {
            totalTokens >= 1_000_000 -> String.format("%.1fM tokens", totalTokens / 1_000_000.0)
            totalTokens >= 1_000 -> String.format("%.1fK tokens", totalTokens / 1_000.0)
            else -> "$totalTokens tokens"
        }
        
        // Calculate total cost
        val totalCost = messages.sumOf { it.cost ?: 0.0 }
        costLabel.text = String.format("$%.4f", totalCost)
    }
}
