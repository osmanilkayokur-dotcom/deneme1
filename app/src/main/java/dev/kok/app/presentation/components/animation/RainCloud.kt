package dev.kok.app.presentation.components.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/**
 * Su eklendiğinde kabın üzerinde beliren stilize bulut ve ondan düşen yağmur damlaları.
 * [trigger] her değiştiğinde bir kez oynar (~1.4 sn). Tek Animatable ile 60 FPS, düşük maliyet.
 */
@Composable
fun RainCloud(trigger: Int, cloud: Color, drop: Color, modifier: Modifier = Modifier) {
    val t = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            t.snapTo(0f)
            t.animateTo(1f, tween(1400, easing = LinearEasing))
        }
    }
    Canvas(modifier) {
        val p = t.value
        if (p >= 1f) return@Canvas
        // 0–0.2 bulut belirir, 0.2–0.85 yağmur, 0.85–1 bulut söner
        val cloudAlpha = when {
            p < 0.2f -> p / 0.2f
            p > 0.85f -> (1f - p) / 0.15f
            else -> 1f
        }.coerceIn(0f, 1f)
        drawCloud(cloud.copy(alpha = 0.92f * cloudAlpha))
        val rainP = ((p - 0.15f) / 0.7f).coerceIn(0f, 1f)
        if (rainP > 0f && rainP < 1f) drawRain(drop, rainP)
    }
}

private fun DrawScope.drawCloud(color: Color) {
    val w = size.width
    val baseY = size.height * 0.22f
    drawCircle(color, w * 0.16f, Offset(w * 0.36f, baseY))
    drawCircle(color, w * 0.21f, Offset(w * 0.53f, baseY - w * 0.06f))
    drawCircle(color, w * 0.15f, Offset(w * 0.69f, baseY + w * 0.01f))
    drawRoundRectCloudBase(color, baseY, w)
}

private fun DrawScope.drawRoundRectCloudBase(color: Color, baseY: Float, w: Float) {
    val path = Path().apply {
        moveTo(w * 0.22f, baseY + w * 0.05f)
        lineTo(w * 0.82f, baseY + w * 0.05f)
        quadraticTo(w * 0.86f, baseY + w * 0.13f, w * 0.78f, baseY + w * 0.14f)
        lineTo(w * 0.28f, baseY + w * 0.14f)
        quadraticTo(w * 0.18f, baseY + w * 0.13f, w * 0.22f, baseY + w * 0.05f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawRain(color: Color, p: Float) {
    val w = size.width
    val h = size.height
    val top = h * 0.34f
    val bottom = h * 0.92f
    val columns = listOf(0.34f, 0.44f, 0.54f, 0.64f, 0.72f)
    columns.forEachIndexed { i, cx ->
        // Her damla sütunu farklı fazla, iki dalga halinde düşer.
        repeat(2) { wave ->
            val local = ((p * 1.6f) - i * 0.08f - wave * 0.45f)
            if (local in 0f..1f) {
                val y = top + (bottom - top) * local
                val x = w * cx + sin((local + i) * 6f) * w * 0.006f
                val len = h * 0.05f
                val alpha = (1f - local * 0.6f).coerceIn(0f, 1f)
                val drop = Path().apply {
                    moveTo(x, y - len)
                    quadraticTo(x + len * 0.28f, y - len * 0.15f, x, y)
                    quadraticTo(x - len * 0.28f, y - len * 0.15f, x, y - len)
                    close()
                }
                drawPath(drop, color.copy(alpha = alpha))
            }
        }
    }
}
