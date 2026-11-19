package dev.kok.app.domain.model

import java.time.Instant

data class ChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String,
    val createdAt: Instant,
    val toolName: String? = null,
    val toolPayloadJson: String? = null,
    val actionSummary: String? = null,
    val undoRef: String? = null,
)
