package dev.kok.app.domain.usecase

import dev.kok.app.domain.engine.MetabolismCalculator
import dev.kok.app.domain.model.ActivityLevel
import dev.kok.app.domain.model.NutritionGoals
import dev.kok.app.domain.model.Sex
import dev.kok.app.domain.model.UserProfile
import dev.kok.app.domain.repository.ProfileRepository
import java.time.Instant
import javax.inject.Inject

data class BodyBasics(
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val sex: Sex,
    val activityLevel: ActivityLevel,
)

class SaveProfileUseCase @Inject constructor(
    private val profiles: ProfileRepository,
    private val calculator: MetabolismCalculator,
) {
    fun validate(basics: BodyBasics): String? = when {
        basics.age !in 14..100 -> "Yaş 14–100 arasında olmalı"
        basics.heightCm !in 120.0..230.0 -> "Boy 120–230 cm arasında olmalı"
        basics.weightKg !in 35.0..250.0 -> "Kilo 35–250 kg arasında olmalı"
        else -> null
    }

    fun recommend(basics: BodyBasics): NutritionGoals =
        calculator.recommendGoals(basics.weightKg, basics.heightCm, basics.age, basics.sex, basics.activityLevel)

    suspend operator fun invoke(basics: BodyBasics, goals: NutritionGoals): Result<Unit> {
        validate(basics)?.let { return Result.failure(IllegalArgumentException(it)) }
        if (goals.waterMl <= 0 || goals.proteinG <= 0 || goals.carbG < 0 || goals.fatG <= 0) {
            return Result.failure(IllegalArgumentException("Hedefler pozitif olmalı"))
        }
        return runCatching {
            profiles.saveProfile(
                UserProfile(
                    age = basics.age,
                    heightCm = basics.heightCm,
                    weightKg = basics.weightKg,
                    sex = basics.sex,
                    activityLevel = basics.activityLevel,
                    goals = goals,
                    updatedAt = Instant.EPOCH, // repository güncel zamanı yazar
                ),
            )
        }
    }
}
