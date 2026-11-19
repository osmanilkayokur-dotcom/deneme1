package dev.kok.app.presentation.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kok.app.domain.model.ActivityLevel
import dev.kok.app.domain.model.Sex
import dev.kok.app.presentation.components.BranchDivider
import dev.kok.app.presentation.components.NumberField
import dev.kok.app.presentation.components.PaperCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRoute(
    onDone: () -> Unit,
    showBack: Boolean,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            if (showBack) {
                onDone()
            } else {
                viewModel.consumeSaved()
                snackbar.showSnackbar("Profil ve hedefler kaydedildi")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.isExisting) "Profil ve hedefler" else "Profil oluştur") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onDone) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val f = state.form
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
            PaperCard {
                Text("Vücut", style = MaterialTheme.typography.titleLarge)
                BranchDivider(Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(f.age, { v -> viewModel.update { it.copy(age = v) } }, "Yaş", Modifier.weight(1f), decimal = false)
                    NumberField(f.heightCm, { v -> viewModel.update { it.copy(heightCm = v) } }, "Boy", Modifier.weight(1f), suffix = "cm")
                    NumberField(f.weightKg, { v -> viewModel.update { it.copy(weightKg = v) } }, "Kilo", Modifier.weight(1f), suffix = "kg")
                }
                Spacer(Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Sex.entries.forEachIndexed { i, sex ->
                        SegmentedButton(
                            selected = f.sex == sex,
                            onClick = { viewModel.update { it.copy(sex = sex) } },
                            shape = SegmentedButtonDefaults.itemShape(i, Sex.entries.size),
                        ) { Text(if (sex == Sex.MALE) "Erkek" else "Kadın") }
                    }
                }
            }

            PaperCard {
                Text("Aktivite düzeyi", style = MaterialTheme.typography.titleLarge)
                BranchDivider(Modifier.padding(vertical = 8.dp))
                Column(Modifier.selectableGroup()) {
                    ActivityLevel.entries.forEach { level ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = f.activity == level,
                                    onClick = { viewModel.update { it.copy(activity = level) } },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = f.activity == level, onClick = null)
                            Text(
                                "${level.label}  ×${level.multiplier}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                    }
                }
            }

            PaperCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Günlük hedefler", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = viewModel::applyRecommendations) { Text("Önerileri uygula") }
                }
                BranchDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    "Boş bırakırsan vücut bilgilerine göre otomatik hesaplanır. Protein 1,8 g/kg, yağ enerjinin %25'i, kalanı karbonhidrat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                GoalRow(
                    GoalField(f.waterMl, "Su", "ml") { v -> viewModel.update { it.copy(waterMl = v) } },
                    GoalField(f.proteinG, "Protein", "g") { v -> viewModel.update { it.copy(proteinG = v) } },
                )
                GoalRow(
                    GoalField(f.carbG, "Karbonhidrat", "g") { v -> viewModel.update { it.copy(carbG = v) } },
                    GoalField(f.fatG, "Yağ", "g") { v -> viewModel.update { it.copy(fatG = v) } },
                )
                GoalRow(
                    GoalField(f.fiberG, "Lif", "g") { v -> viewModel.update { it.copy(fiberG = v) } },
                    GoalField(f.sodiumMg, "Sodyum (üst)", "mg") { v -> viewModel.update { it.copy(sodiumMg = v) } },
                )
                GoalRow(
                    GoalField(f.potassiumMg, "Potasyum", "mg") { v -> viewModel.update { it.copy(potassiumMg = v) } },
                    GoalField(f.ironMg, "Demir", "mg") { v -> viewModel.update { it.copy(ironMg = v) } },
                )
                GoalRow(
                    GoalField(f.vitaminCMg, "C vitamini", "mg") { v -> viewModel.update { it.copy(vitaminCMg = v) } },
                    GoalField(f.vitaminDMcg, "D vitamini", "mcg") { v -> viewModel.update { it.copy(vitaminDMcg = v) } },
                )
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (state.saving) "Kaydediliyor…" else "Kaydet") }
        }
    }
}

private class GoalField(val value: String, val label: String, val unit: String, val onChange: (String) -> Unit)

@Composable
private fun GoalRow(a: GoalField, b: GoalField) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        listOf(a, b).forEach { field ->
            NumberField(field.value, field.onChange, field.label, Modifier.weight(1f), suffix = field.unit)
        }
    }
}
