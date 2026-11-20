package dev.kok.app.presentation.feature.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kok.app.domain.model.BodyMeasurement
import dev.kok.app.domain.usecase.MeasurementSchedule
import dev.kok.app.presentation.components.BranchDivider
import dev.kok.app.presentation.components.NumberField
import dev.kok.app.presentation.components.PaperCard
import dev.kok.app.presentation.components.formatAmount
import dev.kok.app.presentation.theme.KokTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")
private val MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", TR)
private val DAY_FMT = DateTimeFormatter.ofPattern("d MMMM", TR)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyRoute(onOpenProfile: () -> Unit, viewModel: BodyViewModel = hiltViewModel()) {
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    mode?.let { m ->
        MeasurementDialog(
            mode = m,
            form = form,
            error = error,
            onChange = viewModel::update,
            onDismiss = viewModel::closeForm,
            onSubmit = viewModel::submit,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Vücut") },
                actions = {
                    TextButton(onClick = onOpenProfile) {
                        Icon(Icons.Outlined.Tune, contentDescription = null)
                        Text(" Profil ve hedefler")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        val s = schedule
        if (s == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { MonthCard(s, onOpenForm = viewModel::openForm) }
            if (s.history.isNotEmpty()) {
                item { Text("Ölçüm geçmişi", style = MaterialTheme.typography.headlineSmall) }
                itemsIndexed(s.history, key = { _, m -> m.id }) { index, m ->
                    HistoryRow(m, previous = s.history.getOrNull(index + 1))
                }
            }
        }
    }
}

@Composable
private fun MonthCard(s: MeasurementSchedule, onOpenForm: () -> Unit) {
    PaperCard {
        Text(s.currentMonth.format(MONTH_FMT).replaceFirstChar { it.titlecase(TR) }, style = MaterialTheme.typography.titleLarge)
        BranchDivider(Modifier.padding(vertical = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (s.isLocked) {
                MoonPhase(s.monthProgress, Modifier.size(84.dp))
            } else {
                TreeRings(s.history.size, Modifier.size(84.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                if (s.isLocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Bu ayın ölçümü alındı", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        "Yeni ölçüm ${s.daysUntilNext} gün sonra (${s.nextOpenDate.format(DAY_FMT)}) açılacak. Ay dolunaydan yeni aya dönerken bir sonraki halka eklenecek.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("Ölçüm zamanı", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Her ay tek ölçüm girilir; her kayıt gövdene yeni bir halka ekler. Şu ana kadar ${s.history.size} halka.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (s.isLocked) {
            OutlinedButton(onClick = onOpenForm) { Text("Bu ayın kaydını düzelt") }
        } else {
            Button(onClick = onOpenForm) { Text("Bu ayın ölçümünü gir") }
        }
    }
}

@Composable
private fun HistoryRow(m: BodyMeasurement, previous: BodyMeasurement?) {
    PaperCard(shape = MaterialTheme.shapes.medium, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                m.yearMonth.format(MONTH_FMT).replaceFirstChar { it.titlecase(TR) },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text("${formatAmount(m.weightKg)} kg", style = MaterialTheme.typography.titleMedium)
            previous?.let { p -> Delta(m.weightKg - p.weightKg, "kg", lowerIsBetter = null) }
        }
        val parts = listOfNotNull(
            m.bodyFatPercent?.let { "Yağ %${formatAmount(it)}" },
            m.chestCm?.let { "Göğüs ${formatAmount(it)}" },
            m.waistCm?.let { "Bel ${formatAmount(it)}" },
            m.armCm?.let { "Kol ${formatAmount(it)}" },
            m.legCm?.let { "Bacak ${formatAmount(it)}" },
            m.shoulderCm?.let { "Omuz ${formatAmount(it)}" },
            m.neckCm?.let { "Boyun ${formatAmount(it)}" },
        )
        if (parts.isNotEmpty()) {
            Text(
                parts.joinToString(" · ") + " cm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Delta(value: Double, unit: String, lowerIsBetter: Boolean?) {
    if (value == 0.0) return
    val sign = if (value > 0) "+" else "−"
    val color = when (lowerIsBetter) {
        null -> MaterialTheme.colorScheme.onSurfaceVariant
        true -> if (value < 0) KokTheme.nature.leaf else KokTheme.nature.clay
        false -> if (value > 0) KokTheme.nature.leaf else KokTheme.nature.clay
    }
    Text("  $sign${formatAmount(kotlin.math.abs(value))} $unit", style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
private fun MeasurementDialog(
    mode: FormMode,
    form: MeasurementForm,
    error: String?,
    onChange: ((MeasurementForm) -> MeasurementForm) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == FormMode.NEW) "Aylık ölçüm" else "Bu ayın ölçümünü düzelt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(form.weight, { v -> onChange { it.copy(weight = v) } }, "Kilo*", Modifier.weight(1f), suffix = "kg")
                    NumberField(form.fat, { v -> onChange { it.copy(fat = v) } }, "Yağ", Modifier.weight(1f), suffix = "%")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(form.chest, { v -> onChange { it.copy(chest = v) } }, "Göğüs", Modifier.weight(1f), suffix = "cm")
                    NumberField(form.waist, { v -> onChange { it.copy(waist = v) } }, "Bel", Modifier.weight(1f), suffix = "cm")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(form.arm, { v -> onChange { it.copy(arm = v) } }, "Kol", Modifier.weight(1f), suffix = "cm")
                    NumberField(form.leg, { v -> onChange { it.copy(leg = v) } }, "Bacak", Modifier.weight(1f), suffix = "cm")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(form.shoulder, { v -> onChange { it.copy(shoulder = v) } }, "Omuz", Modifier.weight(1f), suffix = "cm")
                    NumberField(form.neck, { v -> onChange { it.copy(neck = v) } }, "Boyun", Modifier.weight(1f), suffix = "cm")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onSubmit) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
