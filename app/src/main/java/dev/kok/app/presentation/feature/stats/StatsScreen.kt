package dev.kok.app.presentation.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kok.app.domain.usecase.PeriodStats
import dev.kok.app.domain.usecase.StatsPeriod
import dev.kok.app.presentation.components.BranchDivider
import dev.kok.app.presentation.components.PaperCard
import dev.kok.app.presentation.components.formatAmount
import dev.kok.app.presentation.theme.KokTheme
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val TR = Locale.forLanguageTag("tr-TR")

private val labels = mapOf(
    StatsPeriod.THIS_WEEK to "Bu hafta",
    StatsPeriod.LAST_WEEK to "Geçen hafta",
    StatsPeriod.THIS_MONTH to "Bu ay",
    StatsPeriod.LAST_MONTH to "Geçen ay",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsRoute(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Özet") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatsPeriod.entries.forEach { p ->
                    FilterChip(selected = selected == p, onClick = { viewModel.select(p) }, label = { Text(labels.getValue(p)) })
                }
            }
            val stats = state.stats
            if (stats == null || stats.period != selected) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Overview(stats)
                MetaphorCard(stats)
                KcalChart(stats, state.kcalGoal)
            }
        }
    }
}

@Composable
private fun Overview(s: PeriodStats) {
    val range = "${s.start.format(DateTimeFormatter.ofPattern("d MMM", TR))} – ${s.end.format(DateTimeFormatter.ofPattern("d MMM", TR))}"
    PaperCard {
        Text(range, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${s.loggedDays} gün kayıt", style = MaterialTheme.typography.headlineSmall)
        BranchDivider(Modifier.padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Ort. kalori", "${s.avgKcal.roundToInt()}", "kcal", Modifier.weight(1f))
            Stat("Ort. protein", formatAmount(s.avgProteinG), "g", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Yemek", formatAmount(s.totalFoodKg), "kg", Modifier.weight(1f))
            Stat("Su", formatAmount(s.totalWaterL), "L", Modifier.weight(1f))
            Stat("Yakılan", "${s.totalBurnedKcal.roundToInt()}", "kcal", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(" $unit", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun MetaphorCard(s: PeriodStats) {
    PaperCard {
        Text("Somut karşılıklar", style = MaterialTheme.typography.titleLarge)
        BranchDivider(Modifier.padding(vertical = 8.dp))
        if (s.metaphors.isEmpty()) {
            Text(
                "Bu dönem için henüz kayıt yok. Yemek, su ve antrenman ekledikçe burada gerçek hayattaki karşılıkları görünecek.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            s.metaphors.forEach { m ->
                Text(
                    m.headline,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = dev.kok.app.presentation.theme.Fraunces),
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }
}

/** Günlük kalori sütunları + hedef çizgisi. */
@Composable
private fun KcalChart(s: PeriodStats, goal: Double?) {
    val nature = KokTheme.nature
    val max = maxOf(s.points.maxOfOrNull { it.kcal } ?: 0.0, goal ?: 0.0, 1.0)
    val desc = s.points.joinToString { "${it.date.dayOfMonth}: ${it.kcal.roundToInt()} kcal" }
    PaperCard {
        Text("Günlük kalori", style = MaterialTheme.typography.titleLarge)
        BranchDivider(Modifier.padding(vertical = 8.dp))
        Canvas(Modifier.fillMaxWidth().height(160.dp).semantics { contentDescription = desc }) {
            val n = s.points.size.coerceAtLeast(1)
            val slot = size.width / n
            val barW = (slot * 0.62f).coerceAtMost(28.dp.toPx())
            s.points.forEachIndexed { i, p ->
                val h = (p.kcal / max).toFloat() * size.height
                val x = i * slot + (slot - barW) / 2f
                drawRoundRect(
                    color = if (goal != null && p.kcal > goal * 1.1) nature.clay else nature.leaf,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 2.5f),
                )
            }
            goal?.let {
                val y = size.height - (it / max).toFloat() * size.height
                drawLine(
                    nature.bark.copy(alpha = 0.6f),
                    Offset(0f, y),
                    Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                )
            }
        }
        if (s.points.size <= 7) {
            Row(Modifier.fillMaxWidth()) {
                s.points.forEach { p ->
                    Text(
                        p.date.dayOfWeek.getDisplayName(TextStyle.SHORT, TR),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
        goal?.let {
            Text(
                "Kesik çizgi: makro hedeflerinden gelen günlük kalori (${it.roundToInt()} kcal)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
