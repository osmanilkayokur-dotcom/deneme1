package dev.kok.app.domain.usecase

import dev.kok.app.core.TimeProvider
import dev.kok.app.domain.engine.StatsMetaphorMapper
import dev.kok.app.domain.model.DaySummary
import dev.kok.app.domain.repository.NutritionRepository
import dev.kok.app.domain.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class StatsPeriod { THIS_WEEK, LAST_WEEK, THIS_MONTH, LAST_MONTH }

data class DailyPoint(val date: LocalDate, val kcal: Double, val proteinG: Double, val waterMl: Int)

data class PeriodStats(
    val period: StatsPeriod,
    val start: LocalDate,
    val end: LocalDate,
    val loggedDays: Int,
    val totalFoodKg: Double,
    val totalWaterL: Double,
    val totalBurnedKcal: Double,
    val avgKcal: Double,
    val avgProteinG: Double,
    val points: List<DailyPoint>,
    val metaphors: List<StatsMetaphorMapper.Metaphor>,
)

class ObservePeriodStatsUseCase @Inject constructor(
    private val nutrition: NutritionRepository,
    private val profiles: ProfileRepository,
    private val mapper: StatsMetaphorMapper,
    private val time: TimeProvider,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(period: StatsPeriod): Flow<PeriodStats> {
        val (start, end) = range(period, time.today())
        return combine(nutrition.observeRange(start, end), profiles.observeProfile()) { days, profile ->
            compute(period, start, end, days, profile?.weightKg)
        }
    }

    fun compute(period: StatsPeriod, start: LocalDate, end: LocalDate, days: List<DaySummary>, weightKg: Double?): PeriodStats {
        val logged = days.filter { it.meals.isNotEmpty() || it.waterMl > 0 || it.burnedKcal > 0 }
        val mealDays = days.filter { it.meals.isNotEmpty() }
        val foodKg = days.sumOf { it.totalFoodGrams } / 1000.0
        val waterL = days.sumOf { it.waterMl } / 1000.0
        val burned = days.sumOf { it.burnedKcal }.toDouble()
        val mapperPeriod = if (period == StatsPeriod.THIS_WEEK || period == StatsPeriod.LAST_WEEK) {
            StatsMetaphorMapper.Period.WEEK
        } else {
            StatsMetaphorMapper.Period.MONTH
        }
        val metaphors = buildList {
            if (foodKg > 0) add(mapper.foodWeight(foodKg, mapperPeriod))
            if (waterL > 0) add(mapper.water(waterL, mapperPeriod))
            if (burned > 0 && weightKg != null) add(mapper.burnedEnergy(burned, weightKg, mapperPeriod))
        }
        val byDate = days.associateBy { it.date }
        val points = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.map { d ->
            val day = byDate[d]
            DailyPoint(d, day?.totals?.kcal ?: 0.0, day?.totals?.proteinG ?: 0.0, day?.waterMl ?: 0)
        }.toList()
        return PeriodStats(
            period = period,
            start = start,
            end = end,
            loggedDays = logged.size,
            totalFoodKg = foodKg,
            totalWaterL = waterL,
            totalBurnedKcal = burned,
            avgKcal = if (mealDays.isEmpty()) 0.0 else mealDays.sumOf { it.totals.kcal } / mealDays.size,
            avgProteinG = if (mealDays.isEmpty()) 0.0 else mealDays.sumOf { it.totals.proteinG } / mealDays.size,
            points = points,
            metaphors = metaphors,
        )
    }

    companion object {
        /** Haftalar Pazartesi başlar (Türkiye). Bu hafta/ay için bitiş tarihi bugündür. */
        fun range(period: StatsPeriod, today: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
            StatsPeriod.THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to today
            StatsPeriod.LAST_WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
                start to start.plusDays(6)
            }
            StatsPeriod.THIS_MONTH -> today.withDayOfMonth(1) to today
            StatsPeriod.LAST_MONTH -> {
                val m = YearMonth.from(today).minusMonths(1)
                m.atDay(1) to m.atEndOfMonth()
            }
        }
    }
}
