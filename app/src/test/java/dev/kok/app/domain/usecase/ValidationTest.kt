package dev.kok.app.domain.usecase

import dev.kok.app.core.TimeProvider
import dev.kok.app.domain.model.BodyMeasurement
import dev.kok.app.domain.model.EntrySource
import dev.kok.app.domain.model.FoodCategory
import dev.kok.app.domain.model.FoodTemplate
import dev.kok.app.domain.model.Meal
import dev.kok.app.domain.model.MicroNutrients
import dev.kok.app.domain.model.NewMeal
import dev.kok.app.domain.model.Nutrients
import dev.kok.app.domain.model.DaySummary
import dev.kok.app.domain.repository.BodyRepository
import dev.kok.app.domain.repository.MeasurementSaveResult
import dev.kok.app.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ValidationTest {
    private val time = TimeProvider(Clock.fixed(Instant.parse("2026-11-20T09:00:00Z"), ZoneId.of("Europe/Istanbul")))

    private val noopNutrition = object : NutritionRepository {
        override fun observeDay(date: LocalDate): Flow<DaySummary> = emptyFlow()
        override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DaySummary>> = emptyFlow()
        override suspend fun getRange(start: LocalDate, end: LocalDate) = emptyList<DaySummary>()
        override suspend fun addMeal(meal: NewMeal) = 1L
        override suspend fun addMeals(meals: List<NewMeal>) = meals.indices.map { it.toLong() + 1 }
        override suspend fun updateMeal(meal: Meal) = Unit
        override suspend fun deleteMeals(ids: List<Long>) = Unit
        override suspend fun addWater(date: LocalDate, deltaMl: Int) = Unit
        override suspend fun addBurnedCalories(date: LocalDate, deltaKcal: Int) = Unit
        override suspend fun recentFoods(limit: Int) = emptyList<FoodTemplate>()
    }

    private fun meal(grams: Double, p: Double, c: Double, f: Double, name: String = "Yulaf") = NewMeal(
        LocalDate.of(2026, 11, 20), name, grams,
        Nutrients(p * 4 + c * 4 + f * 9, p, c, f, 0.0, MicroNutrients.ZERO), EntrySource.MANUAL, FoodCategory.GRAIN_RICE,
    )

    @Test
    fun `makro toplami gramaji asamaz`() {
        val uc = AddMealsUseCase(noopNutrition)
        assertTrue(uc.validate(meal(100.0, 10.0, 60.0, 7.0)) is MealValidation.Valid)
        assertTrue(uc.validate(meal(50.0, 30.0, 30.0, 10.0)) is MealValidation.Invalid)
        assertTrue(uc.validate(meal(100.0, 1.0, 1.0, 1.0, name = " ")) is MealValidation.Invalid)
        assertTrue(uc.validate(meal(0.0, 0.0, 0.0, 0.0)) is MealValidation.Invalid)
    }

    @Test
    fun `gecersiz ogun varsa hicbiri kaydedilmez`() = runTest {
        val uc = AddMealsUseCase(noopNutrition)
        val r = uc(listOf(meal(100.0, 10.0, 20.0, 5.0), meal(10.0, 50.0, 0.0, 0.0)))
        assertTrue(r.isFailure)
    }

    private class FakeBody : BodyRepository {
        val store = mutableMapOf<YearMonth, BodyMeasurement>()
        override fun observeAll(): Flow<List<BodyMeasurement>> = emptyFlow()
        override suspend fun getForMonth(yearMonth: YearMonth) = store[yearMonth]
        override suspend fun insert(measurement: BodyMeasurement): MeasurementSaveResult {
            if (store.containsKey(measurement.yearMonth)) return MeasurementSaveResult.MonthAlreadyRecorded
            store[measurement.yearMonth] = measurement.copy(id = store.size + 1L)
            return MeasurementSaveResult.Saved(store.size.toLong())
        }
        override suspend fun update(measurement: BodyMeasurement) { store[measurement.yearMonth] = measurement }
    }

    private fun input(w: Double, fat: Double? = null) = MeasurementInput(w, fat, null, 85.0, null, null, null, null)

    @Test
    fun `ayni ay ikinci kayit yeni satir acmaz guncelleme olur`() = runTest {
        val body = FakeBody()
        val uc = SaveMeasurementUseCase(body, time)
        uc(input(80.0)).getOrThrow()
        uc(input(79.5)).getOrThrow()
        assertEquals(1, body.store.size)
        assertEquals(79.5, body.store.getValue(YearMonth.of(2026, 11)).weightKg, 0.001)
    }

    @Test
    fun `olcum dogrulamasi`() {
        val uc = SaveMeasurementUseCase(FakeBody(), time)
        assertNull(uc.validate(input(80.0, 15.0)))
        assertNotNull(uc.validate(input(20.0)))
        assertNotNull(uc.validate(input(80.0, 80.0)))
    }
}
