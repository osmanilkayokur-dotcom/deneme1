package dev.kok.app.domain.usecase

import dev.kok.app.core.TimeProvider
import dev.kok.app.domain.engine.MetabolismCalculator
import dev.kok.app.domain.model.MuscleGroup
import dev.kok.app.domain.model.MuscleRole
import dev.kok.app.domain.model.SetEntry
import dev.kok.app.domain.model.WorkoutDay
import dev.kok.app.domain.model.WorkoutProgram
import dev.kok.app.domain.repository.ProfileRepository
import dev.kok.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** Kas kodu → o gün çalışma yoğunluğu (birincil, ikincil). Listede olmayanlar dinlenir. */
data class MuscleHighlight(val byCode: Map<String, MuscleRole>) {
    fun roleOf(code: String): MuscleRole? = byCode[code]

    companion object {
        val NONE = MuscleHighlight(emptyMap())

        /** Bir kas birden fazla egzersizde geçiyorsa en yüksek rol (PRIMARY) kazanır. */
        fun of(day: WorkoutDay?): MuscleHighlight {
            if (day == null) return NONE
            val map = mutableMapOf<String, MuscleRole>()
            day.exercises.flatMap { it.exercise.muscles }.forEach { t ->
                val current = map[t.muscle.code]
                if (current == null || (current == MuscleRole.SECONDARY && t.role == MuscleRole.PRIMARY)) {
                    map[t.muscle.code] = t.role
                }
            }
            return MuscleHighlight(map)
        }
    }
}

data class TrainingToday(
    val program: WorkoutProgram?,
    val selectedDay: WorkoutDay?,
    /** Kullanıcı bugün için gün seçmediyse sıradaki gün önerilir. */
    val isSuggested: Boolean,
    val highlight: MuscleHighlight,
    val muscles: List<MuscleGroup>,
)

class ObserveTrainingTodayUseCase @Inject constructor(
    private val workouts: WorkoutRepository,
) {
    operator fun invoke(date: LocalDate): Flow<TrainingToday> = combine(
        workouts.observeActiveProgram(),
        workouts.observeSelectedDayId(date),
        workouts.observeLastTrainedDayId(),
        workouts.observeMuscleGroups(),
    ) { program, selectedId, lastId, muscles ->
        val days = program?.days.orEmpty()
        val selected = days.firstOrNull { it.id == selectedId }
        val day = selected ?: suggestNext(days, lastId)
        TrainingToday(
            program = program,
            selectedDay = day,
            isSuggested = selected == null && day != null,
            highlight = MuscleHighlight.of(day),
            muscles = muscles,
        )
    }

    companion object {
        /** Son antrenman yapılan günden sonraki gün (döngüsel); hiç kayıt yoksa ilk gün. */
        fun suggestNext(days: List<WorkoutDay>, lastTrainedDayId: Long?): WorkoutDay? {
            if (days.isEmpty()) return null
            val idx = days.indexOfFirst { it.id == lastTrainedDayId }
            return if (idx < 0) days.first() else days[(idx + 1) % days.size]
        }
    }
}

class LogWorkoutSessionUseCase @Inject constructor(
    private val workouts: WorkoutRepository,
    private val profiles: ProfileRepository,
    private val calculator: MetabolismCalculator,
    private val time: TimeProvider,
) {
    /** Süre ve yoğunluğa göre MET tabanlı kalori tahmini yapıp oturumu kaydeder; tahmini kaloriyi döner. */
    suspend operator fun invoke(
        date: LocalDate,
        workoutDayId: Long?,
        durationMin: Int,
        intensityMet: Double,
        notes: String?,
        sets: List<SetEntry> = emptyList(),
    ): Result<Int> = runCatching {
        require(durationMin in 1..600) { "Süre 1–600 dakika arasında olmalı" }
        val weight = profiles.getProfile()?.weightKg ?: error("Önce profil oluşturmalısın")
        val kcal = calculator.workoutKcal(weight, durationMin, intensityMet)
        workouts.logSession(date, workoutDayId, time.now(), durationMin, kcal, notes, sets)
        workouts.selectWorkoutDay(date, workoutDayId)
        kcal
    }
}
