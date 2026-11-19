package dev.kok.app.domain.usecase

import dev.kok.app.core.TimeProvider
import dev.kok.app.domain.model.AssistantAction
import dev.kok.app.domain.model.AssistantContext
import dev.kok.app.domain.model.AssistantReply
import dev.kok.app.domain.model.ChatMessage
import dev.kok.app.domain.model.ChatRole
import dev.kok.app.domain.model.ToolOutcome
import dev.kok.app.domain.repository.AssistantGateway
import dev.kok.app.domain.repository.ChatRepository
import dev.kok.app.domain.repository.NutritionRepository
import dev.kok.app.domain.repository.ProfileRepository
import dev.kok.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Sohbet akışının orkestrasyonu:
 * kullanıcı mesajı → model → (araç çağrısı ise) onay kartı → onay/iptal → araç yanıtı → modelin kapanış mesajı.
 * Onaylanan aksiyonlar geri alınabilir (undoRef).
 */
@Singleton
class ChatAssistantUseCase @Inject constructor(
    private val chat: ChatRepository,
    private val gateway: AssistantGateway,
    private val profiles: ProfileRepository,
    private val workouts: WorkoutRepository,
    private val nutrition: NutritionRepository,
    private val addMeals: AddMealsUseCase,
    private val time: TimeProvider,
) {
    private val mutex = Mutex()

    fun observe(): Flow<List<ChatMessage>> = chat.observeMessages()

    fun actionOf(message: ChatMessage): AssistantAction? =
        if (message.role == ChatRole.MODEL && message.toolName != null) gateway.parseAction(message, time.today()) else null

    suspend fun send(text: String): Result<Unit> = mutex.withLock {
        val clean = text.trim()
        if (clean.isEmpty()) return@withLock Result.success(Unit)
        chat.append(ChatMessage(0, ChatRole.USER, clean, time.now()))
        generateAndStore()
    }

    /** Son kullanıcı mesajına yanıt alınamadıysa tekrar dener. */
    suspend fun retry(): Result<Unit> = mutex.withLock { generateAndStore() }

    suspend fun confirm(message: ChatMessage): Result<Unit> = mutex.withLock {
        val action = actionOf(message) ?: return@withLock Result.failure(IllegalStateException("Aksiyon okunamadı"))
        val applied = runCatching { apply(action) }
        val outcome = applied.fold(
            onSuccess = { ToolOutcome(ToolOutcome.Status.SAVED, it.summary) },
            onFailure = { ToolOutcome(ToolOutcome.Status.FAILED, it.message ?: "Kaydedilemedi") },
        )
        chat.updateAction(message.id, outcome.message, applied.getOrNull()?.undoRef)
        chat.append(toolMessage(action.toolName, outcome))
        applied.exceptionOrNull()?.let { return@withLock Result.failure(it) }
        generateAndStore()
    }

    suspend fun reject(message: ChatMessage): Result<Unit> = mutex.withLock {
        val toolName = message.toolName ?: return@withLock Result.success(Unit)
        val outcome = ToolOutcome(ToolOutcome.Status.CANCELLED, "Kullanıcı öneriyi reddetti.")
        chat.updateAction(message.id, "İptal edildi", null)
        chat.append(toolMessage(toolName, outcome))
        generateAndStore()
    }

    suspend fun undo(message: ChatMessage): Result<Unit> = mutex.withLock {
        val ref = message.undoRef ?: return@withLock Result.success(Unit)
        runCatching {
            val (kind, ids) = ref.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            when (kind) {
                UNDO_MEALS -> nutrition.deleteMeals(ids.split(",").mapNotNull { it.toLongOrNull() })
                UNDO_PROGRAM -> ids.toLongOrNull()?.let { workouts.deleteProgram(it) }
                else -> error("Bilinmeyen geri alma türü")
            }
            chat.updateAction(message.id, "Geri alındı", null)
        }
    }

    suspend fun clear() = mutex.withLock { chat.clear() }

    private data class Applied(val summary: String, val undoRef: String?)

    private suspend fun apply(action: AssistantAction): Applied = when (action) {
        is AssistantAction.LogMeals -> {
            val ids = addMeals(action.meals).getOrThrow()
            val kcal = action.meals.sumOf { it.nutrients.kcal }.roundToInt()
            val protein = action.meals.sumOf { it.nutrients.proteinG }.roundToInt()
            Applied("${ids.size} yemek kaydedildi · $kcal kcal · $protein g protein", "$UNDO_MEALS:${ids.joinToString(",")}")
        }
        is AssistantAction.UpsertProgram -> {
            val before = workouts.getProgramIdByName(action.draft.name)
            val id = workouts.createOrUpdateProgram(action.draft, action.activate)
            val verb = if (before == null) "oluşturuldu" else "güncellendi"
            Applied(
                "'${action.draft.name}' programı $verb · ${action.draft.days.size} gün",
                if (before == null) "$UNDO_PROGRAM:$id" else null, // güncellemeler geri alınamaz (eski hali saklanmıyor)
            )
        }
    }

    private suspend fun generateAndStore(): Result<Unit> = runCatching {
        val reply = gateway.generate(chat.recent(HISTORY_LIMIT), buildContext())
        when (reply) {
            is AssistantReply.Text -> chat.append(
                ChatMessage(0, ChatRole.MODEL, reply.text, time.now(), toolPayloadJson = reply.rawContentJson),
            )
            is AssistantReply.Action -> chat.append(
                ChatMessage(
                    id = 0,
                    role = ChatRole.MODEL,
                    text = reply.leadText ?: "",
                    createdAt = time.now(),
                    toolName = reply.action.toolName,
                    toolPayloadJson = reply.rawContentJson,
                ),
            )
        }
        Unit
    }

    private suspend fun buildContext() = AssistantContext(
        today = time.today(),
        profile = profiles.getProfile(),
        programs = workouts.getPrograms(),
        exerciseNames = workouts.getExercises().map { it.name },
        muscles = workouts.getMuscleGroups(),
    )

    private fun toolMessage(toolName: String, outcome: ToolOutcome) = ChatMessage(
        id = 0,
        role = ChatRole.TOOL,
        text = outcome.message,
        createdAt = time.now(),
        toolName = toolName,
        toolPayloadJson = gateway.encodeOutcome(toolName, outcome),
    )

    companion object {
        private const val HISTORY_LIMIT = 40
        const val UNDO_MEALS = "meals"
        const val UNDO_PROGRAM = "program"
    }
}
