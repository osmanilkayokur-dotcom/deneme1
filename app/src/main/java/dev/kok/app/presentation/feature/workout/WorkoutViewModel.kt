package dev.kok.app.presentation.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kok.app.core.TimeProvider
import dev.kok.app.domain.model.WorkoutProgram
import dev.kok.app.domain.repository.WorkoutRepository
import dev.kok.app.domain.usecase.LogWorkoutSessionUseCase
import dev.kok.app.domain.usecase.ObserveTrainingTodayUseCase
import dev.kok.app.domain.usecase.TrainingToday
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutUiState(
    val loading: Boolean = true,
    val today: TrainingToday? = null,
    val programs: List<WorkoutProgram> = emptyList(),
)

/** Antrenman yoğunluğu → MET (Compendium of Physical Activities, direnç egzersizi). */
enum class Intensity(val label: String, val met: Double) {
    LIGHT("Hafif", 3.5),
    MODERATE("Orta", 5.0),
    HARD("Ağır", 6.0),
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    observeToday: ObserveTrainingTodayUseCase,
    private val workouts: WorkoutRepository,
    private val logSession: LogWorkoutSessionUseCase,
    private val time: TimeProvider,
) : ViewModel() {

    val state: StateFlow<WorkoutUiState> = combine(observeToday(time.today()), workouts.observePrograms()) { today, programs ->
        WorkoutUiState(loading = false, today = today, programs = programs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun selectDay(dayId: Long) = viewModelScope.launch { workouts.selectWorkoutDay(time.today(), dayId) }

    fun activate(programId: Long) = viewModelScope.launch {
        workouts.activateProgram(programId)
        // Program değişince bugünkü gün seçimi sıfırlanır; sıradaki gün önerilir.
        workouts.selectWorkoutDay(time.today(), null)
    }

    fun delete(programId: Long) = viewModelScope.launch {
        runCatching { workouts.deleteProgram(programId) }
            .onSuccess { _messages.send("Program silindi") }
            .onFailure { _messages.send(it.message ?: "Silinemedi") }
    }

    fun logSession(durationMin: Int, intensity: Intensity, notes: String?) = viewModelScope.launch {
        val dayId = state.value.today?.selectedDay?.id
        logSession(time.today(), dayId, durationMin, intensity.met, notes)
            .onSuccess { kcal -> _messages.send("Antrenman kaydedildi · yaklaşık $kcal kcal") }
            .onFailure { _messages.send(it.message ?: "Kaydedilemedi") }
    }
}
