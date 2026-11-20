package dev.kok.app.presentation.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kok.app.domain.model.AssistantAction
import dev.kok.app.domain.model.ChatMessage
import dev.kok.app.domain.model.ChatRole
import dev.kok.app.domain.model.FoodCategory
import dev.kok.app.domain.repository.AssistantNotConfiguredException
import dev.kok.app.domain.repository.AssistantRequestException
import dev.kok.app.domain.repository.AssistantSettingsRepository
import dev.kok.app.domain.usecase.ChatAssistantUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ActionStatus { PENDING, RESOLVED, EXPIRED }

data class ChatItem(
    val message: ChatMessage,
    val action: AssistantAction?,
    val status: ActionStatus?,
)

data class ChatUiState(
    val items: List<ChatItem> = emptyList(),
    val configured: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val canRetry: Boolean = false,
    /** Onaylanan yemek kaydının baskın kategorisi: doğa animasyonu için. */
    val mealAnimation: FoodCategory? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val assistant: ChatAssistantUseCase,
    settings: AssistantSettingsRepository,
) : ViewModel() {

    private val ui = MutableStateFlow(ChatUiState())

    val state: StateFlow<ChatUiState> = combine(assistant.observe(), settings.observe(), ui) { messages, cfg, local ->
        local.copy(items = toItems(messages), configured = cfg?.isConfigured ?: false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    /** Onay kartı yalnızca en son model mesajındaysa aktiftir; sonrasında yazılırsa süresi dolar. */
    private fun toItems(messages: List<ChatMessage>): List<ChatItem> {
        val visible = messages.filter { it.role != ChatRole.TOOL }
        val lastModelId = visible.lastOrNull()?.takeIf { it.role == ChatRole.MODEL }?.id
        return visible.map { m ->
            val action = assistant.actionOf(m)
            val status = when {
                m.toolName == null || m.role != ChatRole.MODEL -> null
                m.actionSummary != null -> ActionStatus.RESOLVED
                m.id == lastModelId -> ActionStatus.PENDING
                else -> ActionStatus.EXPIRED
            }
            ChatItem(m, action, status)
        }
    }

    fun send(text: String) = run(retryable = true) { assistant.send(text) }
    fun retry() = run(retryable = true) { assistant.retry() }
    fun confirm(item: ChatItem) = run {
        assistant.confirm(item.message).also { result ->
            val saved = result.isSuccess || result.exceptionOrNull() is AssistantRequestException
            val meals = (item.action as? AssistantAction.LogMeals)?.meals
            if (saved && meals != null) {
                ui.update { it.copy(mealAnimation = meals.maxByOrNull { m -> m.grams }?.category) }
            }
        }
    }

    fun mealAnimationDone() = ui.update { it.copy(mealAnimation = null) }
    fun reject(item: ChatItem) = run { assistant.reject(item.message) }
    fun undo(item: ChatItem) = run { assistant.undo(item.message) }
    fun clear() = run { runCatching { assistant.clear() } }
    fun dismissError() = ui.update { it.copy(error = null, canRetry = false) }

    private fun run(retryable: Boolean = false, block: suspend () -> Result<*>) {
        if (ui.value.busy) return
        ui.update { it.copy(busy = true, error = null, canRetry = false) }
        viewModelScope.launch {
            val result = block()
            ui.update { s ->
                val e = result.exceptionOrNull()
                s.copy(
                    busy = false,
                    error = when (e) {
                        null -> null
                        is AssistantNotConfiguredException -> "Önce Ayarlar'dan Gemini API anahtarını gir."
                        else -> e.message ?: "Bir hata oluştu"
                    },
                    canRetry = e != null && retryable && e !is AssistantNotConfiguredException,
                )
            }
        }
    }
}
