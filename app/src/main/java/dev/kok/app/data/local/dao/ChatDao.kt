package dev.kok.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.kok.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_message ORDER BY created_at, id")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM (SELECT * FROM chat_message ORDER BY created_at DESC, id DESC LIMIT :limit) ORDER BY created_at, id")
    suspend fun recent(limit: Int): List<ChatMessageEntity>

    @Insert
    suspend fun insert(entity: ChatMessageEntity): Long

    @Query("UPDATE chat_message SET action_summary = :summary, undo_ref = :undoRef WHERE id = :id")
    suspend fun updateAction(id: Long, summary: String?, undoRef: String?)

    @Query("DELETE FROM chat_message")
    suspend fun clear()
}
