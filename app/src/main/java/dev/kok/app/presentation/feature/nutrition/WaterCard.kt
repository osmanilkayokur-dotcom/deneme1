package dev.kok.app.presentation.feature.nutrition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import dev.kok.app.presentation.components.BranchDivider
import dev.kok.app.presentation.components.PaperCard
import dev.kok.app.presentation.components.animation.RainCloud
import dev.kok.app.presentation.components.formatAmount
import dev.kok.app.presentation.theme.KokTheme
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Su takibi kartı: testi biçimli kap, seviye dalgalanarak yükselir.
 * [pulse] her su eklemede artar: kabın üstünde bulut belirir, yağmur yağar ve su dalgalanarak yükselir.
 */
@Composable
fun WaterCard(
    currentMl: Int,
    goalMl: Int,
    pulse: Int,
    onAdd: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratio = if (goalMl > 0) currentMl.toFloat() / goalMl else 0f
    PaperCard(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Su", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                "%${(ratio * 100).roundToInt()}",
                style = MaterialTheme.typography.titleMedium,
                color = KokTheme.nature.water,
            )
        }
        BranchDivider(Modifier.height(14.dp))
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 84.dp, height = 140.dp)) {
                WaterJar(
                    ratio = ratio,
                    pulse = pulse,
                    modifier = Modifier.align(Alignment.BottomCenter).size(width = 76.dp, height = 104.dp),
                )
                RainCloud(
                    trigger = pulse,
                    cloud = MaterialTheme.colorScheme.surfaceContainerHighest,
                    drop = KokTheme.nature.water,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${formatAmount(currentMl / 1000.0)} L / ${formatAmount(goalMl / 1000.0)} L",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onAdd(250) }) { Text("+250 ml") }
                    FilledTonalButton(onClick = { onAdd(500) }) { Text("+500") }
                }
                OutlinedButton(onClick = { onAdd(-250) }, enabled = currentMl > 0) { Text("−250 ml") }
            }
        }
    }
}

@Composable
private fun WaterJar(ratio: Float, pulse: Int, modifier: Modifier = Modifier) {
    val nature = KokTheme.nature
    val level by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "waterLevel",
    )
    val wave = rememberInfiniteTransition(label = "wave")
    val phase by wave.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    // Su eklendiğinde dalga genliği bir an büyür, sonra yay gibi sakinleşir.
    val splash = remember { Animatable(1f) }
    LaunchedEffect(pulse) {
        if (pulse > 0) {
            splash.snapTo(3.2f)
            splash.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow))
        }
    }
    val amplitudeBoost = splash.value
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // Testi silueti: dar boyun, geniş gövde
        val jar = Path().apply {
            moveTo(w * 0.36f, 0f)
            lineTo(w * 0.64f, 0f)
            lineTo(w * 0.64f, h * 0.14f)
            cubicTo(w * 1.02f, h * 0.24f, w * 1.0f, h * 0.9f, w * 0.78f, h)
            lineTo(w * 0.22f, h)
            cubicTo(w * 0.0f, h * 0.9f, w * -0.02f, h * 0.24f, w * 0.36f, h * 0.14f)
            close()
        }
        clipPath(jar) {
            drawRect(nature.trackEmpty)
            val surfaceY = h * (1f - level)
            val amp = 3.dp.toPx() * amplitudeBoost
            val water = Path().apply {
                moveTo(0f, h)
                lineTo(0f, surfaceY)
                var x = 0f
                while (x <= w) {
                    lineTo(x, surfaceY + amp * sin(phase + x / w * 2f * PI.toFloat()))
                    x += 4f
                }
                lineTo(w, h)
                close()
            }
            drawPath(water, nature.waterLight.copy(alpha = 0.85f))
            drawRect(
                nature.water.copy(alpha = 0.35f),
                topLeft = Offset(0f, surfaceY + amp * 2),
                size = androidx.compose.ui.geometry.Size(w, h),
            )
        }
        drawPath(jar, nature.bark.copy(alpha = 0.6f), style = Stroke(width = 1.6.dp.toPx()))
    }
}
