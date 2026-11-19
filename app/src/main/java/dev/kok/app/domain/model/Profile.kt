package dev.kok.app.domain.model

import java.time.Instant

data class UserProfile(
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val sex: Sex,
    val activityLevel: ActivityLevel,
    val goals: NutritionGoals,
    val updatedAt: Instant,
)

data class NutritionGoals(
    val waterMl: Int,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val fiberG: Double,
    val micros: MicroNutrients,
)

/** Temel mikro besinler; hem hedef hem tüketim değerleri için ortak tip. */
data class MicroNutrients(
    val sodiumMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
    val ironMg: Double = 0.0,
    val vitaminCMg: Double = 0.0,
    val vitaminDMcg: Double = 0.0,
) {
    operator fun plus(o: MicroNutrients) = MicroNutrients(
        sodiumMg + o.sodiumMg,
        potassiumMg + o.potassiumMg,
        ironMg + o.ironMg,
        vitaminCMg + o.vitaminCMg,
        vitaminDMcg + o.vitaminDMcg,
    )

    companion object {
        val ZERO = MicroNutrients()
    }
}
