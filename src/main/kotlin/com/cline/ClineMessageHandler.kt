package com.cline

import com.intellij.openapi.diagnostic.Logger

class ClineMessageHandler : ClineMessageListener {
    private val logger = Logger.getInstance(ClineMessageHandler::class.java)

    override fun onMessageReceived(message: ClineMessage) {
        logger.info("Received Cline message: ${message.type}")
        // Message handling logic will be implemented in Phase 3
    }
}
