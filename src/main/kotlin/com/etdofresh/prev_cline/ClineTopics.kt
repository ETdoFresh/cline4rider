package com.cline

import com.intellij.util.messages.Topic
import com.cline.model.ClineMessage

object ClineTopics {
    val CLINE_MESSAGES = Topic.create(
        "Cline Messages",
        ClineMessageListener::class.java
    )
}

interface ClineMessageListener {
    fun onMessageReceived(message: ClineMessage)
}
