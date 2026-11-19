package dev.kok.app.domain.model

import java.time.LocalDate

/** Asistanın veritabanına uygulamayı önerdiği yapılandırılmış aksiyon (Function Calling sonucu). */
sealed interface AssistantAction {
    val toolName: String

    data class LogMeals(val meals: List<NewMeal>) : AssistantAction {
        override val toolName = TOOL_LOG_MEAL
    }

    data class UpsertProgram(val draft: ProgramDraft, val activate: Boolean) : AssistantAction {
        override val toolName = TOOL_UPSERT_PROGRAM
    }

    companion object {
        const val TOOL_LOG_MEAL = "log_meal"
        const val TOOL_UPSERT_PROGRAM = "create_or_update_workout_program"
    }
}

sealed interface AssistantReply {
    /** Modelin ham içeriği (düşünce imzaları dahil) geçmişe aynen geri gönderilmek üzere saklanır. */
    val rawContentJson: String

    data class Text(val text: String, override val rawContentJson: String) : AssistantReply

    data class Action(
        val action: AssistantAction,
        val leadText: String?,
        override val rawContentJson: String,
    ) : AssistantReply
}

/** Aksiyon sonucunun modele bildirilen özeti. */
data class ToolOutcome(val status: Status, val message: String) {
    enum class Status { SAVED, CANCELLED, FAILED }
}

/** Modelin doğru karar verebilmesi için sistem talimatına eklenen güncel uygulama bağlamı. */
data class AssistantContext(
    val today: LocalDate,
    val profile: UserProfile?,
    val programs: List<WorkoutProgram>,
    val exerciseNames: List<String>,
    val muscles: List<MuscleGroup>,
)
