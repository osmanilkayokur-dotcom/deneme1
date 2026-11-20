package dev.kok.app.presentation.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kok.app.domain.model.BodyMeasurement
import dev.kok.app.domain.repository.MeasurementSaveResult
import dev.kok.app.domain.usecase.MeasurementInput
import dev.kok.app.domain.usecase.MeasurementSchedule
import dev.kok.app.domain.usecase.ObserveMeasurementScheduleUseCase
import dev.kok.app.domain.usecase.SaveMeasurementUseCase
import dev.kok.app.presentation.feature.profile.clean
import dev.kok.app.presentation.feature.profile.toDecimal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeasurementForm(
    val weight: String = "",
    val fat: String = "",
    val chest: String = "",
    val waist: String = "",
    val arm: String = "",
    val leg: String = "",
    val shoulder: String = "",
    val neck: String = "",
) {
    companion object {
        fun from(m: BodyMeasurement) = MeasurementForm(
            weight = m.weightKg.clean(),
            fat = m.bodyFatPercent?.clean().orEmpty(),
            chest = m.chestCm?.clean().orEmpty(),
            waist = m.waistCm?.clean().orEmpty(),
            arm = m.armCm?.clean().orEmpty(),
            leg = m.legCm?.clean().orEmpty(),
            shoulder = m.shoulderCm?.clean().orEmpty(),
            neck = m.neckCm?.clean().orEmpty(),
        )
    }
}

/** Form ekranı: null kapalı, NEW yeni kayıt, EDIT bu ayın kaydını düzenleme. */
enum class FormMode { NEW, EDIT }

@HiltViewModel
class BodyViewModel @Inject constructor(
    observeSchedule: ObserveMeasurementScheduleUseCase,
    private val save: SaveMeasurementUseCase,
) : ViewModel() {

    val schedule: StateFlow<MeasurementSchedule?> =
        observeSchedule().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _form = MutableStateFlow(MeasurementForm())
    val form: StateFlow<MeasurementForm> = _form.asStateFlow()

    private val _mode = MutableStateFlow<FormMode?>(null)
    val mode: StateFlow<FormMode?> = _mode.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun openForm() {
        val current = schedule.value?.currentEntry
        _form.value = current?.let(MeasurementForm::from) ?: MeasurementForm()
        _mode.value = if (current == null) FormMode.NEW else FormMode.EDIT
        _error.value = null
    }

    fun closeForm() { _mode.value = null }

    fun update(transform: (MeasurementForm) -> MeasurementForm) {
        _form.update(transform)
        _error.value = null
    }

    fun submit() {
        val f = _form.value
        val weight = f.weight.toDecimal()
        if (weight == null) {
            _error.value = "Kilo zorunlu"
            return
        }
        val input = MeasurementInput(
            weightKg = weight,
            bodyFatPercent = f.fat.toDecimal(),
            chestCm = f.chest.toDecimal(),
            waistCm = f.waist.toDecimal(),
            armCm = f.arm.toDecimal(),
            legCm = f.leg.toDecimal(),
            shoulderCm = f.shoulder.toDecimal(),
            neckCm = f.neck.toDecimal(),
        )
        viewModelScope.launch {
            save(input)
                .onSuccess { r ->
                    when (r) {
                        is MeasurementSaveResult.Saved -> {
                            _mode.value = null
                            _messages.send("Ölçüm kaydedildi; profil kilon güncellendi")
                        }
                        MeasurementSaveResult.MonthAlreadyRecorded -> _error.value = "Bu ay zaten ölçüm girilmiş"
                    }
                }
                .onFailure { _error.value = it.message }
        }
    }
}
