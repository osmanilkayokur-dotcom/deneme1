package dev.kok.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.kok.app.data.local.entity.DailyLogEntity
import dev.kok.app.data.local.relation.DailyLogWithMeals
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
abstract class DailyLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(entity: DailyLogEntity): Long

    @Query("SELECT id FROM daily_log WHERE local_date = :date")
    abstract suspend fun idForDate(date: LocalDate): Long?

    /** O tarihin kaydını garanti eder ve id'sini döner (yoksa oluşturur). */
    @Transaction
    open suspend fun ensure(date: LocalDate): Long =
        idForDate(date) ?: insertIgnore(DailyLogEntity(localDate = date)).let { inserted ->
            if (inserted > 0) inserted else requireNotNull(idForDate(date))
        }

    @Query("SELECT * FROM daily_log WHERE local_date = :date")
    abstract suspend fun getByDate(date: LocalDate): DailyLogEntity?

    @Transaction
    @Query("SELECT * FROM daily_log WHERE local_date = :date")
    abstract fun observeDay(date: LocalDate): Flow<DailyLogWithMeals?>

    @Transaction
    @Query("SELECT * FROM daily_log WHERE local_date BETWEEN :start AND :end ORDER BY local_date")
    abstract fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DailyLogWithMeals>>

    @Transaction
    @Query("SELECT * FROM daily_log WHERE local_date BETWEEN :start AND :end ORDER BY local_date")
    abstract suspend fun getRange(start: LocalDate, end: LocalDate): List<DailyLogWithMeals>

    /** Su miktarı negatif olamaz; geri alma (eksi değer) sıfırda durur. */
    @Query("UPDATE daily_log SET water_ml = MAX(0, water_ml + :deltaMl) WHERE id = :logId")
    abstract suspend fun addWater(logId: Long, deltaMl: Int)

    @Query("UPDATE daily_log SET burned_kcal = MAX(0, burned_kcal + :deltaKcal) WHERE id = :logId")
    abstract suspend fun addBurned(logId: Long, deltaKcal: Int)

    @Query("UPDATE daily_log SET active_workout_day_id = :workoutDayId WHERE id = :logId")
    abstract suspend fun setActiveWorkoutDay(logId: Long, workoutDayId: Long?)

    @Query("SELECT active_workout_day_id FROM daily_log WHERE local_date = :date")
    abstract fun observeActiveWorkoutDayId(date: LocalDate): Flow<Long?>
}
