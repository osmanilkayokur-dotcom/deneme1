package dev.kok.app.domain.model

import java.time.Instant
import java.time.LocalDate

data class Meal(
    val id: Long,
    val date: LocalDate,
    val name: String,
    val grams: Double,
    val nutrients: Nutrients,
    val source: EntrySource,
    val category: FoodCategory,
    val createdAt: Instant,
)

/** Bir öğünün besin değerleri (gramaja göre hesaplanmış mutlak değerler). */
data class Nutrients(
    val kcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val fiberG: Double,
    val micros: MicroNutrients,
) {
    operator fun plus(o: Nutrients) = Nutrients(
        kcal + o.kcal,
        proteinG + o.proteinG,
        carbG + o.carbG,
        fatG + o.fatG,
        fiberG + o.fiberG,
        micros + o.micros,
    )

    companion object {
        val ZERO = Nutrients(0.0, 0.0, 0.0, 0.0, 0.0, MicroNutrients.ZERO)
    }
}

/** Yeni öğün kaydı için girdi. */
data class NewMeal(
    val date: LocalDate,
    val name: String,
    val grams: Double,
    val nutrients: Nutrients,
    val source: EntrySource,
    val category: FoodCategory,
)

data class DaySummary(
    val date: LocalDate,
    val waterMl: Int,
    val burnedKcal: Int,
    val meals: List<Meal>,
) {
    val totals: Nutrients get() = meals.fold(Nutrients.ZERO) { acc, m -> acc + m.nutrients }
    val totalFoodGrams: Double get() = meals.sumOf { it.grams }
}

/** Daha önce girilmiş bir yemeğin tekrar kullanılabilir şablonu (hızlı ekleme için). */
data class FoodTemplate(
    val name: String,
    val grams: Double,
    val nutrients: Nutrients,
    val category: FoodCategory,
)
