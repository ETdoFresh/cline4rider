package com.rooveterinary.cline4rider.ui

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ClineToolWindowContentTest {
    @Test
    fun testInitialState() {
        val content = ClineToolWindowContent()
        assertEquals("Home", content.getCurrentTab(), "Initial tab should be Home")
    }

    @Test
    fun testTabSwitching() {
        val content = ClineToolWindowContent()
        
        content.showTab("Tasks")
        assertEquals("Tasks", content.getCurrentTab(), "Should switch to Tasks tab")
        
        content.showTab("History")
        assertEquals("History", content.getCurrentTab(), "Should switch to History tab")
        
        content.showTab("Home")
        assertEquals("Home", content.getCurrentTab(), "Should switch back to Home tab")
    }
}