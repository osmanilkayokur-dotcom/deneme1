package dev.kok.app.data.repository

import androidx.room.withTransaction
import dev.kok.app.core.TimeProvider
import dev.kok.app.data.local.KokDatabase
import dev.kok.app.data.local.entity.ExerciseEntity
import dev.kok.app.data.local.entity.ExerciseMuscleCrossRef
import dev.kok.app.data.local.entity.WorkoutDayEntity
import dev.kok.app.data.local.entity.WorkoutDayExerciseEntity
import dev.kok.app.data.local.entity.WorkoutProgramEntity
import dev.kok.app.data.local.entity.WorkoutSessionLogEntity
import dev.kok.app.data.local.entity.WorkoutSetLogEntity
import dev.kok.app.data.mapper.toDomain
import dev.kok.app.domain.model.Exercise
import dev.kok.app.domain.model.MuscleGroup
import dev.kok.app.domain.model.ProgramDraft
import dev.kok.app.domain.model.SetEntry
import dev.kok.app.domain.model.WorkoutDay
import dev.kok.app.domain.model.WorkoutProgram
import dev.kok.app.domain.model.WorkoutSession
import dev.kok.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val db: KokDatabase,
    private val time: TimeProvider,
) : WorkoutRepository {
    private val dao = db.workoutDao()
    private val logs = db.dailyLogDao()

    override fun observePrograms(): Flow<List<WorkoutProgram>> =
        dao.observePrograms().map { list -> list.map { it.toDomain() } }

    override fun observeActiveProgram(): Flow<WorkoutProgram?> =
        dao.observeActiveProgram().map { it?.toDomain() }

    override fun observeExercises(): Flow<List<Exercise>> =
        dao.observeExercises().map { list -> list.map { it.toDomain() } }

    override fun observeMuscleGroups(): Flow<List<MuscleGroup>> =
        dao.observeMuscleGroups().map { list -> list.map { it.toDomain() } }

    override suspend fun getExercises(): List<Exercise> = dao.getExercises().map { it.toDomain() }

    override suspend fun getMuscleGroups(): List<MuscleGroup> = dao.getMuscleGroups().map { it.toDomain() }

    override suspend fun getProgram(id: Long): WorkoutProgram? = dao.getProgram(id)?.toDomain()

    override suspend fun createOrUpdateProgram(draft: ProgramDraft, activate: Boolean): Long = db.withTransaction {
        require(draft.name.isNotBlank()) { "Program adı boş olamaz" }
        require(draft.days.isNotEmpty()) { "Program en az bir gün içermeli" }

        val existing = dao.findProgramByName(draft.name.trim())
        val programId = if (existing != null) {
            dao.deleteDaysOfProgram(existing.id)
            existing.id
        } else {
            dao.insertProgram(
                WorkoutProgramEntity(
                    name = draft.name.trim(),
                    isActive = false,
                    source = draft.source,
                    createdAt = time.now(),
                ),
            )
        }
        val muscleIds = dao.getMuscleGroups().associate { it.code to it.id }

        draft.days.forEachIndexed { dayIndex, dayDraft ->
            val dayId = dao.insertDay(
                WorkoutDayEntity(programId = programId, dayOrder = dayIndex + 1, name = dayDraft.name.trim()),
            )
            val planned = dayDraft.exercises.mapIndexed { exIndex, ex ->
                val exerciseId = resolveExercise(ex.name.trim(), ex.muscles, muscleIds)
                WorkoutDayExerciseEntity(
                    workoutDayId = dayId,
                    exerciseId = exerciseId,
                    orderIndex = exIndex,
                    targetSets = ex.sets.coerceAtLeast(1),
                    targetRepsMin = ex.repsMin.coerceAtLeast(1),
                    targetRepsMax = ex.repsMax.coerceAtLeast(ex.repsMin.coerceAtLeast(1)),
                    targetWeightKg = ex.weightKg,
                    notes = ex.notes?.takeIf { it.isNotBlank() },
                )
            }
            if (planned.isNotEmpty()) dao.insertDayExercises(planned)
        }

        if (activate || dao.programCount() == 1) dao.activateOnly(programId)
        programId
    }

    /** Katalogda varsa mevcut egzersiz; yoksa verilen kas eşleşmeleriyle yeni egzersiz. */
    private suspend fun resolveExercise(
        name: String,
        muscles: Map<String, dev.kok.app.domain.model.MuscleRole>,
        muscleIds: Map<String, Long>,
    ): Long {
        dao.findExerciseByName(name)?.let { return it.id }
        val id = dao.insertExercise(ExerciseEntity(name = name))
        val refs = muscles.mapNotNull { (code, role) ->
            muscleIds[code.uppercase()]?.let { ExerciseMuscleCrossRef(id, it, role) }
        }
        if (refs.isNotEmpty()) dao.insertCrossRefs(refs)
        return id
    }

    override suspend fun renameProgram(id: Long, name: String) {
        require(name.isNotBlank()) { "Program adı boş olamaz" }
        dao.renameProgram(id, name.trim())
    }

    override suspend fun deleteProgram(id: Long) = db.withTransaction {
        val wasActive = dao.getProgram(id)?.program?.isActive == true
        dao.deleteProgram(id)
        if (wasActive) {
            // Silinen aktif programın yerine en yeni programı aktifleştir.
            dao.getNewestProgramId()?.let { dao.activateOnly(it) }
        }
    }

    override suspend fun activateProgram(id: Long) = dao.activateOnly(id)

    override fun observeSelectedDayId(date: LocalDate): Flow<Long?> = logs.observeActiveWorkoutDayId(date)

    override suspend fun selectWorkoutDay(date: LocalDate, workoutDayId: Long?) = db.withTransaction {
        logs.setActiveWorkoutDay(logs.ensure(date), workoutDayId)
    }

    override fun observeWorkoutDay(id: Long): Flow<WorkoutDay?> = dao.observeDay(id).map { it?.toDomain() }

    override suspend fun logSession(
        date: LocalDate,
        workoutDayId: Long?,
        startedAt: Instant,
        durationMin: Int,
        burnedKcal: Int,
        notes: String?,
        sets: List<SetEntry>,
    ): Long = db.withTransaction {
        val logId = logs.ensure(date)
        val sessionId = dao.insertSession(
            WorkoutSessionLogEntity(
                dailyLogId = logId,
                workoutDayId = workoutDayId,
                startedAt = startedAt,
                durationMin = durationMin.coerceAtLeast(0),
                burnedKcal = burnedKcal.coerceAtLeast(0),
                notes = notes?.takeIf { it.isNotBlank() },
            ),
        )
        if (sets.isNotEmpty()) {
            dao.insertSets(sets.map { WorkoutSetLogEntity(0, sessionId, it.exerciseId, it.setIndex, it.reps, it.weightKg) })
        }
        logs.addBurned(logId, burnedKcal.coerceAtLeast(0))
        sessionId
    }

    override fun observeSessions(start: LocalDate, end: LocalDate): Flow<List<WorkoutSession>> =
        combine(dao.observeSessions(start, end), logs.observeRange(start, end)) { sessions, days ->
            val dateById = days.associate { it.log.id to it.log.localDate }
            sessions.mapNotNull { s -> dateById[s.session.dailyLogId]?.let { s.toDomain(it) } }
        }
}
