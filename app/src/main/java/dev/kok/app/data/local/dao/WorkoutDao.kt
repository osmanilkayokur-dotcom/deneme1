package dev.kok.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.kok.app.data.local.entity.ExerciseEntity
import dev.kok.app.data.local.entity.ExerciseMuscleCrossRef
import dev.kok.app.data.local.entity.MuscleGroupEntity
import dev.kok.app.data.local.entity.WorkoutDayEntity
import dev.kok.app.data.local.entity.WorkoutDayExerciseEntity
import dev.kok.app.data.local.entity.WorkoutProgramEntity
import dev.kok.app.data.local.entity.WorkoutSessionLogEntity
import dev.kok.app.data.local.entity.WorkoutSetLogEntity
import dev.kok.app.data.local.relation.DayWithExercises
import dev.kok.app.data.local.relation.ExerciseWithMuscles
import dev.kok.app.data.local.relation.ProgramWithDays
import dev.kok.app.data.local.relation.SessionWithSets
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WorkoutDao {
    // ---- Program ----
    @Transaction
    @Query("SELECT * FROM workout_program ORDER BY is_active DESC, created_at DESC")
    fun observePrograms(): Flow<List<ProgramWithDays>>

    @Transaction
    @Query("SELECT * FROM workout_program WHERE is_active = 1 LIMIT 1")
    fun observeActiveProgram(): Flow<ProgramWithDays?>

    @Transaction
    @Query("SELECT * FROM workout_program WHERE id = :id")
    suspend fun getProgram(id: Long): ProgramWithDays?

    @Query("SELECT * FROM workout_program WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findProgramByName(name: String): WorkoutProgramEntity?

    @Insert
    suspend fun insertProgram(entity: WorkoutProgramEntity): Long

    @Query("UPDATE workout_program SET name = :name WHERE id = :id")
    suspend fun renameProgram(id: Long, name: String)

    @Query("DELETE FROM workout_program WHERE id = :id")
    suspend fun deleteProgram(id: Long)

    @Query("UPDATE workout_program SET is_active = (id = :id)")
    suspend fun activateOnly(id: Long)

    @Query("SELECT id FROM workout_program ORDER BY created_at DESC LIMIT 1")
    suspend fun getNewestProgramId(): Long?

    @Query("SELECT COUNT(*) FROM workout_program")
    suspend fun programCount(): Int

    // ---- Gün ----
    @Insert
    suspend fun insertDay(entity: WorkoutDayEntity): Long

    @Query("DELETE FROM workout_day WHERE program_id = :programId")
    suspend fun deleteDaysOfProgram(programId: Long)

    @Transaction
    @Query("SELECT * FROM workout_day WHERE id = :id")
    fun observeDay(id: Long): Flow<DayWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_day WHERE id = :id")
    suspend fun getDay(id: Long): DayWithExercises?

    @Insert
    suspend fun insertDayExercises(entities: List<WorkoutDayExerciseEntity>)

    // ---- Egzersiz & Kas ----
    @Query("SELECT * FROM exercise WHERE name = :name LIMIT 1")
    suspend fun findExerciseByName(name: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercise(entity: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(refs: List<ExerciseMuscleCrossRef>)

    @Transaction
    @Query("SELECT * FROM exercise ORDER BY name")
    fun observeExercises(): Flow<List<ExerciseWithMuscles>>

    @Transaction
    @Query("SELECT * FROM exercise ORDER BY name")
    suspend fun getExercises(): List<ExerciseWithMuscles>

    @Query("SELECT * FROM muscle_group ORDER BY id")
    fun observeMuscleGroups(): Flow<List<MuscleGroupEntity>>

    @Query("SELECT * FROM muscle_group ORDER BY id")
    suspend fun getMuscleGroups(): List<MuscleGroupEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMuscleGroups(entities: List<MuscleGroupEntity>): List<Long>

    // ---- Antrenman kayıtları ----
    @Insert
    suspend fun insertSession(entity: WorkoutSessionLogEntity): Long

    @Insert
    suspend fun insertSets(entities: List<WorkoutSetLogEntity>)

    @Query("DELETE FROM workout_session_log WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Transaction
    @Query(
        """
        SELECT s.* FROM workout_session_log s
        JOIN daily_log d ON d.id = s.daily_log_id
        WHERE d.local_date BETWEEN :start AND :end
        ORDER BY s.started_at DESC
        """,
    )
    fun observeSessions(start: LocalDate, end: LocalDate): Flow<List<SessionWithSets>>
}
