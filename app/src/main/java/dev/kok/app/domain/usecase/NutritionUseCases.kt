package dev.kok.app.domain.usecase

import dev.kok.app.domain.engine.MetabolismCalculator
import dev.kok.app.domain.model.DaySummary
import dev.kok.app.domain.model.Meal
import dev.kok.app.domain.model.NewMeal
import dev.kok.app.domain.model.UserProfile
import dev.kok.app.domain.repository.NutritionRepository
import dev.kok.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** Ana ekranın tek veri kaynağı: profil + günün kayıtları + metabolizma değerleri. */
data class DailyOverview(
    val profile: UserProfile?,
    val day: DaySummary,
    val bmr: Double?,
    val tdee: Double?,
)

class ObserveDailyOverviewUseCase @Inject constructor(
    private val profiles: ProfileRepository,
    private val nutrition: NutritionRepository,
    private val calculator: MetabolismCalculator,
) {
    operator fun invoke(date: LocalDate): Flow<DailyOverview> =
        combine(profiles.observeProfile(), nutrition.observeDay(date)) { profile, day ->
            val bmr = profile?.let { calculator.bmr(it.weightKg, it.heightCm, it.age, it.sex) }
            DailyOverview(
                profile = profile,
                day = day,
                bmr = bmr,
                tdee = bmr?.let { calculator.tdee(it, profile.activityLevel) },
            )
        }
}

sealed interface MealValidation {
    data object Valid : MealValidation
    data class Invalid(val reason: String) : MealValidation
}

class AddMealsUseCase @Inject constructor(private val nutrition: NutritionRepository) {

    fun validate(meal: NewMeal): MealValidation = when {
        meal.name.isBlank() -> MealValidation.Invalid("Yemek adı boş olamaz")
        meal.grams <= 0 -> MealValidation.Invalid("Gramaj sıfırdan büyük olmalı")
        meal.nutrients.kcal < 0 || meal.nutrients.proteinG < 0 || meal.nutrients.carbG < 0 ||
            meal.nutrients.fatG < 0 || meal.nutrients.fiberG < 0 -> MealValidation.Invalid("Besin değerleri negatif olamaz")
        meal.nutrients.proteinG + meal.nutrients.carbG + meal.nutrients.fatG + meal.nutrients.fiberG > meal.grams * 1.05 ->
            MealValidation.Invalid("Makroların toplamı gramajı aşıyor")
        else -> MealValidation.Valid
    }

    /** Hepsi geçerliyse tek transaction'da kaydeder ve id'leri döner (geri alma için). */
    suspend operator fun invoke(meals: List<NewMeal>): Result<List<Long>> {
        val invalid = meals.map(::validate).filterIsInstance<MealValidation.Invalid>().firstOrNull()
        if (invalid != null) return Result.failure(IllegalArgumentException(invalid.reason))
        return runCatching { nutrition.addMeals(meals) }
    }
}

class DeleteMealUseCase @Inject constructor(private val nutrition: NutritionRepository) {
    suspend operator fun invoke(ids: List<Long>) = nutrition.deleteMeals(ids)

    /** Silinen öğünü (geri al) yeniden ekler. */
    suspend fun restore(meal: Meal): Long = nutrition.addMeal(
        NewMeal(meal.date, meal.name, meal.grams, meal.nutrients, meal.source, meal.category),
    )
}

class AddWaterUseCase @Inject constructor(private val nutrition: NutritionRepository) {
    suspend operator fun invoke(date: LocalDate, deltaMl: Int) {
        require(deltaMl != 0 && kotlin.math.abs(deltaMl) <= MAX_SINGLE_ENTRY_ML) { "Geçersiz su miktarı" }
        nutrition.addWater(date, deltaMl)
    }

    companion object {
        const val MAX_SINGLE_ENTRY_ML = 3000
    }
}
