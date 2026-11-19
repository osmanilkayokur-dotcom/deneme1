package dev.kok.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.kok.app.domain.model.ChatRole
import java.time.Instant

@Entity(
    tableName = "chat_message",
    indices = [Index(value = ["created_at"])],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: ChatRole,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "tool_name") val toolName: String?,
    @ColumnInfo(name = "tool_payload_json") val toolPayloadJson: String?,
    @ColumnInfo(name = "action_summary") val actionSummary: String?,
    @ColumnInfo(name = "undo_ref") val undoRef: String?,
)
