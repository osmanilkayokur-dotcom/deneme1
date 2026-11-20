package dev.kok.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsUseCasesTest {
    private val thursday = LocalDate.of(2026, 11, 19)

    @Test
    fun `bu hafta pazartesi baslar`() {
        val (s, e) = ObservePeriodStatsUseCase.range(StatsPeriod.THIS_WEEK, thursday)
        assertEquals(LocalDate.of(2026, 11, 16), s)
        assertEquals(thursday, e)
    }

    @Test
    fun `gecen hafta tam yedi gun`() {
        val (s, e) = ObservePeriodStatsUseCase.range(StatsPeriod.LAST_WEEK, thursday)
        assertEquals(LocalDate.of(2026, 11, 9), s)
        assertEquals(LocalDate.of(2026, 11, 15), e)
    }

    @Test
    fun `gecen ay ocak ayinda yil degisir`() {
        val (s, e) = ObservePeriodStatsUseCase.range(StatsPeriod.LAST_MONTH, LocalDate.of(2027, 1, 5))
        assertEquals(LocalDate.of(2026, 12, 1), s)
        assertEquals(LocalDate.of(2026, 12, 31), e)
    }
}
