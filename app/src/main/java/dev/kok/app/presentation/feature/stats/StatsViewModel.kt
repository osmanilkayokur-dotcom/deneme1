package dev.kok.app.presentation.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kok.app.domain.repository.ProfileRepository
import dev.kok.app.domain.usecase.ObservePeriodStatsUseCase
import dev.kok.app.domain.usecase.PeriodStats
import dev.kok.app.domain.usecase.StatsPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(val period: StatsPeriod, val stats: PeriodStats?, val kcalGoal: Double?, val proteinGoal: Double?)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    observeStats: ObservePeriodStatsUseCase,
    profiles: ProfileRepository,
) : ViewModel() {
    private val period = MutableStateFlow(StatsPeriod.THIS_WEEK)
    val selected: StateFlow<StatsPeriod> = period.asStateFlow()

    val state: StateFlow<StatsUiState> = combine(
        period.flatMapLatest { p -> observeStats(p).map { p to it } },
        profiles.observeProfile(),
    ) { (p, stats), profile ->
        val goals = profile?.goals
        val kcal = goals?.let { it.proteinG * 4 + it.carbG * 4 + it.fatG * 9 }
        StatsUiState(p, stats, kcal, goals?.proteinG)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState(StatsPeriod.THIS_WEEK, null, null, null))

    fun select(p: StatsPeriod) { period.value = p }
}
