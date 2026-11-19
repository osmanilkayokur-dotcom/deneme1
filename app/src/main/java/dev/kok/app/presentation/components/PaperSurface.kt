package dev.kok.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Hafif kağıt/toprak dokusu: sabit tohumlu rastgele lif ve gren noktaları.
 * drawWithCache ile yalnızca boyut değişince yeniden hesaplanır.
 */
fun Modifier.paperTexture(fiber: Color, seed: Int = 7): Modifier = drawWithCache {
    val rnd = Random(seed)
    val count = ((size.width * size.height) / 900f).toInt().coerceIn(40, 900)
    val grains = List(count) {
        Triple(
            Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height),
            0.4f + rnd.nextFloat() * 1.1f,
            0.025f + rnd.nextFloat() * 0.05f,
        )
    }
    val fibers = List(count / 12) {
        val start = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
        val len = 6f + rnd.nextFloat() * 14f
        val angle = rnd.nextFloat() * Math.PI.toFloat()
        start to Offset(start.x + len * kotlin.math.cos(angle), start.y + len * kotlin.math.sin(angle))
    }
    onDrawBehind {
        grains.forEach { (o, r, a) -> drawCircle(fiber.copy(alpha = a), radius = r, center = o) }
        fibers.forEach { (s, e) -> drawLine(fiber.copy(alpha = 0.04f), s, e, strokeWidth = 0.8f) }
    }
}

/** Doğa kartı: dokulu yüzey + ince toprak tonlu kenar. */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentPadding: androidx.compose.ui.unit.Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val fiber = MaterialTheme.colorScheme.onSurface
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color)
            .paperTexture(fiber)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(contentPadding),
        content = content,
    )
}
