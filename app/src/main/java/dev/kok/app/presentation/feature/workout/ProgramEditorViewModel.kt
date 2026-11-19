package dev.kok.app.presentation.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kok.app.domain.model.DayDraft
import dev.kok.app.domain.model.EntrySource
import dev.kok.app.domain.model.Exercise
import dev.kok.app.domain.model.ExerciseDraft
import dev.kok.app.domain.model.MuscleGroup
import dev.kok.app.domain.model.MuscleRole
import dev.kok.app.domain.model.ProgramDraft
import dev.kok.app.domain.repository.WorkoutRepository
import dev.kok.app.presentation.feature.profile.clean
import dev.kok.app.presentation.feature.profile.toDecimal
import dev.kok.app.presentation.navigation.ProgramEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditExercise(
    val key: Long,
    val name: String,
    val sets: String = "3",
    val repsMin: String = "8",
    val repsMax: String = "12",
    val weightKg: String = "",
    val muscles: Map<String, MuscleRole> = emptyMap(),
)

data class EditDay(val key: Long, val name: String, val exercises: List<EditExercise> = emptyList())

data class ProgramEditorState(
    val programId: Long? = null,
    val originalName: String? = null,
    val name: String = "",
    val days: List<EditDay> = emptyList(),
    val catalog: List<Exercise> = emptyList(),
    val muscles: List<MuscleGroup> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class ProgramEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workouts: WorkoutRepository,
) : ViewModel() {

    private val programId = savedStateHandle.toRoute<ProgramEditorRoute>().programId.takeIf { it > 0 }
    private var nextKey = 1L
    private fun key() = nextKey++

    private val _state = MutableStateFlow(ProgramEditorState(programId = programId))
    val state: StateFlow<ProgramEditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val catalog = workouts.getExercises()
            val muscles = workouts.getMuscleGroups()
            val program = programId?.let { workouts.getProgram(it) }
            _state.update { s ->
                if (program == null) {
                    s.copy(loading = false, catalog = catalog, muscles = muscles, days = listOf(EditDay(key(), "Gün 1")))
                } else {
                    s.copy(
                        loading = false,
                        catalog = catalog,
                        muscles = muscles,
                        originalName = program.name,
                        name = program.name,
                        days = program.days.map { d ->
                            EditDay(
                                key(),
                                d.name,
                                d.exercises.map { pe ->
                                    EditExercise(
                                        key = key(),
                                        name = pe.exercise.name,
                                        sets = pe.targetSets.toString(),
                                        repsMin = pe.targetRepsMin.toString(),
                                        repsMax = pe.targetRepsMax.toString(),
                                        weightKg = pe.targetWeightKg?.clean().orEmpty(),
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v.take(60), error = null) }

    fun addDay() = _state.update { it.copy(days = it.days + EditDay(key(), "Gün ${it.days.size + 1}")) }

    fun removeDay(dayKey: Long) = _state.update { it.copy(days = it.days.filterNot { d -> d.key == dayKey }) }

    fun renameDay(dayKey: Long, name: String) = updateDay(dayKey) { it.copy(name = name.take(40)) }

    fun addExercise(dayKey: Long, name: String, muscles: Map<String, MuscleRole>) =
        updateDay(dayKey) { it.copy(exercises = it.exercises + EditExercise(key(), name.trim(), muscles = muscles)) }

    fun removeExercise(dayKey: Long, exKey: Long) =
        updateDay(dayKey) { d -> d.copy(exercises = d.exercises.filterNot { it.key == exKey }) }

    fun updateExercise(dayKey: Long, exKey: Long, transform: (EditExercise) -> EditExercise) =
        updateDay(dayKey) { d -> d.copy(exercises = d.exercises.map { if (it.key == exKey) transform(it) else it }) }

    private fun updateDay(dayKey: Long, transform: (EditDay) -> EditDay) =
        _state.update { s -> s.copy(days = s.days.map { if (it.key == dayKey) transform(it) else it }, error = null) }

    fun save() {
        val s = _state.value
        val name = s.name.trim()
        val error = when {
            name.isBlank() -> "Program adı gir"
            s.days.isEmpty() -> "En az bir gün ekle"
            s.days.any { it.name.isBlank() } -> "Gün adlarını boş bırakma"
            s.days.any { it.exercises.isEmpty() } -> "Her güne en az bir egzersiz ekle"
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        val draft = ProgramDraft(
            name = name,
            source = EntrySource.MANUAL,
            days = s.days.map { d ->
                DayDraft(
                    d.name.trim(),
                    d.exercises.map { e ->
                        val min = e.repsMin.toIntOrNull()?.coerceIn(1, 100) ?: 8
                        ExerciseDraft(
                            name = e.name,
                            sets = e.sets.toIntOrNull()?.coerceIn(1, 20) ?: 3,
                            repsMin = min,
                            repsMax = (e.repsMax.toIntOrNull() ?: min).coerceIn(min, 100),
                            weightKg = e.weightKg.toDecimal(),
                            muscles = e.muscles,
                        )
                    },
                )
            },
        )
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching {
                val existingWithName = workouts.getProgramIdByName(name)
                if (existingWithName != null && existingWithName != programId) error("Bu adda başka bir program var")
                if (programId != null && s.originalName != null && !s.originalName.equals(name, ignoreCase = true)) {
                    workouts.renameProgram(programId, name)
                }
                workouts.createOrUpdateProgram(draft, activate = programId == null)
            }.onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message) } }
        }
    }
}
