package com.rooveterinary.cline4rider.ui.tabs

import javax.swing.JPanel

/**
 * Base interface for all tab panels in the Cline tool window.
 * Ensures consistent behavior and state management across tabs.
 */
interface TabPanel {
    /**
     * Called when the tab becomes active
     */
    fun onActivate()

    /**
     * Called when the tab becomes inactive
     */
    fun onDeactivate()

    /**
     * Get the panel component for this tab
     */
    fun getPanel(): JPanel
}

/**
 * Abstract base class for tab panels that provides default implementations
 */
abstract class BaseTabPanel : JPanel(), TabPanel {
    override fun onActivate() {
        // Default implementation
    }

    override fun onDeactivate() {
        // Default implementation
    }

    override fun getPanel(): JPanel = this
}