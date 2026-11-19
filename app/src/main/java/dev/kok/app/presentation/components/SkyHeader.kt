package dev.kok.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import dev.kok.app.presentation.theme.KokTheme

/**
 * Gökyüzünden ufka geçişli başlık alanı; altta yumuşak tepe silueti.
 */
@Composable
fun SkyHeader(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val nature = KokTheme.nature
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(nature.skyTop, nature.horizon)))
                val h = size.height
                val w = size.width
                val hill = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h - 18.dp.toPx())
                    cubicTo(w * 0.22f, h - 34.dp.toPx(), w * 0.42f, h - 6.dp.toPx(), w * 0.62f, h - 20.dp.toPx())
                    cubicTo(w * 0.78f, h - 30.dp.toPx(), w * 0.9f, h - 14.dp.toPx(), w, h - 22.dp.toPx())
                    lineTo(w, h)
                    close()
                }
                drawPath(hill, nature.leaf.copy(alpha = 0.10f))
                val nearHill = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h - 8.dp.toPx())
                    cubicTo(w * 0.3f, h - 16.dp.toPx(), w * 0.6f, h - 2.dp.toPx(), w, h - 10.dp.toPx())
                    lineTo(w, h)
                    close()
                }
                drawPath(nearHill, nature.leaf.copy(alpha = 0.08f))
            },
        content = content,
    )
}

/** Ufuk çizgisi olmadan, yalnızca gradyan kullanan sade varyasyon için ayrı çizim. */
@Composable
fun SunDisc(modifier: Modifier = Modifier) {
    val nature = KokTheme.nature
    Canvas(modifier) {
        drawCircle(nature.clayLight.copy(alpha = 0.35f), radius = size.minDimension / 2f)
        drawCircle(nature.clayLight.copy(alpha = 0.55f), radius = size.minDimension / 3f)
    }
}
