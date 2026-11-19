package dev.kok.app.presentation.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kok.app.domain.model.ActivityLevel
import dev.kok.app.domain.model.MicroNutrients
import dev.kok.app.domain.model.NutritionGoals
import dev.kok.app.domain.model.Sex
import dev.kok.app.domain.repository.ProfileRepository
import dev.kok.app.domain.usecase.BodyBasics
import dev.kok.app.domain.usecase.SaveProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/** Form alanları metin olarak tutulur; kaydederken ayrıştırılır. */
data class ProfileForm(
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val sex: Sex = Sex.MALE,
    val activity: ActivityLevel = ActivityLevel.MODERATE,
    val waterMl: String = "",
    val proteinG: String = "",
    val carbG: String = "",
    val fatG: String = "",
    val fiberG: String = "",
    val sodiumMg: String = "",
    val potassiumMg: String = "",
    val ironMg: String = "",
    val vitaminCMg: String = "",
    val vitaminDMcg: String = "",
)

data class ProfileUiState(
    val form: ProfileForm = ProfileForm(),
    val isExisting: Boolean = false,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val saveProfile: SaveProfileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val p = profiles.getProfile()
            _state.update {
                if (p == null) {
                    it.copy(loading = false)
                } else {
                    it.copy(
                        loading = false,
                        isExisting = true,
                        form = ProfileForm(
                            age = p.age.toString(),
                            heightCm = p.heightCm.clean(),
                            weightKg = p.weightKg.clean(),
                            sex = p.sex,
                            activity = p.activityLevel,
                        ).withGoals(p.goals),
                    )
                }
            }
        }
    }

    fun consumeSaved() = _state.update { it.copy(saved = false, isExisting = true) }

    fun update(transform: (ProfileForm) -> ProfileForm) =
        _state.update { it.copy(form = transform(it.form), error = null) }

    /** Vücut bilgilerinden hedefleri yeniden hesaplar ve forma yazar. */
    fun applyRecommendations() {
        val basics = parseBasics() ?: return
        saveProfile.validate(basics)?.let { msg -> _state.update { it.copy(error = msg) }; return }
        update { it.withGoals(saveProfile.recommend(basics)) }
    }

    fun save() {
        val basics = parseBasics() ?: return
        saveProfile.validate(basics)?.let { msg -> _state.update { it.copy(error = msg) }; return }
        val goals = parseGoals() ?: saveProfile.recommend(basics)
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            saveProfile(basics, goals)
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message) } }
        }
    }

    private fun parseBasics(): BodyBasics? {
        val f = _state.value.form
        val age = f.age.toIntOrNull()
        val h = f.heightCm.toDecimal()
        val w = f.weightKg.toDecimal()
        if (age == null || h == null || w == null) {
            _state.update { it.copy(error = "Yaş, boy ve kilo alanlarını sayı olarak doldur") }
            return null
        }
        return BodyBasics(age, h, w, f.sex, f.activity)
    }

    /** Hedef alanlarının tamamı doluysa onları, hiçbiri dolu değilse null (öneri kullanılır) döner. */
    private fun parseGoals(): NutritionGoals? {
        val f = _state.value.form
        val values = listOf(f.waterMl, f.proteinG, f.carbG, f.fatG, f.fiberG, f.sodiumMg, f.potassiumMg, f.ironMg, f.vitaminCMg, f.vitaminDMcg)
        if (values.all { it.isBlank() }) return null
        val parsed = values.map { it.toDecimal() }
        if (parsed.any { it == null }) {
            _state.update { it.copy(error = "Hedef alanlarını boş bırakma ya da 'Önerileri uygula'ya bas") }
            return null
        }
        val v = parsed.filterNotNull()
        return NutritionGoals(
            waterMl = v[0].roundToInt(),
            proteinG = v[1],
            carbG = v[2],
            fatG = v[3],
            fiberG = v[4],
            micros = MicroNutrients(v[5], v[6], v[7], v[8], v[9]),
        )
    }
}

private fun ProfileForm.withGoals(g: NutritionGoals) = copy(
    waterMl = g.waterMl.toString(),
    proteinG = g.proteinG.clean(),
    carbG = g.carbG.clean(),
    fatG = g.fatG.clean(),
    fiberG = g.fiberG.clean(),
    sodiumMg = g.micros.sodiumMg.clean(),
    potassiumMg = g.micros.potassiumMg.clean(),
    ironMg = g.micros.ironMg.clean(),
    vitaminCMg = g.micros.vitaminCMg.clean(),
    vitaminDMcg = g.micros.vitaminDMcg.clean(),
)

/** 80.0 -> "80", 72.5 -> "72,5" */
internal fun Double.clean(): String =
    if (this == this.roundToInt().toDouble()) this.roundToInt().toString() else toString().replace('.', ',')

/** Hem "72,5" hem "72.5" kabul edilir. */
internal fun String.toDecimal(): Double? = trim().replace(',', '.').toDoubleOrNull()
