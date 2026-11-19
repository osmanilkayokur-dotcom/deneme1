package dev.kok.app.domain.repository

import dev.kok.app.domain.model.BodyMeasurement
import dev.kok.app.domain.model.ChatMessage
import dev.kok.app.domain.model.DaySummary
import dev.kok.app.domain.model.Exercise
import dev.kok.app.domain.model.FoodTemplate
import dev.kok.app.domain.model.Meal
import dev.kok.app.domain.model.MuscleGroup
import dev.kok.app.domain.model.NewMeal
import dev.kok.app.domain.model.ProgramDraft
import dev.kok.app.domain.model.SetEntry
import dev.kok.app.domain.model.UserProfile
import dev.kok.app.domain.model.WorkoutDay
import dev.kok.app.domain.model.WorkoutProgram
import dev.kok.app.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getProfile(): UserProfile?
    suspend fun saveProfile(profile: UserProfile)
    suspend fun updateWeight(weightKg: Double)
}

interface NutritionRepository {
    /** Kayıt yoksa boş bir gün özeti yayar. */
    fun observeDay(date: LocalDate): Flow<DaySummary>
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DaySummary>>
    suspend fun getRange(start: LocalDate, end: LocalDate): List<DaySummary>
    suspend fun addMeal(meal: NewMeal): Long
    suspend fun addMeals(meals: List<NewMeal>): List<Long>
    suspend fun updateMeal(meal: Meal)
    suspend fun deleteMeals(ids: List<Long>)
    suspend fun addWater(date: LocalDate, deltaMl: Int)
    suspend fun addBurnedCalories(date: LocalDate, deltaKcal: Int)
    suspend fun recentFoods(limit: Int): List<FoodTemplate>
}

interface WorkoutRepository {
    fun observePrograms(): Flow<List<WorkoutProgram>>
    fun observeActiveProgram(): Flow<WorkoutProgram?>
    fun observeExercises(): Flow<List<Exercise>>
    fun observeMuscleGroups(): Flow<List<MuscleGroup>>
    suspend fun getExercises(): List<Exercise>
    suspend fun getMuscleGroups(): List<MuscleGroup>
    suspend fun getProgram(id: Long): WorkoutProgram?
    suspend fun getPrograms(): List<WorkoutProgram>
    suspend fun getProgramIdByName(name: String): Long?

    /** Aynı adlı program varsa günleri baştan yazılır; yoksa yeni program oluşur. Program id'si döner. */
    suspend fun createOrUpdateProgram(draft: ProgramDraft, activate: Boolean): Long
    suspend fun renameProgram(id: Long, name: String)
    suspend fun deleteProgram(id: Long)
    suspend fun activateProgram(id: Long)

    fun observeSelectedDayId(date: LocalDate): Flow<Long?>
    suspend fun selectWorkoutDay(date: LocalDate, workoutDayId: Long?)
    fun observeWorkoutDay(id: Long): Flow<WorkoutDay?>

    suspend fun logSession(
        date: LocalDate,
        workoutDayId: Long?,
        startedAt: Instant,
        durationMin: Int,
        burnedKcal: Int,
        notes: String?,
        sets: List<SetEntry>,
    ): Long
    fun observeSessions(start: LocalDate, end: LocalDate): Flow<List<WorkoutSession>>
}

sealed interface MeasurementSaveResult {
    data class Saved(val id: Long) : MeasurementSaveResult
    data object MonthAlreadyRecorded : MeasurementSaveResult
}

interface BodyRepository {
    fun observeAll(): Flow<List<BodyMeasurement>>
    suspend fun getForMonth(yearMonth: YearMonth): BodyMeasurement?
    suspend fun insert(measurement: BodyMeasurement): MeasurementSaveResult
    suspend fun update(measurement: BodyMeasurement)
}

interface ChatRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun recent(limit: Int): List<ChatMessage>
    suspend fun append(message: ChatMessage): Long
    suspend fun updateAction(id: Long, summary: String?, undoRef: String?)
    suspend fun clear()
}

data class AssistantSettings(val apiKey: String?, val model: String) {
    val isConfigured: Boolean get() = !apiKey.isNullOrBlank()
}

interface AssistantSettingsRepository {
    fun observe(): Flow<AssistantSettings?>
    suspend fun get(): AssistantSettings
    /** [apiKey] null ise mevcut anahtar korunur, yalnızca model güncellenir. */
    suspend fun save(apiKey: String?, model: String)
    suspend fun clearApiKey()
}

class AssistantNotConfiguredException : IllegalStateException("Gemini API anahtarı girilmemiş")

class AssistantRequestException(message: String, val httpCode: Int? = null) : RuntimeException(message)

interface AssistantGateway {
    /** Sohbet geçmişine göre bir sonraki model yanıtını üretir. */
    suspend fun generate(
        history: List<dev.kok.app.domain.model.ChatMessage>,
        context: dev.kok.app.domain.model.AssistantContext,
    ): dev.kok.app.domain.model.AssistantReply

    /** Saklanmış bir model mesajındaki fonksiyon çağrısını domain aksiyonuna çevirir. */
    fun parseAction(message: dev.kok.app.domain.model.ChatMessage, today: java.time.LocalDate): dev.kok.app.domain.model.AssistantAction?

    /** Aksiyon sonucunu modele dönecek fonksiyon yanıtı yüküne çevirir. */
    fun encodeOutcome(toolName: String, outcome: dev.kok.app.domain.model.ToolOutcome): String
}
