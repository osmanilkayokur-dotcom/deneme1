package dev.kok.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "daily_log",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["active_workout_day_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["local_date"], unique = true),
        Index(value = ["active_workout_day_id"]),
    ],
)
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "local_date") val localDate: LocalDate,
    @ColumnInfo(name = "water_ml", defaultValue = "0") val waterMl: Int = 0,
    @ColumnInfo(name = "burned_kcal", defaultValue = "0") val burnedKcal: Int = 0,
    @ColumnInfo(name = "active_workout_day_id") val activeWorkoutDayId: Long? = null,
)
