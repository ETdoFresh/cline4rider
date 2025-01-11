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
    private val apiTokensLabel = JLabel().apply {
        foreground = Color(180, 180, 180)
    }
    private val cachedTokensLabel = JLabel().apply {
        foreground = Color(100, 200, 100)  // Green color for savings
    }
    private val costLabel = JLabel().apply {
        foreground = Color(180, 180, 180)
    }
    private val responseTimeLabel = JLabel().apply {
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
        statsPanel.add(apiTokensLabel)
        statsPanel.add(Box.createHorizontalStrut(10))
        statsPanel.add(cachedTokensLabel)
        statsPanel.add(Box.createHorizontalStrut(10))
        statsPanel.add(costLabel)
        statsPanel.add(Box.createHorizontalStrut(10))
        statsPanel.add(responseTimeLabel)
        
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
        
        // Calculate response time for the latest assistant message
        val latestUserMessage = messages.lastOrNull { it.role == ClineMessage.Role.USER }
        val latestAssistantMessage = messages.lastOrNull { 
            it.role == ClineMessage.Role.ASSISTANT && 
            (latestUserMessage == null || it.timestamp > latestUserMessage.timestamp)
        }
        val responseTime = if (latestUserMessage != null && latestAssistantMessage != null) {
            (latestAssistantMessage.timestamp - latestUserMessage.timestamp) / 1000.0
        } else 0.0

        // Calculate prompt and completion tokens
        val promptTokens = messages.filter { it.role == ClineMessage.Role.USER }
            .sumOf { it.tokens ?: (it.content.length / 4) }
        val completionTokens = messages.filter { it.role == ClineMessage.Role.ASSISTANT }
            .sumOf { it.tokens ?: (it.content.length / 4) }
        
        // Calculate cached tokens and costs
        val cachedTokens = messages.sumOf { it.cachedTokens ?: 0 }
        val promptCost = messages.sumOf { if (it.role == ClineMessage.Role.USER) (it.cost ?: 0.0) else 0.0 }
        val completionCost = messages.sumOf { if (it.role == ClineMessage.Role.ASSISTANT) (it.cost ?: 0.0) else 0.0 }
        val cacheSavings = messages.sumOf { it.cacheDiscount ?: 0.0 }

        // Update labels with new format
        apiTokensLabel.text = "API Tokens: ↑$promptTokens ↓$completionTokens"
        
        if (cachedTokens > 0) {
            cachedTokensLabel.text = "Cached API Tokens: $cachedTokens (-$${String.format("%.4f", cacheSavings)})"
            cachedTokensLabel.isVisible = true
        } else {
            cachedTokensLabel.isVisible = false
        }
        
        costLabel.text = "Cost: ↑$${String.format("%.4f", promptCost)} ↓$${String.format("%.4f", completionCost)}"
        responseTimeLabel.text = "Response Time: ${String.format("%.2f", responseTime)}s"
    }
}
