package dev.kok.app.core

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/** Test edilebilir zaman kaynağı; tüm "bugün" hesapları buradan geçer. */
@Singleton
class TimeProvider @Inject constructor(private val clock: Clock) {
    fun now(): Instant = clock.instant()
    fun today(): LocalDate = LocalDate.now(clock)
    fun currentMonth(): YearMonth = YearMonth.now(clock)
}
