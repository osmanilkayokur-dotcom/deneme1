package dev.kok.app.presentation.feature.body

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.kok.app.presentation.theme.KokTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ay döngüsü: ay başında yeni ay (karanlık), ortasında dolunay, sonunda tekrar incelir.
 * [progress] 0..1 ayın geçen kısmı.
 */
@Composable
fun MoonPhase(progress: Float, modifier: Modifier = Modifier) {
    val nature = KokTheme.nature
    val lit = nature.clayLight
    val dark = nature.trackEmpty
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = center
        drawCircle(dark, r, c)
        // Aydınlık oran: 0 → 0, 0.5 → 1, 1 → 0
        val phase = progress.coerceIn(0f, 1f)
        val waxing = phase <= 0.5f
        val illum = if (waxing) phase * 2f else (1f - phase) * 2f
        // Terminatör elipsinin yatay yarıçapı: -r..r
        val tx = r * (1f - 2f * illum)
        val path = Path().apply {
            val steps = 48
            // Aydınlık kenar (yarım daire)
            for (i in 0..steps) {
                val a = -PI / 2 + PI * i / steps
                val x = c.x + (if (waxing) 1 else -1) * r * cos(a).toFloat()
                val y = c.y + r * sin(a).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            // Terminatör (elips yarısı)
            for (i in steps downTo 0) {
                val a = -PI / 2 + PI * i / steps
                val x = c.x + (if (waxing) 1 else -1) * tx * cos(a).toFloat()
                val y = c.y + r * sin(a).toFloat()
                lineTo(x, y)
            }
            close()
        }
        drawPath(path, lit)
        drawCircle(nature.bark.copy(alpha = 0.3f), r, c, style = Stroke(1.dp.toPx()))
    }
}

/** Ağaç halkaları: her aylık ölçüm bir halka; halkalar hafif düzensiz (organik). */
@Composable
fun TreeRings(count: Int, modifier: Modifier = Modifier, highlightOuter: Boolean = true) {
    val nature = KokTheme.nature
    Canvas(modifier) {
        val maxR = size.minDimension / 2f
        drawCircle(nature.clayLight.copy(alpha = 0.18f), maxR, center)
        val rings = count.coerceAtLeast(0)
        if (rings == 0) {
            drawCircle(nature.bark.copy(alpha = 0.3f), maxR * 0.12f, center)
            return@Canvas
        }
        for (i in 1..rings) {
            val baseR = maxR * (0.12f + 0.86f * i / rings)
            val rnd = Random(i * 31)
            val path = Path()
            val steps = 64
            val wobble = baseR * 0.04f
            val offsets = FloatArray(6) { rnd.nextFloat() * 2f * PI.toFloat() }
            for (s in 0..steps) {
                val a = 2f * PI.toFloat() * s / steps
                val rr = baseR + wobble * (sin(a * 3 + offsets[0]) + 0.5f * sin(a * 5 + offsets[1]))
                val p = Offset(center.x + rr * cos(a), center.y + rr * sin(a))
                if (s == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            val isOuter = i == rings && highlightOuter
            drawPath(
                path,
                if (isOuter) nature.clay else nature.bark.copy(alpha = 0.45f),
                style = Stroke(if (isOuter) 2.dp.toPx() else 1.2.dp.toPx()),
            )
        }
        drawCircle(nature.bark.copy(alpha = 0.5f), maxR * 0.06f, center)
    }
}

