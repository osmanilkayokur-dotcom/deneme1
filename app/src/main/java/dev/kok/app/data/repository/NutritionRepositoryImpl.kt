package dev.kok.app.data.repository

import androidx.room.withTransaction
import dev.kok.app.core.TimeProvider
import dev.kok.app.data.local.KokDatabase
import dev.kok.app.data.local.entity.MealEntryEntity
import dev.kok.app.data.mapper.toDomain
import dev.kok.app.domain.model.DaySummary
import dev.kok.app.domain.model.FoodTemplate
import dev.kok.app.domain.model.Meal
import dev.kok.app.domain.model.NewMeal
import dev.kok.app.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class NutritionRepositoryImpl @Inject constructor(
    private val db: KokDatabase,
    private val time: TimeProvider,
) : NutritionRepository {
    private val logs = db.dailyLogDao()
    private val meals = db.mealDao()

    override fun observeDay(date: LocalDate): Flow<DaySummary> =
        logs.observeDay(date).map { it?.toDomain() ?: DaySummary(date, 0, 0, emptyList()) }

    override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DaySummary>> =
        logs.observeRange(start, end).map { list -> list.map { it.toDomain() } }

    override suspend fun getRange(start: LocalDate, end: LocalDate): List<DaySummary> =
        logs.getRange(start, end).map { it.toDomain() }

    override suspend fun addMeal(meal: NewMeal): Long = addMeals(listOf(meal)).single()

    override suspend fun addMeals(meals: List<NewMeal>): List<Long> = db.withTransaction {
        val now = time.now()
        val entities = meals.map { meal ->
            val logId = logs.ensure(meal.date)
            meal.toEntity(logId, now)
        }
        this.meals.insertAll(entities)
    }

    override suspend fun updateMeal(meal: Meal) = db.withTransaction {
        val existing = requireNotNull(meals.getById(meal.id)) { "Öğün bulunamadı: ${meal.id}" }
        meals.update(
            NewMeal(meal.date, meal.name, meal.grams, meal.nutrients, meal.source, meal.category)
                .toEntity(existing.dailyLogId, existing.createdAt)
                .copy(id = meal.id),
        )
    }

    override suspend fun deleteMeals(ids: List<Long>) = meals.deleteAll(ids)

    override suspend fun addWater(date: LocalDate, deltaMl: Int) = db.withTransaction {
        logs.addWater(logs.ensure(date), deltaMl)
    }

    override suspend fun addBurnedCalories(date: LocalDate, deltaKcal: Int) = db.withTransaction {
        logs.addBurned(logs.ensure(date), deltaKcal)
    }

    override suspend fun recentFoods(limit: Int): List<FoodTemplate> =
        meals.recentDistinct(limit).map { e ->
            val m = e.toDomain(LocalDate.EPOCH)
            FoodTemplate(m.name, m.grams, m.nutrients, m.category)
        }

    private fun NewMeal.toEntity(logId: Long, createdAt: java.time.Instant) = MealEntryEntity(
        dailyLogId = logId,
        name = name.trim(),
        grams = grams,
        kcal = nutrients.kcal,
        proteinG = nutrients.proteinG,
        carbG = nutrients.carbG,
        fatG = nutrients.fatG,
        fiberG = nutrients.fiberG,
        sodiumMg = nutrients.micros.sodiumMg,
        potassiumMg = nutrients.micros.potassiumMg,
        ironMg = nutrients.micros.ironMg,
        vitaminCMg = nutrients.micros.vitaminCMg,
        vitaminDMcg = nutrients.micros.vitaminDMcg,
        source = source,
        animationCategory = category,
        createdAt = createdAt,
    )
}
