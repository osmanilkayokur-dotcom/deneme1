package dev.kok.app.presentation.feature.dashboard

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kok.app.domain.model.FoodCategory
import dev.kok.app.domain.model.Meal
import dev.kok.app.domain.usecase.DailyOverview
import dev.kok.app.presentation.components.BranchDivider
import dev.kok.app.presentation.components.ComparisonBars
import dev.kok.app.presentation.components.ComparisonItem
import dev.kok.app.presentation.components.GoalBar
import dev.kok.app.presentation.components.LeafBadge
import dev.kok.app.presentation.components.PaperCard
import dev.kok.app.presentation.components.SkyHeader
import dev.kok.app.presentation.components.animation.FoodOriginAnimation
import dev.kok.app.presentation.components.formatAmount
import dev.kok.app.presentation.feature.nutrition.WaterCard
import dev.kok.app.presentation.theme.KokTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val TR = Locale.forLanguageTag("tr-TR")
private val DATE_FMT = DateTimeFormatter.ofPattern("d MMMM, EEEE", TR)

@Composable
fun DashboardRoute(
    onAddMeal: (LocalDate) -> Unit,
    onOpenProfile: () -> Unit,
    mealAnimation: FoodCategory? = null,
    onMealAnimationDone: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var waterPulse by remember { mutableStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DashboardEvent.MealDeleted -> {
                    val result = snackbar.showSnackbar(
                        message = "${event.meal.name} silindi",
                        actionLabel = "Geri al",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.meal)
                }
                is DashboardEvent.WaterAdded -> if (event.deltaMl > 0) waterPulse++
                is DashboardEvent.Error -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            val ready = state as? DashboardUiState.Ready
            if (ready?.overview?.profile != null) {
                ExtendedFloatingActionButton(
                    onClick = { onAddMeal(ready.date) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Yemek ekle") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        mealAnimation?.let { category ->
            MealAnimationOverlay(category, onMealAnimationDone)
        }
        when (val s = state) {
            DashboardUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is DashboardUiState.Ready -> DashboardContent(
                state = s,
                contentPadding = padding,
                waterPulse = waterPulse,
                onPrev = viewModel::previousDay,
                onNext = viewModel::nextDay,
                onToday = viewModel::goToToday,
                onAddWater = { viewModel.addWater(it) },
                onDeleteMeal = { viewModel.delete(it) },
                onOpenProfile = onOpenProfile,
            )
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Ready,
    contentPadding: PaddingValues,
    waterPulse: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onAddWater: (Int) -> Unit,
    onDeleteMeal: (Meal) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val overview = state.overview
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header(state, onPrev, onNext, onToday) }

        val profile = overview.profile
        if (profile == null) {
            item { ProfilePrompt(onOpenProfile, Modifier.padding(horizontal = 16.dp)) }
            return@LazyColumn
        }

        item { CalorieCard(overview, Modifier.padding(horizontal = 16.dp)) }
        item { MacroCard(overview, Modifier.padding(horizontal = 16.dp)) }
        item {
            WaterCard(
                currentMl = overview.day.waterMl,
                goalMl = profile.goals.waterMl,
                pulse = waterPulse,
                onAdd = onAddWater,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item { MicroCard(overview, Modifier.padding(horizontal = 16.dp)) }
        item {
            Text(
                "Öğünler",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        if (overview.day.meals.isEmpty()) {
            item {
                Text(
                    "Bu gün için henüz yemek kaydı yok.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        } else {
            items(overview.day.meals, key = { it.id }) { meal ->
                MealRow(meal, onDelete = { onDeleteMeal(meal) }, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun Header(state: DashboardUiState.Ready, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Günaydın"
        in 12..17 -> "İyi günler"
        in 18..22 -> "İyi akşamlar"
        else -> "İyi geceler"
    }
    SkyHeader {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 36.dp),
        ) {
            Text(
                if (state.isToday) greeting else "Geçmiş gün",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.date.format(DATE_FMT),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onPrev) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki gün")
                }
                IconButton(onClick = onNext, enabled = !state.isToday) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki gün")
                }
            }
            if (!state.isToday) {
                TextButton(onClick = onToday, contentPadding = PaddingValues(0.dp)) { Text("Bugüne dön") }
            }
        }
    }
}

@Composable
private fun ProfilePrompt(onOpenProfile: () -> Unit, modifier: Modifier = Modifier) {
    PaperCard(modifier) {
        Text("Önce seni tanıyalım", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Bazal metabolizma ve günlük ihtiyacını hesaplayabilmem için yaş, boy, kilo ve aktivite düzeyini gir.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        FilledTonalButton(onClick = onOpenProfile) { Text("Profili oluştur") }
    }
}

@Composable
private fun CalorieCard(overview: DailyOverview, modifier: Modifier = Modifier) {
    val nature = KokTheme.nature
    val intake = overview.day.totals.kcal
    val tdee = overview.tdee ?: 0.0
    val remaining = (tdee - intake).roundToInt()
    PaperCard(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Enerji", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            LeafBadge(
                if (remaining >= 0) "$remaining kcal kaldı" else "${-remaining} kcal fazla",
                container = if (remaining >= 0) MaterialTheme.colorScheme.primaryContainer else nature.clayLight,
            )
        }
        BranchDivider(Modifier.padding(vertical = 8.dp))
        ComparisonBars(
            listOf(
                ComparisonItem(
                    "Bazal metabolizma (BMR)",
                    overview.bmr ?: 0.0,
                    nature.root,
                    nature.trackEmpty,
                    "Dinlenirken harcadığın enerji",
                ),
                ComparisonItem(
                    "Sporla günlük ihtiyaç (TDEE)",
                    tdee,
                    nature.leaf,
                    nature.trackEmpty,
                    overview.profile?.activityLevel?.label,
                ),
                ComparisonItem(
                    "Bugün alınan",
                    intake,
                    if (intake > tdee && tdee > 0) nature.clay else nature.water,
                    nature.trackEmpty,
                    if (overview.day.burnedKcal > 0) "Antrenmanla yakılan: ${overview.day.burnedKcal} kcal" else null,
                ),
            ),
        )
    }
}

@Composable
private fun MacroCard(overview: DailyOverview, modifier: Modifier = Modifier) {
    val goals = overview.profile?.goals ?: return
    val t = overview.day.totals
    val nature = KokTheme.nature
    PaperCard(modifier) {
        Text("Makrolar", style = MaterialTheme.typography.titleLarge)
        BranchDivider(Modifier.padding(vertical = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GoalBar("Protein", t.proteinG, goals.proteinG, "g", nature.protein)
            GoalBar("Karbonhidrat", t.carbG, goals.carbG, "g", nature.carb)
            GoalBar("Yağ", t.fatG, goals.fatG, "g", nature.fat)
            GoalBar("Lif", t.fiberG, goals.fiberG, "g", nature.fiber)
        }
    }
}

@Composable
private fun MicroCard(overview: DailyOverview, modifier: Modifier = Modifier) {
    val goals = overview.profile?.goals?.micros ?: return
    val m = overview.day.totals.micros
    val c = KokTheme.nature.micro
    var expanded by remember { mutableStateOf(false) }
    PaperCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mikro besinler", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Gizle" else "Tümü") }
        }
        BranchDivider(Modifier.padding(vertical = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GoalBar("Sodyum (üst sınır)", m.sodiumMg, goals.sodiumMg, "mg", c, isLimit = true)
            GoalBar("Potasyum", m.potassiumMg, goals.potassiumMg, "mg", c)
            if (expanded) {
                GoalBar("Demir", m.ironMg, goals.ironMg, "mg", c)
                GoalBar("C vitamini", m.vitaminCMg, goals.vitaminCMg, "mg", c)
                GoalBar("D vitamini", m.vitaminDMcg, goals.vitaminDMcg, "mcg", c)
            }
        }
    }
}

@Composable
private fun MealRow(meal: Meal, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    PaperCard(modifier, shape = MaterialTheme.shapes.medium, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(meal.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    LeafBadge(meal.category.label, container = MaterialTheme.colorScheme.secondaryContainer, content = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${formatAmount(meal.grams)} g · ${meal.nutrients.kcal.roundToInt()} kcal · " +
                        "P ${formatAmount(meal.nutrients.proteinG)} · K ${formatAmount(meal.nutrients.carbG)} · Y ${formatAmount(meal.nutrients.fatG)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = "${meal.name} sil")
            }
        }
    }
}

/** Yemek eklendikten sonra ekranın üstünde kısa süre oynayan doğa kökeni animasyonu. */
@Composable
fun MealAnimationOverlay(category: FoodCategory, onDone: () -> Unit) {
    val caption = when (category) {
        FoodCategory.GRAIN_RICE -> "Çeltikten kaseye"
        FoodCategory.VEGETABLE -> "Topraktan filizlendi"
        FoodCategory.FRUIT -> "Dalından düştü"
        FoodCategory.PROTEIN -> "Kabuğundan çıktı"
        FoodCategory.DAIRY -> "Kovadan kaseye"
        FoodCategory.OTHER -> "Rüzgârla geldi"
    }
    androidx.compose.ui.window.Popup(alignment = Alignment.Center) {
        Column(
            Modifier
                .size(220.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FoodOriginAnimation(
                category = category,
                colors = KokTheme.nature,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onFinished = onDone,
            )
            Text(caption, style = MaterialTheme.typography.titleMedium)
        }
    }
}
