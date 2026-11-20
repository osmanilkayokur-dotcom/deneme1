package dev.kok.app.presentation.components.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import dev.kok.app.domain.model.FoodCategory
import dev.kok.app.presentation.theme.NatureColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Yemek eklendiğinde kategorisine göre kısa doğa kökeni animasyonu:
 * - Tahıl: çeltik başağından pirinç taneleri kaseye dökülür
 * - Sebze: topraktan filiz çıkar ve yaprak açar
 * - Meyve: daldan meyve kaseye düşer
 * - Protein: tohum/baklagil topraktan kabuğunu açar
 * - Süt ürünü: kovadan süt damlaları kaseye akar
 * - Diğer: rüzgârda süzülen bir yaprak
 * [onFinished] animasyon bitince çağrılır.
 */
@Composable
fun FoodOriginAnimation(
    category: FoodCategory,
    colors: NatureColors,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    val t = remember(category) { Animatable(0f) }
    LaunchedEffect(category) {
        t.snapTo(0f)
        t.animateTo(1f, tween(1700, easing = FastOutSlowInEasing))
        onFinished()
    }
    Canvas(modifier) {
        val p = t.value
        val fade = if (p > 0.85f) (1f - p) / 0.15f else 1f
        when (category) {
            FoodCategory.GRAIN_RICE -> riceFromPaddy(p, fade, colors)
            FoodCategory.VEGETABLE -> sproutFromSoil(p, fade, colors)
            FoodCategory.FRUIT -> fruitFromBranch(p, fade, colors)
            FoodCategory.PROTEIN -> podOpening(p, fade, colors)
            FoodCategory.DAIRY -> milkFromPail(p, fade, colors)
            FoodCategory.OTHER -> driftingLeaf(p, fade, colors)
        }
    }
}

private fun DrawScope.bowl(c: NatureColors, alpha: Float) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.3f, h * 0.72f)
        lineTo(w * 0.7f, h * 0.72f)
        cubicTo(w * 0.69f, h * 0.86f, w * 0.6f, h * 0.92f, w * 0.5f, h * 0.92f)
        cubicTo(w * 0.4f, h * 0.92f, w * 0.31f, h * 0.86f, w * 0.3f, h * 0.72f)
        close()
    }
    drawPath(path, c.root.copy(alpha = alpha))
    drawLine(c.bark.copy(alpha = alpha), Offset(w * 0.28f, h * 0.72f), Offset(w * 0.72f, h * 0.72f), 3f, StrokeCap.Round)
}

private fun DrawScope.riceFromPaddy(p: Float, a: Float, c: NatureColors) {
    val w = size.width
    val h = size.height
    bowl(c, a)
    // Eğilen başak sapı
    val bend = (p * 1.4f).coerceAtMost(1f)
    val stalk = Path().apply {
        moveTo(w * 0.22f, h * 0.05f)
        quadraticTo(w * 0.32f, h * 0.18f, w * (0.36f + 0.06f * bend), h * (0.26f + 0.06f * bend))
    }
    drawPath(stalk, c.leaf.copy(alpha = a), style = Stroke(4f, cap = StrokeCap.Round))
    // Başaktaki taneler
    repeat(6) { i ->
        val gx = w * (0.26f + i * 0.022f + 0.01f * bend)
        val gy = h * (0.1f + i * 0.03f + 0.02f * bend)
        drawOval(c.carb.copy(alpha = a), Offset(gx, gy), Size(w * 0.03f, h * 0.018f))
    }
    // Dökülen pirinç taneleri
    val rnd = Random(7)
    repeat(18) { i ->
        val start = 0.12f + i * 0.03f
        val local = ((p - start) / 0.4f).coerceIn(0f, 1f)
        if (local > 0f) {
            val x0 = w * (0.4f + rnd.nextFloat() * 0.04f)
            val x1 = w * (0.4f + rnd.nextFloat() * 0.22f)
            val y = h * (0.3f + local * 0.42f)
            val x = x0 + (x1 - x0) * local
            rotate(degrees = 40f * i, pivot = Offset(x, y)) {
                drawOval(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f * a), Offset(x, y), Size(w * 0.022f, h * 0.012f))
            }
        }
    }
    // Kasede birikim
    val fill = ((p - 0.35f) / 0.5f).coerceIn(0f, 1f)
    if (fill > 0f) {
        drawOval(
            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f * a),
            Offset(w * (0.5f - 0.18f * fill), h * (0.72f - 0.05f * fill)),
            Size(w * 0.36f * fill, h * 0.08f * fill),
        )
    }
}

private fun DrawScope.sproutFromSoil(p: Float, a: Float, c: NatureColors) {
    val w = size.width
    val h = size.height
    // Toprak tümseği
    drawOval(c.root.copy(alpha = a), Offset(w * 0.25f, h * 0.78f), Size(w * 0.5f, h * 0.14f))
    val grow = (p / 0.6f).coerceIn(0f, 1f)
    val topY = h * (0.82f - 0.5f * grow)
    drawLine(c.leaf.copy(alpha = a), Offset(w * 0.5f, h * 0.82f), Offset(w * 0.5f, topY), 5f, StrokeCap.Round)
    val leafOpen = ((p - 0.45f) / 0.35f).coerceIn(0f, 1f)
    if (leafOpen > 0f) {
        val len = w * 0.16f * leafOpen
        listOf(-1f, 1f).forEach { dir ->
            val leaf = Path().apply {
                moveTo(w * 0.5f, topY + h * 0.04f)
                quadraticTo(w * 0.5f + dir * len * 0.4f, topY - len * 0.6f, w * 0.5f + dir * len, topY - len * 0.1f)
                quadraticTo(w * 0.5f + dir * len * 0.5f, topY + h * 0.05f, w * 0.5f, topY + h * 0.04f)
                close()
            }
            drawPath(leaf, c.fiber.copy(alpha = a))
        }
    }
}

private fun DrawScope.fruitFromBranch(p: Float, a: Float, c: NatureColors) {
    val w = size.width
    val h = size.height
    bowl(c, a)
    val branch = Path().apply {
        moveTo(0f, h * 0.08f)
        quadraticTo(w * 0.3f, h * 0.02f, w * 0.62f, h * 0.12f)
    }
    drawPath(branch, c.bark.copy(alpha = a), style = Stroke(6f, cap = StrokeCap.Round))
    drawOval(c.leaf.copy(alpha = a), Offset(w * 0.3f, h * 0.02f), Size(w * 0.1f, h * 0.05f))
    val sway = sin(p * PI * 4).toFloat() * (1f - p) * w * 0.02f
    val fall = ((p - 0.25f) / 0.5f).coerceIn(0f, 1f)
    val eased = fall * fall
    val y = h * (0.16f + 0.5f * eased)
    val bounce = if (fall >= 1f) sin(((p - 0.75f) / 0.25f).coerceIn(0f, 1f) * PI).toFloat() * h * 0.03f else 0f
    drawCircle(c.clay.copy(alpha = a), w * 0.07f, Offset(w * 0.5f + sway, y - bounce))
    drawLine(c.bark.copy(alpha = a), Offset(w * 0.5f + sway, y - bounce - w * 0.07f), Offset(w * 0.51f + sway, y - bounce - w * 0.1f), 3f)
}

private fun DrawScope.podOpening(p: Float, a: Float, c: NatureColors) {
    val w = size.width
    val h = size.height
    drawOval(c.root.copy(alpha = a), Offset(w * 0.22f, h * 0.8f), Size(w * 0.56f, h * 0.12f))
    val rise = (p / 0.4f).coerceIn(0f, 1f)
    val open = ((p - 0.35f) / 0.4f).coerceIn(0f, 1f)
    val cy = h * (0.8f - 0.3f * rise)
    // Kabuğun iki yarısı açılır
    listOf(-1f, 1f).forEach { dir ->
        rotate(degrees = dir * 35f * open, pivot = Offset(w * 0.5f, cy + h * 0.08f)) {
            val half = Path().apply {
                moveTo(w * 0.5f, cy + h * 0.08f)
                quadraticTo(w * (0.5f + dir * 0.2f), cy, w * 0.5f, cy - h * 0.12f)
                close()
            }
            drawPath(half, c.leaf.copy(alpha = a))
        }
    }
    // İçindeki taneler
    repeat(3) { i ->
        drawCircle(c.protein.copy(alpha = a * open), w * 0.03f, Offset(w * 0.5f, cy - h * 0.07f + i * h * 0.05f))
    }
}

private fun DrawScope.milkFromPail(p: Float, a: Float, c: NatureColors) {
    val w = size.width
    val h = size.height
    bowl(c, a)
    val tilt = (p / 0.3f).coerceIn(0f, 1f) * 35f
    rotate(degrees = tilt, pivot = Offset(w * 0.32f, h * 0.2f)) {
        drawRect(c.bark.copy(alpha = a), Offset(w * 0.18f, h * 0.08f), Size(w * 0.18f, h * 0.18f))
        drawArc(c.bark.copy(alpha = a), 180f, 180f, false, Offset(w * 0.18f, h * 0.0f), Size(w * 0.18f, h * 0.16f), style = Stroke(3f))
    }
    val pour = ((p - 0.25f) / 0.55f).coerceIn(0f, 1f)
    repeat(10) { i ->
        val local = (pour * 1.6f - i * 0.07f)
        if (local in 0f..1f) {
            val y = h * (0.26f + 0.46f * local)
            val x = w * (0.4f + 0.08f * local)
            drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = a * 0.95f), w * 0.018f, Offset(x, y))
        }
    }
    if (pour > 0.3f) {
        val f = ((pour - 0.3f) / 0.7f)
        drawOval(androidx.compose.ui.graphics.Color.White.copy(alpha = a * 0.9f), Offset(w * (0.5f - 0.17f * f), h * 0.69f), Size(w * 0.34f * f, h * 0.06f))
    }
}

private fun DrawScope.driftingLeaf(p: Float, a: Float, c: NatureColors) {
    val w = size.width
    val h = size.height
    val x = w * (0.15f + 0.7f * p) + sin(p * PI * 3).toFloat() * w * 0.06f
    val y = h * (0.1f + 0.7f * p)
    rotate(degrees = 360f * p * 0.8f, pivot = Offset(x, y)) {
        val leaf = Path().apply {
            moveTo(x - w * 0.08f, y)
            quadraticTo(x, y - h * 0.08f, x + w * 0.08f, y)
            quadraticTo(x, y + h * 0.08f, x - w * 0.08f, y)
            close()
        }
        drawPath(leaf, c.leaf.copy(alpha = a))
        drawLine(c.bark.copy(alpha = a), Offset(x - w * 0.08f, y), Offset(x + w * 0.08f, y), 2f)
    }
}
