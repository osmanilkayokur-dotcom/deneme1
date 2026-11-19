package dev.kok.app.presentation.feature.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kok.app.domain.model.Exercise
import dev.kok.app.domain.model.MuscleGroup
import dev.kok.app.domain.model.MuscleRole
import dev.kok.app.presentation.components.BranchDivider
import dev.kok.app.presentation.components.NumberField
import dev.kok.app.presentation.components.PaperCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramEditorRouteScreen(onDone: () -> Unit, viewModel: ProgramEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickerFor by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    pickerFor?.let { dayKey ->
        ExercisePickerDialog(
            catalog = state.catalog,
            muscles = state.muscles,
            onDismiss = { pickerFor = null },
            onPick = { name, muscles ->
                viewModel.addExercise(dayKey, name, muscles)
                pickerFor = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.programId == null) "Yeni program" else "Programı düzenle") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
                actions = { TextButton(onClick = viewModel::save, enabled = !state.saving) { Text("Kaydet") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize().imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text("Program adı") },
                    placeholder = { Text("Ör: PPL, Upper/Lower") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
            }
            items(state.days, key = { it.key }) { day ->
                PaperCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = day.name,
                            onValueChange = { viewModel.renameDay(day.key, it) },
                            label = { Text("Gün adı") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                        )
                        IconButton(onClick = { viewModel.removeDay(day.key) }) { Icon(Icons.Outlined.Delete, "Günü sil") }
                    }
                    BranchDivider(Modifier.padding(vertical = 8.dp))
                    day.exercises.forEach { ex ->
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(ex.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.removeExercise(day.key, ex.key) }) { Icon(Icons.Outlined.Close, "Egzersizi çıkar") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NumberField(ex.sets, { v -> viewModel.updateExercise(day.key, ex.key) { it.copy(sets = v) } }, "Set", Modifier.weight(1f), decimal = false)
                                NumberField(ex.repsMin, { v -> viewModel.updateExercise(day.key, ex.key) { it.copy(repsMin = v) } }, "Tekrar min", Modifier.weight(1f), decimal = false)
                                NumberField(ex.repsMax, { v -> viewModel.updateExercise(day.key, ex.key) { it.copy(repsMax = v) } }, "max", Modifier.weight(1f), decimal = false)
                                NumberField(ex.weightKg, { v -> viewModel.updateExercise(day.key, ex.key) { it.copy(weightKg = v) } }, "kg", Modifier.weight(1f))
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    OutlinedButton(onClick = { pickerFor = day.key }, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Egzersiz ekle")
                    }
                }
            }
            item {
                OutlinedButton(onClick = viewModel::addDay, modifier = Modifier.fillMaxWidth()) { Text("Gün ekle") }
            }
            state.error?.let { err ->
                item { Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
            item {
                Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.saving) "Kaydediliyor…" else "Programı kaydet")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisePickerDialog(
    catalog: List<Exercise>,
    muscles: List<MuscleGroup>,
    onDismiss: () -> Unit,
    onPick: (String, Map<String, MuscleRole>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var customMode by remember { mutableStateOf(false) }
    val chosen = remember { mutableStateMapOf<String, MuscleRole>() }
    val filtered = catalog.filter { it.name.contains(query.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customMode) "Yeni egzersiz: ${query.trim()}" else "Egzersiz seç") },
        text = {
            if (!customMode) {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(60) },
                        label = { Text("Ara veya yeni ad yaz") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(Modifier.heightIn(max = 320.dp).padding(top = 8.dp)) {
                        items(filtered, key = { it.id }) { ex ->
                            Column(
                                Modifier.fillMaxWidth().clickable { onPick(ex.name, emptyMap()) }.padding(vertical = 8.dp),
                            ) {
                                Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    ex.muscles.filter { it.role == MuscleRole.PRIMARY }.joinToString { it.muscle.displayName },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    val exact = catalog.any { it.name.equals(query.trim(), ignoreCase = true) }
                    if (query.isNotBlank() && !exact) {
                        TextButton(onClick = { customMode = true }) { Text("“${query.trim()}” adında yeni egzersiz oluştur") }
                    }
                }
            } else {
                Column {
                    Text("Çalıştırdığı kasları seç (1. dokunuş birincil, 2. ikincil, 3. kaldır)", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                        muscles.forEach { m ->
                            val role = chosen[m.code]
                            FilterChip(
                                selected = role != null,
                                onClick = {
                                    when (role) {
                                        null -> chosen[m.code] = MuscleRole.PRIMARY
                                        MuscleRole.PRIMARY -> chosen[m.code] = MuscleRole.SECONDARY
                                        MuscleRole.SECONDARY -> chosen.remove(m.code)
                                    }
                                },
                                label = {
                                    Text(
                                        m.displayName + when (role) {
                                            MuscleRole.PRIMARY -> " ●"
                                            MuscleRole.SECONDARY -> " ○"
                                            null -> ""
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (customMode) {
                TextButton(onClick = { onPick(query.trim(), chosen.toMap()) }, enabled = chosen.isNotEmpty()) { Text("Ekle") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
    )
}
