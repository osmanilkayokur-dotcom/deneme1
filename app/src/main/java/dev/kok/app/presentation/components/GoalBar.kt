package dev.kok.app.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.kok.app.presentation.theme.KokTheme
import kotlin.math.roundToInt

/**
 * Hedefe göre dolan yatay bar. %100'ü geçince taşan kısım kil turuncusu ile gösterilir;
 * [isLimit] true ise (ör. sodyum üst sınırı) hedefi aşmak uyarı rengine döner.
 */
@Composable
fun GoalBar(
    label: String,
    value: Double,
    goal: Double,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
    isLimit: Boolean = false,
    barHeight: Dp = 10.dp,
) {
    val ratio = if (goal > 0) (value / goal).toFloat() else 0f
    val animated by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1.5f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "goalBar",
    )
    val percent = (ratio * 100).roundToInt()
    val nature = KokTheme.nature
    val overColor = if (isLimit) MaterialTheme.colorScheme.error else nature.clay
    val trackColor = nature.trackEmpty

    Column(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label: ${formatAmount(value)} / ${formatAmount(goal)} $unit, yüzde $percent" },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(
                "${formatAmount(value)} / ${formatAmount(goal)} $unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "  %$percent",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (ratio > 1f && isLimit) overColor else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.fillMaxWidth().height(barHeight)) {
            val r = CornerRadius(size.height / 2f, size.height / 2f)
            drawRoundRect(trackColor, cornerRadius = r)
            // Ölçek: 0..max(1, oran). Hedefe kadar olan kısım ana renk, aşan kısım uyarı/kil rengi.
            val scale = maxOf(1f, animated)
            val goalX = size.width / scale
            val fillX = size.width * (animated / scale)
            if (fillX > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color), endX = goalX),
                    size = Size(minOf(fillX, goalX), size.height),
                    cornerRadius = r,
                )
            }
            if (animated > 1f) {
                drawRoundRect(
                    color = overColor,
                    topLeft = Offset(goalX - size.height / 2f, 0f),
                    size = Size(fillX - goalX + size.height / 2f, size.height),
                    cornerRadius = r,
                )
                drawLine(
                    color = trackColor,
                    start = Offset(goalX, 0f),
                    end = Offset(goalX, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            // Çeyrek işaretleri: kök boğumu gibi ince çentikler
            listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                val x = size.width * f
                drawLine(
                    color = Color.Black.copy(alpha = 0.06f),
                    start = Offset(x, size.height * 0.25f),
                    end = Offset(x, size.height * 0.75f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
    }
}

/** Kalori karşılaştırması: BMR, günlük ihtiyaç ve alınan kalori aynı ölçekte. */
@Composable
fun ComparisonBars(
    items: List<ComparisonItem>,
    modifier: Modifier = Modifier,
) {
    val maxValue = (items.maxOfOrNull { it.value } ?: 0.0).coerceAtLeast(1.0)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            val target = (item.value / maxValue).toFloat().coerceIn(0f, 1f)
            val animated by animateFloatAsState(
                targetValue = target,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "cmp",
            )
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(item.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text(
                        "${item.value.roundToInt()} kcal",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                    val r = CornerRadius(size.height / 2f, size.height / 2f)
                    drawRoundRect(item.trackColor, cornerRadius = r)
                    if (animated > 0f) {
                        drawRoundRect(item.color, size = Size(size.width * animated, size.height), cornerRadius = r)
                    }
                }
                item.caption?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

data class ComparisonItem(
    val label: String,
    val value: Double,
    val color: Color,
    val trackColor: Color,
    val caption: String? = null,
)

/** Türkçe sayı biçimi: 12,5 / 1.250 */
fun formatAmount(v: Double): String {
    val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("tr-TR"))
    nf.maximumFractionDigits = if (v >= 100 || v == v.roundToInt().toDouble()) 0 else 1
    return nf.format(v)
}
