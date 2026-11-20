package dev.kok.app.presentation.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kok.app.domain.model.AssistantAction
import dev.kok.app.domain.model.ChatRole
import dev.kok.app.presentation.components.BranchDivider
import dev.kok.app.presentation.components.LeafBadge
import dev.kok.app.presentation.components.PaperCard
import dev.kok.app.presentation.components.formatAmount
import dev.kok.app.presentation.feature.dashboard.MealAnimationOverlay
import dev.kok.app.presentation.theme.KokTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoute(onOpenSettings: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.items.size, state.busy) {
        val count = state.items.size + if (state.busy) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    state.mealAnimation?.let { MealAnimationOverlay(it, viewModel::mealAnimationDone) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Sohbeti temizle") },
            text = { Text("Tüm sohbet geçmişi silinecek. Kaydedilmiş yemek ve programlar etkilenmez.") },
            confirmButton = { TextButton(onClick = { viewModel.clear(); confirmClear = false }) { Text("Temizle") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Vazgeç") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asistan") },
                actions = {
                    IconButton(onClick = { confirmClear = true }, enabled = state.items.isNotEmpty()) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Sohbeti temizle")
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Ayarlar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
            if (!state.configured) {
                PaperCard(Modifier.padding(16.dp)) {
                    Text("Gemini bağlantısı yok", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Yemekleri ve programları sohbetle kaydedebilmek için ücretsiz bir Gemini API anahtarı gir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onOpenSettings) { Text("Ayarlara git") }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.items.isEmpty() && state.configured) {
                    item { EmptyHint() }
                }
                items(state.items, key = { it.message.id }) { item ->
                    when {
                        item.message.role == ChatRole.USER -> UserBubble(item.message.text)
                        item.action != null -> ActionCard(
                            item = item,
                            busy = state.busy,
                            onConfirm = { viewModel.confirm(item) },
                            onReject = { viewModel.reject(item) },
                            onUndo = { viewModel.undo(item) },
                        )
                        item.message.text.isNotBlank() -> ModelBubble(item.message.text)
                    }
                }
                if (state.busy) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(6.dp)) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("  Düşünüyor…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            state.error?.let { err ->
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                    if (state.canRetry) TextButton(onClick = viewModel::retry) { Text("Tekrar dene") }
                    TextButton(onClick = viewModel::dismissError) { Text("Kapat") }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(2000) },
                    placeholder = { Text("Ör: 200 g tavuk ve 1 tabak pilav yedim") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.size(8.dp))
                FilledIconButton(
                    onClick = { viewModel.send(input); input = "" },
                    enabled = input.isNotBlank() && !state.busy && state.configured,
                ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder") }
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    PaperCard {
        Text("Neler yapabilirim?", style = MaterialTheme.typography.titleLarge)
        BranchDivider(Modifier.padding(vertical = 6.dp))
        listOf(
            "“Kahvaltıda 2 yumurta, 1 dilim peynir ve 2 dilim ekmek yedim.”",
            "“Bana haftada 4 gün Upper/Lower programı hazırla.”",
            "“PPL programında bacak gününe hip thrust ekle.”",
        ).forEach {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 3.dp))
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ModelBubble(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ActionCard(
    item: ChatItem,
    busy: Boolean,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onUndo: () -> Unit,
) {
    val action = item.action ?: return
    Column {
        if (item.message.text.isNotBlank()) {
            ModelBubble(item.message.text)
            Spacer(Modifier.height(6.dp))
        }
        PaperCard(shape = MaterialTheme.shapes.medium, contentPadding = 14.dp) {
            when (action) {
                is AssistantAction.LogMeals -> MealProposal(action)
                is AssistantAction.UpsertProgram -> ProgramProposal(action)
            }
            Spacer(Modifier.height(10.dp))
            when (item.status) {
                ActionStatus.PENDING -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onConfirm, enabled = !busy) { Text("Onayla ve kaydet") }
                    OutlinedButton(onClick = onReject, enabled = !busy) { Text("Vazgeç") }
                }
                ActionStatus.RESOLVED -> Row(verticalAlignment = Alignment.CenterVertically) {
                    LeafBadge(item.message.actionSummary.orEmpty(), modifier = Modifier.weight(1f, fill = false))
                    if (item.message.undoRef != null) {
                        TextButton(onClick = onUndo, enabled = !busy) { Text("Geri al") }
                    }
                }
                ActionStatus.EXPIRED -> Text(
                    "Bu öneri yanıtlanmadı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                null -> Unit
            }
        }
    }
}

@Composable
private fun MealProposal(action: AssistantAction.LogMeals) {
    val nature = KokTheme.nature
    Text("Yemek kaydı önerisi", style = MaterialTheme.typography.titleMedium)
    BranchDivider(Modifier.padding(vertical = 6.dp))
    action.meals.forEach { m ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Column(Modifier.weight(1f)) {
                Text("${m.name} · ${formatAmount(m.grams)} g", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "P ${formatAmount(m.nutrients.proteinG)} · K ${formatAmount(m.nutrients.carbG)} · Y ${formatAmount(m.nutrients.fatG)} · Lif ${formatAmount(m.nutrients.fiberG)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${m.nutrients.kcal.roundToInt()} kcal", style = MaterialTheme.typography.labelLarge, color = nature.clay)
        }
    }
    val total = action.meals.sumOf { it.nutrients.kcal }.roundToInt()
    val protein = action.meals.sumOf { it.nutrients.proteinG }
    Text(
        "Toplam: $total kcal · ${formatAmount(protein)} g protein" +
            (action.meals.firstOrNull()?.date?.let { " · $it" } ?: ""),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun ProgramProposal(action: AssistantAction.UpsertProgram) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(action.draft.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action.activate) LeafBadge("Aktif yapılacak")
    }
    BranchDivider(Modifier.padding(vertical = 6.dp))
    action.draft.days.forEach { day ->
        Text(day.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
        day.exercises.forEach { e ->
            val reps = if (e.repsMin == e.repsMax) "${e.repsMin}" else "${e.repsMin}–${e.repsMax}"
            Text(
                "• ${e.name}  ${e.sets}×$reps" + (e.weightKg?.let { " @ ${formatAmount(it)} kg" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
