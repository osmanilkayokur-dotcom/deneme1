package dev.kok.app.data.repository

import dev.kok.app.data.local.dao.ChatDao
import dev.kok.app.data.mapper.toDomain
import dev.kok.app.data.mapper.toEntity
import dev.kok.app.domain.model.ChatMessage
import dev.kok.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(private val dao: ChatDao) : ChatRepository {
    override fun observeMessages(): Flow<List<ChatMessage>> = dao.observeAll().map { l -> l.map { it.toDomain() } }
    override suspend fun recent(limit: Int): List<ChatMessage> = dao.recent(limit).map { it.toDomain() }
    override suspend fun append(message: ChatMessage): Long = dao.insert(message.toEntity().copy(id = 0))
    override suspend fun updateAction(id: Long, summary: String?, undoRef: String?) = dao.updateAction(id, summary, undoRef)
    override suspend fun clear() = dao.clear()
}
