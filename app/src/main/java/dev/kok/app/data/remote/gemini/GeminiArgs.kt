package dev.kok.app.data.remote.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LogMealArgs(
    val meals: List<MealArg> = emptyList(),
    val date: String? = null,
)

@Serializable
internal data class MealArg(
    val name: String,
    val grams: Double,
    val kcal: Double,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carb_g") val carbG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0,
    @SerialName("fiber_g") val fiberG: Double = 0.0,
    @SerialName("sodium_mg") val sodiumMg: Double = 0.0,
    @SerialName("potassium_mg") val potassiumMg: Double = 0.0,
    @SerialName("iron_mg") val ironMg: Double = 0.0,
    @SerialName("vitamin_c_mg") val vitaminCMg: Double = 0.0,
    @SerialName("vitamin_d_mcg") val vitaminDMcg: Double = 0.0,
    val category: String = "OTHER",
)

@Serializable
internal data class ProgramArgs(
    val name: String,
    val activate: Boolean = true,
    val days: List<DayArg> = emptyList(),
)

@Serializable
internal data class DayArg(
    val name: String,
    val exercises: List<ExerciseArg> = emptyList(),
)

@Serializable
internal data class ExerciseArg(
    val name: String,
    val sets: Int = 3,
    @SerialName("reps_min") val repsMin: Int = 8,
    @SerialName("reps_max") val repsMax: Int = 12,
    @SerialName("weight_kg") val weightKg: Double? = null,
    val notes: String? = null,
    val muscles: List<MuscleArg> = emptyList(),
)

@Serializable
internal data class MuscleArg(val code: String, val role: String)
