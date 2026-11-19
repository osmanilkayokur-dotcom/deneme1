package dev.kok.app.presentation.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kok.app.domain.repository.AssistantSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val hasKey: Boolean = false,
    val keyInput: String = "",
    val model: String = "",
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: AssistantSettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val s = settings.get()
            _state.update { it.copy(hasKey = s.isConfigured, model = s.model) }
        }
    }

    fun onKeyChange(v: String) = _state.update { it.copy(keyInput = v.trim(), message = null) }
    fun onModelChange(v: String) = _state.update { it.copy(model = v.trim(), message = null) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        val key = s.keyInput.takeIf { it.isNotBlank() }
        if (key != null && key.length < 20) {
            _state.update { it.copy(message = "Anahtar çok kısa görünüyor") }
            return@launch
        }
        settings.save(key, s.model)
        val saved = settings.get()
        _state.update { it.copy(hasKey = saved.isConfigured, keyInput = "", model = saved.model, message = "Kaydedildi") }
    }

    fun removeKey() = viewModelScope.launch {
        settings.clearApiKey()
        _state.update { it.copy(hasKey = false, message = "Anahtar silindi") }
    }
}
