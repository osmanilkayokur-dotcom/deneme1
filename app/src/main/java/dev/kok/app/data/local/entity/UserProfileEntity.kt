package dev.kok.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.kok.app.domain.model.ActivityLevel
import dev.kok.app.domain.model.Sex
import java.time.Instant

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val age: Int,
    @ColumnInfo(name = "height_cm") val heightCm: Double,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    val sex: Sex,
    @ColumnInfo(name = "activity_level") val activityLevel: ActivityLevel,
    @ColumnInfo(name = "water_goal_ml") val waterGoalMl: Int,
    @ColumnInfo(name = "protein_goal_g") val proteinGoalG: Double,
    @ColumnInfo(name = "carb_goal_g") val carbGoalG: Double,
    @ColumnInfo(name = "fat_goal_g") val fatGoalG: Double,
    @ColumnInfo(name = "fiber_goal_g") val fiberGoalG: Double,
    @ColumnInfo(name = "sodium_goal_mg") val sodiumGoalMg: Double,
    @ColumnInfo(name = "potassium_goal_mg") val potassiumGoalMg: Double,
    @ColumnInfo(name = "iron_goal_mg") val ironGoalMg: Double,
    @ColumnInfo(name = "vitamin_c_goal_mg") val vitaminCGoalMg: Double,
    @ColumnInfo(name = "vitamin_d_goal_mcg") val vitaminDGoalMcg: Double,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
