package dev.kok.app.data.repository

import dev.kok.app.core.TimeProvider
import dev.kok.app.data.local.dao.ProfileDao
import dev.kok.app.data.mapper.toDomain
import dev.kok.app.data.mapper.toEntity
import dev.kok.app.domain.model.UserProfile
import dev.kok.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val dao: ProfileDao,
    private val time: TimeProvider,
) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = dao.observe().map { it?.toDomain() }
    override suspend fun getProfile(): UserProfile? = dao.get()?.toDomain()
    override suspend fun saveProfile(profile: UserProfile) =
        dao.upsert(profile.copy(updatedAt = time.now()).toEntity())
    override suspend fun updateWeight(weightKg: Double) =
        dao.updateWeight(weightKg, time.now().toEpochMilli())
}
