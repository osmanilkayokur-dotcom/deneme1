package dev.kok.app.data.mapper

import dev.kok.app.data.local.entity.BodyMeasurementEntity
import dev.kok.app.data.local.entity.ChatMessageEntity
import dev.kok.app.data.local.entity.MealEntryEntity
import dev.kok.app.data.local.entity.MuscleGroupEntity
import dev.kok.app.data.local.entity.UserProfileEntity
import dev.kok.app.data.local.relation.DailyLogWithMeals
import dev.kok.app.data.local.relation.DayWithExercises
import dev.kok.app.data.local.relation.ExerciseWithMuscles
import dev.kok.app.data.local.relation.ProgramWithDays
import dev.kok.app.data.local.relation.SessionWithSets
import dev.kok.app.domain.model.BodyMeasurement
import dev.kok.app.domain.model.ChatMessage
import dev.kok.app.domain.model.DaySummary
import dev.kok.app.domain.model.Exercise
import dev.kok.app.domain.model.Meal
import dev.kok.app.domain.model.MicroNutrients
import dev.kok.app.domain.model.MuscleGroup
import dev.kok.app.domain.model.MuscleTarget
import dev.kok.app.domain.model.NutritionGoals
import dev.kok.app.domain.model.Nutrients
import dev.kok.app.domain.model.PlannedExercise
import dev.kok.app.domain.model.SetEntry
import dev.kok.app.domain.model.UserProfile
import dev.kok.app.domain.model.WorkoutDay
import dev.kok.app.domain.model.WorkoutProgram
import dev.kok.app.domain.model.WorkoutSession
import java.time.LocalDate

fun UserProfileEntity.toDomain() = UserProfile(
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    sex = sex,
    activityLevel = activityLevel,
    goals = NutritionGoals(
        waterMl = waterGoalMl,
        proteinG = proteinGoalG,
        carbG = carbGoalG,
        fatG = fatGoalG,
        fiberG = fiberGoalG,
        micros = MicroNutrients(sodiumGoalMg, potassiumGoalMg, ironGoalMg, vitaminCGoalMg, vitaminDGoalMcg),
    ),
    updatedAt = updatedAt,
)

fun UserProfile.toEntity() = UserProfileEntity(
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    sex = sex,
    activityLevel = activityLevel,
    waterGoalMl = goals.waterMl,
    proteinGoalG = goals.proteinG,
    carbGoalG = goals.carbG,
    fatGoalG = goals.fatG,
    fiberGoalG = goals.fiberG,
    sodiumGoalMg = goals.micros.sodiumMg,
    potassiumGoalMg = goals.micros.potassiumMg,
    ironGoalMg = goals.micros.ironMg,
    vitaminCGoalMg = goals.micros.vitaminCMg,
    vitaminDGoalMcg = goals.micros.vitaminDMcg,
    updatedAt = updatedAt,
)

fun MealEntryEntity.toDomain(date: LocalDate) = Meal(
    id = id,
    date = date,
    name = name,
    grams = grams,
    nutrients = Nutrients(
        kcal = kcal,
        proteinG = proteinG,
        carbG = carbG,
        fatG = fatG,
        fiberG = fiberG,
        micros = MicroNutrients(sodiumMg, potassiumMg, ironMg, vitaminCMg, vitaminDMcg),
    ),
    source = source,
    category = animationCategory,
    createdAt = createdAt,
)

fun DailyLogWithMeals.toDomain() = DaySummary(
    date = log.localDate,
    waterMl = log.waterMl,
    burnedKcal = log.burnedKcal,
    meals = meals.sortedBy { it.createdAt }.map { it.toDomain(log.localDate) },
)

fun MuscleGroupEntity.toDomain() = MuscleGroup(id, code, displayName, meshNodeId, bodyRegion)

fun ExerciseWithMuscles.toDomain() = Exercise(
    id = exercise.id,
    name = exercise.name,
    muscles = muscles.map { MuscleTarget(it.muscle.toDomain(), it.ref.role) },
)

fun DayWithExercises.toDomain() = WorkoutDay(
    id = day.id,
    programId = day.programId,
    dayOrder = day.dayOrder,
    name = day.name,
    exercises = exercises.sortedBy { it.planned.orderIndex }.map {
        PlannedExercise(
            id = it.planned.id,
            exercise = it.exercise.toDomain(),
            orderIndex = it.planned.orderIndex,
            targetSets = it.planned.targetSets,
            targetRepsMin = it.planned.targetRepsMin,
            targetRepsMax = it.planned.targetRepsMax,
            targetWeightKg = it.planned.targetWeightKg,
            notes = it.planned.notes,
        )
    },
)

fun ProgramWithDays.toDomain() = WorkoutProgram(
    id = program.id,
    name = program.name,
    isActive = program.isActive,
    source = program.source,
    createdAt = program.createdAt,
    days = days.sortedBy { it.day.dayOrder }.map { it.toDomain() },
)

fun SessionWithSets.toDomain(date: LocalDate) = WorkoutSession(
    id = session.id,
    date = date,
    workoutDayId = session.workoutDayId,
    startedAt = session.startedAt,
    durationMin = session.durationMin,
    burnedKcal = session.burnedKcal,
    notes = session.notes,
    sets = sets.sortedWith(compareBy({ it.exerciseId }, { it.setIndex }))
        .map { SetEntry(it.exerciseId, it.setIndex, it.reps, it.weightKg) },
)

fun BodyMeasurementEntity.toDomain() = BodyMeasurement(
    id, yearMonth, measuredOn, weightKg, bodyFatPercent, chestCm, waistCm, armCm, legCm, shoulderCm, neckCm,
)

fun BodyMeasurement.toEntity() = BodyMeasurementEntity(
    id, yearMonth, measuredOn, weightKg, bodyFatPercent, chestCm, waistCm, armCm, legCm, shoulderCm, neckCm,
)

fun ChatMessageEntity.toDomain() = ChatMessage(id, role, text, createdAt, toolName, toolPayloadJson, actionSummary, undoRef)

fun ChatMessage.toEntity() = ChatMessageEntity(id, role, text, createdAt, toolName, toolPayloadJson, actionSummary, undoRef)
