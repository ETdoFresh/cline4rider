package com.rooveterinary.cline4rider.util

import com.intellij.openapi.diagnostic.Logger

/**
 * Central logging utility for the Cline plugin
 */
object ClineLogger {
    private val LOG = Logger.getInstance("Cline4Rider")

    fun info(message: String) {
        LOG.info(message)
    }

    fun warn(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            LOG.warn(message, throwable)
        } else {
            LOG.warn(message)
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            LOG.error(message, throwable)
        } else {
            LOG.error(message)
        }
    }

    fun debug(message: String) {
        LOG.debug(message)
    }
}