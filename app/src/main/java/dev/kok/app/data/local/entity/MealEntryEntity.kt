package dev.kok.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.kok.app.domain.model.EntrySource
import dev.kok.app.domain.model.FoodCategory
import java.time.Instant

@Entity(
    tableName = "meal_entry",
    foreignKeys = [
        ForeignKey(
            entity = DailyLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["daily_log_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["daily_log_id"])],
)
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "daily_log_id") val dailyLogId: Long,
    val name: String,
    val grams: Double,
    val kcal: Double,
    @ColumnInfo(name = "protein_g") val proteinG: Double,
    @ColumnInfo(name = "carb_g") val carbG: Double,
    @ColumnInfo(name = "fat_g") val fatG: Double,
    @ColumnInfo(name = "fiber_g") val fiberG: Double,
    @ColumnInfo(name = "sodium_mg") val sodiumMg: Double,
    @ColumnInfo(name = "potassium_mg") val potassiumMg: Double,
    @ColumnInfo(name = "iron_mg") val ironMg: Double,
    @ColumnInfo(name = "vitamin_c_mg") val vitaminCMg: Double,
    @ColumnInfo(name = "vitamin_d_mcg") val vitaminDMcg: Double,
    val source: EntrySource,
    @ColumnInfo(name = "animation_category") val animationCategory: FoodCategory,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
