package dev.kok.app.presentation.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kok.app.core.TimeProvider
import dev.kok.app.domain.model.Meal
import dev.kok.app.domain.usecase.AddWaterUseCase
import dev.kok.app.domain.usecase.DailyOverview
import dev.kok.app.domain.usecase.DeleteMealUseCase
import dev.kok.app.domain.usecase.ObserveDailyOverviewUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(val date: LocalDate, val isToday: Boolean, val overview: DailyOverview) : DashboardUiState
}

sealed interface DashboardEvent {
    data class MealDeleted(val meal: Meal) : DashboardEvent
    data class WaterAdded(val deltaMl: Int) : DashboardEvent
    data class Error(val message: String) : DashboardEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeOverview: ObserveDailyOverviewUseCase,
    private val addWater: AddWaterUseCase,
    private val deleteMeal: DeleteMealUseCase,
    private val time: TimeProvider,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(time.today())
    val date: StateFlow<LocalDate> = selectedDate.asStateFlow()

    val state: StateFlow<DashboardUiState> = selectedDate
        .flatMapLatest { date ->
            observeOverview(date).map { DashboardUiState.Ready(date, date == time.today(), it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun previousDay() = selectedDate.update { it.minusDays(1) }

    fun nextDay() = selectedDate.update { d -> if (d < time.today()) d.plusDays(1) else d }

    fun goToToday() { selectedDate.value = time.today() }

    fun addWater(deltaMl: Int) = viewModelScope.launch {
        runCatching { addWater(selectedDate.value, deltaMl) }
            .onSuccess { _events.send(DashboardEvent.WaterAdded(deltaMl)) }
            .onFailure { _events.send(DashboardEvent.Error(it.message ?: "Su eklenemedi")) }
    }

    fun delete(meal: Meal) = viewModelScope.launch {
        runCatching { deleteMeal(listOf(meal.id)) }
            .onSuccess { _events.send(DashboardEvent.MealDeleted(meal)) }
            .onFailure { _events.send(DashboardEvent.Error(it.message ?: "Silinemedi")) }
    }

    fun undoDelete(meal: Meal) = viewModelScope.launch {
        runCatching { deleteMeal.restore(meal) }
            .onFailure { _events.send(DashboardEvent.Error(it.message ?: "Geri alınamadı")) }
    }
}
