package dev.kok.app.presentation.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kok.app.domain.model.EntrySource
import dev.kok.app.domain.model.FoodCategory
import dev.kok.app.domain.model.FoodTemplate
import dev.kok.app.domain.model.MicroNutrients
import dev.kok.app.domain.model.NewMeal
import dev.kok.app.domain.model.Nutrients
import dev.kok.app.domain.repository.NutritionRepository
import dev.kok.app.domain.usecase.AddMealsUseCase
import dev.kok.app.presentation.feature.profile.clean
import dev.kok.app.presentation.feature.profile.toDecimal
import dev.kok.app.presentation.navigation.AddMealRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MealForm(
    val name: String = "",
    val grams: String = "",
    val category: FoodCategory = FoodCategory.OTHER,
    val kcal: String = "",
    val protein: String = "",
    val carb: String = "",
    val fat: String = "",
    val fiber: String = "",
    val sodium: String = "",
    val potassium: String = "",
    val iron: String = "",
    val vitaminC: String = "",
    val vitaminD: String = "",
)

data class AddMealUiState(
    val date: LocalDate,
    val form: MealForm = MealForm(),
    val recent: List<FoodTemplate> = emptyList(),
    /** Seçilen şablonun 1 gramlık besin değeri: gramaj değişince değerler orantılı güncellenir. */
    val perGram: Nutrients? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val savedCategory: FoodCategory? = null,
)

@HiltViewModel
class AddMealViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val nutrition: NutritionRepository,
    private val addMeals: AddMealsUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AddMealRoute>()
    private val _state = MutableStateFlow(AddMealUiState(date = LocalDate.parse(route.date)))
    val state: StateFlow<AddMealUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val recent = runCatching { nutrition.recentFoods(RECENT_LIMIT) }.getOrDefault(emptyList())
            _state.update { it.copy(recent = recent) }
        }
    }

    fun update(transform: (MealForm) -> MealForm) = _state.update {
        // Elle besin değeri değiştirilirse şablon oranı bırakılır.
        it.copy(form = transform(it.form), perGram = null, error = null)
    }

    fun setGrams(value: String) = _state.update { s ->
        val grams = value.toDecimal()
        val per = s.perGram
        if (per != null && grams != null && grams > 0) {
            s.copy(form = s.form.copy(grams = value).withNutrients(per.scaled(grams)), error = null)
        } else {
            s.copy(form = s.form.copy(grams = value), error = null)
        }
    }

    fun applyTemplate(t: FoodTemplate) = _state.update {
        it.copy(
            form = MealForm(name = t.name, grams = t.grams.clean(), category = t.category).withNutrients(t.nutrients),
            perGram = if (t.grams > 0) t.nutrients.scaled(1.0 / t.grams) else null,
            error = null,
        )
    }

    fun save() {
        val s = _state.value
        val f = s.form
        val grams = f.grams.toDecimal()
        val protein = f.protein.toDecimal() ?: 0.0
        val carb = f.carb.toDecimal() ?: 0.0
        val fat = f.fat.toDecimal() ?: 0.0
        if (grams == null) {
            _state.update { it.copy(error = "Gramajı gir") }
            return
        }
        // Kalori boşsa makrolardan Atwater katsayılarıyla hesaplanır.
        val kcal = f.kcal.toDecimal() ?: (protein * 4 + carb * 4 + fat * 9)
        val meal = NewMeal(
            date = s.date,
            name = f.name,
            grams = grams,
            nutrients = Nutrients(
                kcal = kcal,
                proteinG = protein,
                carbG = carb,
                fatG = fat,
                fiberG = f.fiber.toDecimal() ?: 0.0,
                micros = MicroNutrients(
                    sodiumMg = f.sodium.toDecimal() ?: 0.0,
                    potassiumMg = f.potassium.toDecimal() ?: 0.0,
                    ironMg = f.iron.toDecimal() ?: 0.0,
                    vitaminCMg = f.vitaminC.toDecimal() ?: 0.0,
                    vitaminDMcg = f.vitaminD.toDecimal() ?: 0.0,
                ),
            ),
            source = EntrySource.MANUAL,
            category = f.category,
        )
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            addMeals(listOf(meal))
                .onSuccess { _state.update { it.copy(saving = false, savedCategory = meal.category) } }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message) } }
        }
    }

    companion object {
        const val RECENT_LIMIT = 12
    }
}

private fun Nutrients.scaled(factor: Double) = Nutrients(
    kcal * factor, proteinG * factor, carbG * factor, fatG * factor, fiberG * factor,
    MicroNutrients(
        micros.sodiumMg * factor, micros.potassiumMg * factor, micros.ironMg * factor,
        micros.vitaminCMg * factor, micros.vitaminDMcg * factor,
    ),
)

private fun Double.short(): String = if (this == 0.0) "" else (Math.round(this * 10) / 10.0).clean()

private fun MealForm.withNutrients(n: Nutrients) = copy(
    kcal = n.kcal.short(),
    protein = n.proteinG.short(),
    carb = n.carbG.short(),
    fat = n.fatG.short(),
    fiber = n.fiberG.short(),
    sodium = n.micros.sodiumMg.short(),
    potassium = n.micros.potassiumMg.short(),
    iron = n.micros.ironMg.short(),
    vitaminC = n.micros.vitaminCMg.short(),
    vitaminD = n.micros.vitaminDMcg.short(),
)
