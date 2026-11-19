package dev.kok.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.kok.app.data.local.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

@Dao
interface BodyMeasurementDao {
    @Query("SELECT * FROM body_measurement ORDER BY year_month DESC")
    fun observeAll(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurement WHERE year_month = :yearMonth")
    suspend fun getByMonth(yearMonth: YearMonth): BodyMeasurementEntity?

    /** UNIQUE(year_month) ihlali SQLiteConstraintException fırlatır — aylık tek kayıt kuralı. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: BodyMeasurementEntity): Long

    @Update
    suspend fun update(entity: BodyMeasurementEntity)
}
