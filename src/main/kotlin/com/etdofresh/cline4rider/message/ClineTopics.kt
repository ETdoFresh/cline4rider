package com.etdofresh.cline4rider.message

import com.intellij.util.messages.Topic
import com.etdofresh.cline4rider.model.ClineMessage

object ClineTopics {
    val CLINE_MESSAGES = Topic.create(
        "Cline Messages",
        ClineMessageListener::class.java
    )
}

interface ClineMessageListener {
    fun onMessageReceived(message: ClineMessage)
}
