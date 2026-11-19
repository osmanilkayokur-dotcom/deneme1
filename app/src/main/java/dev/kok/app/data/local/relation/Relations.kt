package dev.kok.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import dev.kok.app.data.local.entity.DailyLogEntity
import dev.kok.app.data.local.entity.ExerciseEntity
import dev.kok.app.data.local.entity.ExerciseMuscleCrossRef
import dev.kok.app.data.local.entity.MealEntryEntity
import dev.kok.app.data.local.entity.MuscleGroupEntity
import dev.kok.app.data.local.entity.WorkoutDayEntity
import dev.kok.app.data.local.entity.WorkoutDayExerciseEntity
import dev.kok.app.data.local.entity.WorkoutProgramEntity
import dev.kok.app.data.local.entity.WorkoutSessionLogEntity
import dev.kok.app.data.local.entity.WorkoutSetLogEntity

data class DailyLogWithMeals(
    @Embedded val log: DailyLogEntity,
    @Relation(parentColumn = "id", entityColumn = "daily_log_id")
    val meals: List<MealEntryEntity>,
)

/** Rol bilgisini kaybetmemek için cross-ref üzerinden kas grubuna gidilir. */
data class CrossRefWithMuscle(
    @Embedded val ref: ExerciseMuscleCrossRef,
    @Relation(parentColumn = "muscle_group_id", entityColumn = "id")
    val muscle: MuscleGroupEntity,
)

data class ExerciseWithMuscles(
    @Embedded val exercise: ExerciseEntity,
    @Relation(
        entity = ExerciseMuscleCrossRef::class,
        parentColumn = "id",
        entityColumn = "exercise_id",
    )
    val muscles: List<CrossRefWithMuscle>,
)

data class PlannedExerciseWithDetails(
    @Embedded val planned: WorkoutDayExerciseEntity,
    @Relation(
        entity = ExerciseEntity::class,
        parentColumn = "exercise_id",
        entityColumn = "id",
    )
    val exercise: ExerciseWithMuscles,
)

data class DayWithExercises(
    @Embedded val day: WorkoutDayEntity,
    @Relation(
        entity = WorkoutDayExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workout_day_id",
    )
    val exercises: List<PlannedExerciseWithDetails>,
)

data class ProgramWithDays(
    @Embedded val program: WorkoutProgramEntity,
    @Relation(
        entity = WorkoutDayEntity::class,
        parentColumn = "id",
        entityColumn = "program_id",
    )
    val days: List<DayWithExercises>,
)

data class SessionWithSets(
    @Embedded val session: WorkoutSessionLogEntity,
    @Relation(parentColumn = "id", entityColumn = "session_id")
    val sets: List<WorkoutSetLogEntity>,
)
