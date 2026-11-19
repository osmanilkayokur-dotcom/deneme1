package dev.kok.app.presentation.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kok.app.domain.model.EntrySource
import dev.kok.app.domain.model.WorkoutProgram
import dev.kok.app.domain.usecase.TrainingToday
import dev.kok.app.presentation.components.BranchDivider
import dev.kok.app.presentation.components.LeafBadge
import dev.kok.app.presentation.components.PaperCard
import dev.kok.app.presentation.components.formatAmount
import dev.kok.app.presentation.feature.musclemap.MuscleMapPanel
import dev.kok.app.presentation.theme.KokTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutRoute(
    onEditProgram: (Long?) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showLog by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<WorkoutProgram?>(null) }

    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    if (showLog) {
        LogSessionDialog(
            onDismiss = { showLog = false },
            onConfirm = { minutes, intensity, notes ->
                viewModel.logSession(minutes, intensity, notes)
                showLog = false
            },
        )
    }
    deleteTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Programı sil") },
            text = { Text("'${p.name}' ve tüm günleri silinecek. Geçmiş antrenman kayıtların korunur.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(p.id); deleteTarget = null }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Vazgeç") } },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Spor") },
                actions = {
                    TextButton(onClick = { onEditProgram(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Program")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        val today = state.today
        if (state.loading || today == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val program = today.program
            if (program == null) {
                item { EmptyPrograms(onCreate = { onEditProgram(null) }) }
            } else {
                item { TodayCard(today, program, onSelectDay = viewModel::selectDay, onEdit = { onEditProgram(program.id) }, onLog = { showLog = true }) }
                item {
                    PaperCard {
                        Text("Kas haritası", style = MaterialTheme.typography.titleLarge)
                        BranchDivider(Modifier.padding(vertical = 8.dp))
                        MuscleMapPanel(today.highlight, today.muscles)
                    }
                }
            }
            val others = state.programs.filter { !it.isActive }
            if (others.isNotEmpty()) {
                item { Text("Diğer programlar", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 4.dp)) }
                items(others, key = { it.id }) { p ->
                    PaperCard(shape = MaterialTheme.shapes.medium, contentPadding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium)
                                Text("${p.days.size} gün", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { viewModel.activate(p.id) }) { Text("Aktif yap") }
                            IconButton(onClick = { onEditProgram(p.id) }) { Icon(Icons.Outlined.Edit, "Düzenle") }
                            IconButton(onClick = { deleteTarget = p }) { Icon(Icons.Outlined.Delete, "Sil") }
                        }
                    }
                }
            }
            program?.let { p ->
                item {
                    TextButton(onClick = { deleteTarget = p }) {
                        Text("Aktif programı sil", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPrograms(onCreate: () -> Unit) {
    PaperCard {
        Text("Henüz program yok", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Programını elle oluşturabilir ya da Asistan sekmesinde “Bana 4 günlük Upper/Lower programı hazırla” diyebilirsin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onCreate) { Text("Program oluştur") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayCard(
    today: TrainingToday,
    program: WorkoutProgram,
    onSelectDay: (Long) -> Unit,
    onEdit: () -> Unit,
    onLog: () -> Unit,
) {
    PaperCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Aktif program", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(program.name, style = MaterialTheme.typography.headlineSmall)
            }
            if (program.source == EntrySource.GEMINI_CHAT) LeafBadge("Asistan")
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Programı düzenle") }
        }
        BranchDivider(Modifier.padding(vertical = 8.dp))
        Text("Bugünün antrenmanı", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            program.days.forEach { d ->
                FilterChip(
                    selected = today.selectedDay?.id == d.id,
                    onClick = { onSelectDay(d.id) },
                    label = { Text(d.name) },
                )
            }
        }
        if (today.isSuggested) {
            Text(
                "Sıradaki gün önerildi; farklı bir gün seçebilirsin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val day = today.selectedDay
        if (day != null) {
            Spacer(Modifier.height(8.dp))
            day.exercises.forEach { pe ->
                val reps = if (pe.targetRepsMin == pe.targetRepsMax) "${pe.targetRepsMin}" else "${pe.targetRepsMin}–${pe.targetRepsMax}"
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    val primary = pe.exercise.muscles.any { it.role == dev.kok.app.domain.model.MuscleRole.PRIMARY }
                    Box(
                        Modifier.size(8.dp).background(
                            if (primary) KokTheme.nature.clay else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pe.exercise.name, style = MaterialTheme.typography.bodyLarge)
                        val muscles = pe.exercise.muscles.joinToString { it.muscle.displayName }
                        if (muscles.isNotBlank()) {
                            Text(muscles, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        "${pe.targetSets}×$reps" + (pe.targetWeightKg?.let { " · ${formatAmount(it)} kg" } ?: ""),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onLog, modifier = Modifier.fillMaxWidth()) { Text("Antrenmanı tamamladım") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogSessionDialog(onDismiss: () -> Unit, onConfirm: (Int, Intensity, String?) -> Unit) {
    var minutes by remember { mutableFloatStateOf(60f) }
    var intensity by remember { mutableStateOf(Intensity.MODERATE) }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Antrenmanı kaydet") },
        text = {
            Column {
                Text("Süre: ${minutes.roundToInt()} dk", style = MaterialTheme.typography.titleSmall)
                Slider(value = minutes, onValueChange = { minutes = it }, valueRange = 10f..180f, steps = 33)
                Text("Yoğunluk", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Intensity.entries.forEachIndexed { i, it ->
                        SegmentedButton(
                            selected = intensity == it,
                            onClick = { intensity = it },
                            shape = SegmentedButtonDefaults.itemShape(i, Intensity.entries.size),
                        ) { Text(it.label) }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(300) },
                    label = { Text("Not (isteğe bağlı)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes.roundToInt(), intensity, notes.ifBlank { null }) }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
