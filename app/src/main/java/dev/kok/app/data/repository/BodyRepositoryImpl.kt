package dev.kok.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import dev.kok.app.data.local.KokDatabase
import dev.kok.app.data.mapper.toDomain
import dev.kok.app.data.mapper.toEntity
import dev.kok.app.domain.model.BodyMeasurement
import dev.kok.app.domain.repository.BodyRepository
import dev.kok.app.domain.repository.MeasurementSaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

class BodyRepositoryImpl @Inject constructor(private val db: KokDatabase) : BodyRepository {
    private val dao = db.bodyMeasurementDao()

    override fun observeAll(): Flow<List<BodyMeasurement>> = dao.observeAll().map { l -> l.map { it.toDomain() } }

    override suspend fun getForMonth(yearMonth: YearMonth): BodyMeasurement? = dao.getByMonth(yearMonth)?.toDomain()

    override suspend fun insert(measurement: BodyMeasurement): MeasurementSaveResult = try {
        db.withTransaction {
            val id = dao.insert(measurement.toEntity().copy(id = 0))
            db.profileDao().updateWeight(measurement.weightKg, System.currentTimeMillis())
            MeasurementSaveResult.Saved(id)
        }
    } catch (e: SQLiteConstraintException) {
        // UNIQUE(year_month): bu ay zaten ölçüm girilmiş.
        MeasurementSaveResult.MonthAlreadyRecorded
    }

    override suspend fun update(measurement: BodyMeasurement) = db.withTransaction {
        val existing = requireNotNull(dao.getByMonth(measurement.yearMonth)) { "Bu aya ait kayıt yok" }
        require(existing.id == measurement.id) { "Yalnızca mevcut ayın kaydı düzenlenebilir" }
        dao.update(measurement.toEntity())
        db.profileDao().updateWeight(measurement.weightKg, System.currentTimeMillis())
    }
}
