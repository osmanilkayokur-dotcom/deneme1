package dev.kok.app.domain.model

import java.time.Instant
import java.time.LocalDate

data class MuscleGroup(
    val id: Long,
    val code: String,
    val displayName: String,
    val meshNodeId: String,
    val region: BodyRegion,
)

data class MuscleTarget(val muscle: MuscleGroup, val role: MuscleRole)

data class Exercise(
    val id: Long,
    val name: String,
    val muscles: List<MuscleTarget>,
)

data class PlannedExercise(
    val id: Long,
    val exercise: Exercise,
    val orderIndex: Int,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetWeightKg: Double?,
    val notes: String?,
)

data class WorkoutDay(
    val id: Long,
    val programId: Long,
    val dayOrder: Int,
    val name: String,
    val exercises: List<PlannedExercise>,
)

data class WorkoutProgram(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val source: EntrySource,
    val createdAt: Instant,
    val days: List<WorkoutDay>,
)

/** Program oluşturma/güncelleme girdisi (manuel arayüz ve Gemini ortak kullanır). */
data class ProgramDraft(
    val name: String,
    val source: EntrySource,
    val days: List<DayDraft>,
)

data class DayDraft(
    val name: String,
    val exercises: List<ExerciseDraft>,
)

data class ExerciseDraft(
    val name: String,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val weightKg: Double? = null,
    val notes: String? = null,
    /** Katalogda olmayan yeni egzersizler için kas eşleşmeleri (muscle code → rol). */
    val muscles: Map<String, MuscleRole> = emptyMap(),
)

data class SetEntry(val exerciseId: Long, val setIndex: Int, val reps: Int, val weightKg: Double)

data class WorkoutSession(
    val id: Long,
    val date: LocalDate,
    val workoutDayId: Long?,
    val startedAt: Instant,
    val durationMin: Int,
    val burnedKcal: Int,
    val notes: String?,
    val sets: List<SetEntry>,
)
