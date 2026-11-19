package dev.kok.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.kok.app.data.local.dao.BodyMeasurementDao
import dev.kok.app.data.local.dao.ChatDao
import dev.kok.app.data.local.dao.DailyLogDao
import dev.kok.app.data.local.dao.MealDao
import dev.kok.app.data.local.dao.ProfileDao
import dev.kok.app.data.local.dao.WorkoutDao
import dev.kok.app.data.local.entity.BodyMeasurementEntity
import dev.kok.app.data.local.entity.ChatMessageEntity
import dev.kok.app.data.local.entity.DailyLogEntity
import dev.kok.app.data.local.entity.ExerciseEntity
import dev.kok.app.data.local.entity.ExerciseMuscleCrossRef
import dev.kok.app.data.local.entity.MealEntryEntity
import dev.kok.app.data.local.entity.MuscleGroupEntity
import dev.kok.app.data.local.entity.UserProfileEntity
import dev.kok.app.data.local.entity.WorkoutDayEntity
import dev.kok.app.data.local.entity.WorkoutDayExerciseEntity
import dev.kok.app.data.local.entity.WorkoutProgramEntity
import dev.kok.app.data.local.entity.WorkoutSessionLogEntity
import dev.kok.app.data.local.entity.WorkoutSetLogEntity

/**
 * Migration stratejisi: şema her değiştiğinde [version] artırılır ve
 * [KokMigrations.ALL] listesine açık bir Migration eklenir. Yıkıcı migration kullanılmaz.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        DailyLogEntity::class,
        MealEntryEntity::class,
        WorkoutProgramEntity::class,
        WorkoutDayEntity::class,
        ExerciseEntity::class,
        MuscleGroupEntity::class,
        ExerciseMuscleCrossRef::class,
        WorkoutDayExerciseEntity::class,
        WorkoutSessionLogEntity::class,
        WorkoutSetLogEntity::class,
        BodyMeasurementEntity::class,
        ChatMessageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KokDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun mealDao(): MealDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun chatDao(): ChatDao

    companion object {
        const val NAME = "kok.db"
    }
}

object KokMigrations {
    val ALL: Array<androidx.room.migration.Migration> = emptyArray()
}
