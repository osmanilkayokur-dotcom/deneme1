package dev.kok.app.domain.usecase

import dev.kok.app.core.TimeProvider
import dev.kok.app.domain.model.BodyMeasurement
import dev.kok.app.domain.repository.BodyRepository
import dev.kok.app.domain.repository.MeasurementSaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Aylık ölçüm kilidi durumu. */
data class MeasurementSchedule(
    val currentMonth: YearMonth,
    val currentEntry: BodyMeasurement?,
    val history: List<BodyMeasurement>,
    /** Bir sonraki yeni kaydın açılacağı gün (sonraki ayın 1'i). */
    val nextOpenDate: LocalDate,
    val daysUntilNext: Long,
    /** Ay içindeki ilerleme 0..1 (ay döngüsü görseli için). */
    val monthProgress: Float,
) {
    val isLocked: Boolean get() = currentEntry != null
}

class ObserveMeasurementScheduleUseCase @Inject constructor(
    private val body: BodyRepository,
    private val time: TimeProvider,
) {
    operator fun invoke(): Flow<MeasurementSchedule> = body.observeAll().map { all ->
        val today = time.today()
        val month = YearMonth.from(today)
        val next = month.plusMonths(1).atDay(1)
        MeasurementSchedule(
            currentMonth = month,
            currentEntry = all.firstOrNull { it.yearMonth == month },
            history = all.sortedByDescending { it.yearMonth },
            nextOpenDate = next,
            daysUntilNext = ChronoUnit.DAYS.between(today, next),
            monthProgress = (today.dayOfMonth - 1).toFloat() / month.lengthOfMonth().coerceAtLeast(1),
        )
    }
}

data class MeasurementInput(
    val weightKg: Double,
    val bodyFatPercent: Double?,
    val chestCm: Double?,
    val waistCm: Double?,
    val armCm: Double?,
    val legCm: Double?,
    val shoulderCm: Double?,
    val neckCm: Double?,
)

class SaveMeasurementUseCase @Inject constructor(
    private val body: BodyRepository,
    private val time: TimeProvider,
) {
    fun validate(input: MeasurementInput): String? {
        if (input.weightKg !in 35.0..250.0) return "Kilo 35–250 kg arasında olmalı"
        if (input.bodyFatPercent != null && input.bodyFatPercent !in 3.0..60.0) return "Yağ oranı %3–60 arasında olmalı"
        val cms = listOfNotNull(input.chestCm, input.waistCm, input.armCm, input.legCm, input.shoulderCm, input.neckCm)
        if (cms.any { it !in 10.0..250.0 }) return "Çevre ölçüleri 10–250 cm arasında olmalı"
        return null
    }

    /**
     * Bu ay kayıt yoksa yeni kayıt ekler; varsa yalnızca bu ayın kaydını günceller.
     * Aynı ay içinde ikinci yeni kayıt veritabanındaki UNIQUE kısıtıyla da engellenir.
     */
    suspend operator fun invoke(input: MeasurementInput): Result<MeasurementSaveResult> = runCatching {
        validate(input)?.let { throw IllegalArgumentException(it) }
        val today = time.today()
        val month = YearMonth.from(today)
        val existing = body.getForMonth(month)
        val entity = BodyMeasurement(
            id = existing?.id ?: 0,
            yearMonth = month,
            measuredOn = existing?.measuredOn ?: today,
            weightKg = input.weightKg,
            bodyFatPercent = input.bodyFatPercent,
            chestCm = input.chestCm,
            waistCm = input.waistCm,
            armCm = input.armCm,
            legCm = input.legCm,
            shoulderCm = input.shoulderCm,
            neckCm = input.neckCm,
        )
        if (existing == null) {
            body.insert(entity)
        } else {
            body.update(entity)
            MeasurementSaveResult.Saved(existing.id)
        }
    }
}
