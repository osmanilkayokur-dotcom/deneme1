package dev.kok.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.YearMonth

@Entity(
    tableName = "body_measurement",
    indices = [Index(value = ["year_month"], unique = true)],
)
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "year_month") val yearMonth: YearMonth,
    @ColumnInfo(name = "measured_on") val measuredOn: LocalDate,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    @ColumnInfo(name = "body_fat_percent") val bodyFatPercent: Double?,
    @ColumnInfo(name = "chest_cm") val chestCm: Double?,
    @ColumnInfo(name = "waist_cm") val waistCm: Double?,
    @ColumnInfo(name = "arm_cm") val armCm: Double?,
    @ColumnInfo(name = "leg_cm") val legCm: Double?,
    @ColumnInfo(name = "shoulder_cm") val shoulderCm: Double?,
    @ColumnInfo(name = "neck_cm") val neckCm: Double?,
)
