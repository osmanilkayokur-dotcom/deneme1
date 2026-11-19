package dev.kok.app.domain.engine

import dev.kok.app.domain.model.ActivityLevel
import dev.kok.app.domain.model.MicroNutrients
import dev.kok.app.domain.model.NutritionGoals
import dev.kok.app.domain.model.Sex
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Bazal metabolizma (Mifflin-St Jeor), günlük enerji ihtiyacı ve
 * profil tabanlı hedef önerileri. Saf hesaplama — yan etkisi yoktur.
 */
class MetabolismCalculator @Inject constructor() {

    /** Mifflin-St Jeor: 10·kg + 6.25·cm − 5·yaş + (erkek: +5, kadın: −161). */
    fun bmr(weightKg: Double, heightCm: Double, age: Int, sex: Sex): Double {
        require(weightKg > 0 && heightCm > 0 && age > 0) { "Kilo, boy ve yaş pozitif olmalı" }
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return base + if (sex == Sex.MALE) 5.0 else -161.0
    }

    /** Aktivite çarpanıyla (antrenman dahil) günlük toplam enerji ihtiyacı. */
    fun tdee(bmr: Double, activity: ActivityLevel): Double = bmr * activity.multiplier

    /**
     * MET tabanlı antrenman kalori tahmini: kcal = MET × kg × saat.
     * Ağırlık antrenmanı için yoğunluğa göre 3.5 (hafif) – 6.0 (ağır) MET.
     */
    fun workoutKcal(weightKg: Double, durationMin: Int, met: Double = RESISTANCE_MET): Int =
        (met * weightKg * durationMin / 60.0).roundToInt().coerceAtLeast(0)

    /**
     * Profil için önerilen hedefler:
     * - Protein 1.8 g/kg (düzenli antrenman yapan yetişkin için kas korumaya yönelik orta değer)
     * - Yağ: enerjinin %25'i
     * - Karbonhidrat: kalan enerji
     * - Lif: 14 g / 1000 kcal (en az 25 g)
     * - Su: 35 ml/kg (+ aktivite düzeyine göre ek)
     * - Mikro besinler: yetişkin RDA / üst sınır (sodyum)
     */
    fun recommendGoals(weightKg: Double, heightCm: Double, age: Int, sex: Sex, activity: ActivityLevel): NutritionGoals {
        val tdee = tdee(bmr(weightKg, heightCm, age, sex), activity)
        val proteinG = (weightKg * PROTEIN_G_PER_KG).roundTo(5)
        val fatG = (tdee * FAT_ENERGY_SHARE / KCAL_PER_G_FAT).roundTo(5)
        val carbKcal = (tdee - proteinG * KCAL_PER_G_PROTEIN - fatG * KCAL_PER_G_FAT).coerceAtLeast(0.0)
        val carbG = (carbKcal / KCAL_PER_G_CARB).roundTo(5)
        val fiberG = maxOf(25.0, tdee / 1000.0 * 14.0).roundTo(1)
        val activityWaterMl = when (activity) {
            ActivityLevel.SEDENTARY -> 0
            ActivityLevel.LIGHT -> 250
            ActivityLevel.MODERATE -> 500
            ActivityLevel.ACTIVE -> 750
            ActivityLevel.VERY_ACTIVE -> 1000
        }
        val waterMl = ((weightKg * 35.0).roundToInt() + activityWaterMl).roundToStep(250)
        val micros = if (sex == Sex.MALE) {
            MicroNutrients(sodiumMg = 2300.0, potassiumMg = 3400.0, ironMg = 8.0, vitaminCMg = 90.0, vitaminDMcg = 15.0)
        } else {
            MicroNutrients(sodiumMg = 2300.0, potassiumMg = 2600.0, ironMg = 18.0, vitaminCMg = 75.0, vitaminDMcg = 15.0)
        }
        return NutritionGoals(waterMl, proteinG, carbG, fatG, fiberG, micros)
    }

    private fun Double.roundTo(step: Int): Double = (this / step).roundToInt() * step.toDouble()
    private fun Int.roundToStep(step: Int): Int = ((this + step / 2) / step) * step

    companion object {
        const val PROTEIN_G_PER_KG = 1.8
        const val FAT_ENERGY_SHARE = 0.25
        const val KCAL_PER_G_PROTEIN = 4.0
        const val KCAL_PER_G_CARB = 4.0
        const val KCAL_PER_G_FAT = 9.0
        const val RESISTANCE_MET = 5.0
    }
}
