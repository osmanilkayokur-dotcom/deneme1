package dev.kok.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.kok.app.presentation.theme.KokTheme

/**
 * İnce ağaç dalı ayırıcı: hafif dalgalı bir gövde, aralıklarla küçük yaprak tomurcukları.
 * Yaprak konumları sabittir (her çizimde aynı görünür).
 */
@Composable
fun BranchDivider(
    modifier: Modifier = Modifier,
    color: Color = KokTheme.nature.bark.copy(alpha = 0.45f),
    leafColor: Color = KokTheme.nature.leaf.copy(alpha = 0.55f),
) {
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val midY = size.height / 2f
        val w = size.width
        val stroke = 1.2.dp.toPx()
        val branch = Path().apply {
            moveTo(0f, midY + 1.dp.toPx())
            cubicTo(w * 0.25f, midY - 2.dp.toPx(), w * 0.5f, midY + 2.5.dp.toPx(), w * 0.75f, midY)
            cubicTo(w * 0.85f, midY - 1.dp.toPx(), w * 0.95f, midY + 1.dp.toPx(), w, midY - 0.5.dp.toPx())
        }
        drawPath(branch, color, style = Stroke(width = stroke, cap = StrokeCap.Round))

        val leafLen = 6.dp.toPx()
        listOf(0.18f to -1f, 0.42f to 1f, 0.63f to -1f, 0.88f to 1f).forEach { (fx, dir) ->
            val base = Offset(w * fx, midY)
            val tip = Offset(base.x + leafLen * 0.8f, base.y + dir * leafLen * 0.7f)
            val leaf = Path().apply {
                moveTo(base.x, base.y)
                quadraticTo(base.x + leafLen * 0.1f, tip.y, tip.x, tip.y)
                quadraticTo(tip.x - leafLen * 0.1f, base.y, base.x, base.y)
                close()
            }
            drawPath(leaf, leafColor, style = Fill)
        }
    }
}
