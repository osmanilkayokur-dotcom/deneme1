package dev.kok.app.presentation.feature.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kok.app.domain.model.FoodCategory
import dev.kok.app.presentation.components.BranchDivider
import dev.kok.app.presentation.components.NumberField
import dev.kok.app.presentation.components.PaperCard
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMealRouteScreen(
    onBack: () -> Unit,
    onSaved: (FoodCategory) -> Unit,
    viewModel: AddMealViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedCategory) { state.savedCategory?.let(onSaved) }
    var showMicros by rememberSaveable { mutableStateOf(false) }
    val f = state.form
    val dateText = state.date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("tr-TR")))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yemek ekle · $dateText") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.recent.isNotEmpty()) {
                PaperCard {
                    Text("Son yediklerin", style = MaterialTheme.typography.titleMedium)
                    BranchDivider(Modifier.padding(vertical = 6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.recent.forEach { t ->
                            AssistChip(onClick = { viewModel.applyTemplate(t) }, label = { Text(t.name) })
                        }
                    }
                }
            }

            PaperCard {
                OutlinedTextField(
                    value = f.name,
                    onValueChange = { v -> viewModel.update { it.copy(name = v.take(80)) } },
                    label = { Text("Yemek adı") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                    NumberField(f.grams, viewModel::setGrams, "Gramaj", Modifier.weight(1f), suffix = "g")
                    NumberField(f.kcal, { v -> viewModel.update { it.copy(kcal = v) } }, "Kalori", Modifier.weight(1f), suffix = "kcal")
                }
                Text(
                    "Kaloriyi boş bırakırsan makrolardan hesaplanır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text("Kategori", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodCategory.entries.forEach { c ->
                        FilterChip(
                            selected = f.category == c,
                            onClick = { viewModel.update { it.copy(category = c) } },
                            label = { Text(c.label) },
                        )
                    }
                }
            }

            PaperCard {
                Text("Makrolar", style = MaterialTheme.typography.titleMedium)
                BranchDivider(Modifier.padding(vertical = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(f.protein, { v -> viewModel.update { it.copy(protein = v) } }, "Protein", Modifier.weight(1f), suffix = "g")
                    NumberField(f.carb, { v -> viewModel.update { it.copy(carb = v) } }, "Karb.", Modifier.weight(1f), suffix = "g")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    NumberField(f.fat, { v -> viewModel.update { it.copy(fat = v) } }, "Yağ", Modifier.weight(1f), suffix = "g")
                    NumberField(f.fiber, { v -> viewModel.update { it.copy(fiber = v) } }, "Lif", Modifier.weight(1f), suffix = "g")
                }
                TextButton(onClick = { showMicros = !showMicros }, modifier = Modifier.padding(top = 4.dp)) {
                    Text(if (showMicros) "Mikro besinleri gizle" else "Mikro besinler (isteğe bağlı)")
                }
                AnimatedVisibility(showMicros) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumberField(f.sodium, { v -> viewModel.update { it.copy(sodium = v) } }, "Sodyum", Modifier.weight(1f), suffix = "mg")
                            NumberField(f.potassium, { v -> viewModel.update { it.copy(potassium = v) } }, "Potasyum", Modifier.weight(1f), suffix = "mg")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumberField(f.iron, { v -> viewModel.update { it.copy(iron = v) } }, "Demir", Modifier.weight(1f), suffix = "mg")
                            NumberField(f.vitaminC, { v -> viewModel.update { it.copy(vitaminC = v) } }, "C vit.", Modifier.weight(1f), suffix = "mg")
                        }
                        NumberField(f.vitaminD, { v -> viewModel.update { it.copy(vitaminD = v) } }, "D vitamini", Modifier.fillMaxWidth(), suffix = "mcg")
                    }
                }
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (state.saving) "Kaydediliyor…" else "Kaydet") }
        }
    }
}
