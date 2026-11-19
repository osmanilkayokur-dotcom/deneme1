package dev.kok.app.domain.model

import java.time.LocalDate
import java.time.YearMonth

data class BodyMeasurement(
    val id: Long,
    val yearMonth: YearMonth,
    val measuredOn: LocalDate,
    val weightKg: Double,
    val bodyFatPercent: Double?,
    val chestCm: Double?,
    val waistCm: Double?,
    val armCm: Double?,
    val legCm: Double?,
    val shoulderCm: Double?,
    val neckCm: Double?,
)
