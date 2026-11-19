package dev.kok.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.kok.app.domain.model.BodyRegion
import dev.kok.app.domain.model.EntrySource
import dev.kok.app.domain.model.MuscleRole
import java.time.Instant

@Entity(
    tableName = "workout_program",
    indices = [Index(value = ["is_active"])],
)
data class WorkoutProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    val source: EntrySource,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

@Entity(
    tableName = "workout_day",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["program_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["program_id", "day_order"], unique = true)],
)
data class WorkoutDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "program_id") val programId: Long,
    @ColumnInfo(name = "day_order") val dayOrder: Int,
    val name: String,
)

@Entity(
    tableName = "exercise",
    indices = [Index(value = ["name"], unique = true)],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
)

@Entity(
    tableName = "muscle_group",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["mesh_node_id"], unique = true),
    ],
)
data class MuscleGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "mesh_node_id") val meshNodeId: String,
    @ColumnInfo(name = "body_region") val bodyRegion: BodyRegion,
)

@Entity(
    tableName = "exercise_muscle_cross_ref",
    primaryKeys = ["exercise_id", "muscle_group_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MuscleGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["muscle_group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["muscle_group_id"])],
)
data class ExerciseMuscleCrossRef(
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "muscle_group_id") val muscleGroupId: Long,
    val role: MuscleRole,
)

@Entity(
    tableName = "workout_day_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_day_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["workout_day_id", "order_index"], unique = true),
        Index(value = ["exercise_id"]),
    ],
)
data class WorkoutDayExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "workout_day_id") val workoutDayId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "target_sets") val targetSets: Int,
    @ColumnInfo(name = "target_reps_min") val targetRepsMin: Int,
    @ColumnInfo(name = "target_reps_max") val targetRepsMax: Int,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double?,
    val notes: String?,
)

@Entity(
    tableName = "workout_session_log",
    foreignKeys = [
        ForeignKey(
            entity = DailyLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["daily_log_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_day_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["daily_log_id"]), Index(value = ["workout_day_id"])],
)
data class WorkoutSessionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "daily_log_id") val dailyLogId: Long,
    @ColumnInfo(name = "workout_day_id") val workoutDayId: Long?,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "duration_min") val durationMin: Int,
    @ColumnInfo(name = "burned_kcal") val burnedKcal: Int,
    val notes: String?,
)

@Entity(
    tableName = "workout_set_log",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["session_id", "exercise_id", "set_index"], unique = true),
        Index(value = ["exercise_id"]),
    ],
)
data class WorkoutSetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "set_index") val setIndex: Int,
    val reps: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
)
