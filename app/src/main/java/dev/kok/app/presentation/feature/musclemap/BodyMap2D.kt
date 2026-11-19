package dev.kok.app.presentation.feature.musclemap

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.kok.app.domain.model.MuscleRole
import dev.kok.app.domain.usecase.MuscleHighlight
import dev.kok.app.presentation.theme.KokTheme

/**
 * Ön ve arka görünümde stilize vücut haritası. Kas bölgeleri veritabanındaki kas kodlarıyla
 * eşleşir; o günün birincil kasları kil turuncusu, ikincilleri açık kil, diğerleri soluk kalır.
 */
@Composable
fun BodyMap2D(highlight: MuscleHighlight, modifier: Modifier = Modifier) {
    val nature = KokTheme.nature
    val rest = MaterialTheme.colorScheme.surfaceContainerHighest
    val silhouette = nature.trackEmpty
    val outline = nature.bark.copy(alpha = 0.35f)
    val colors = MuscleColors(
        primary = nature.clay,
        secondary = nature.clayLight.copy(alpha = 0.75f),
        rest = rest,
    )
    val description = highlight.byCode.entries.joinToString { "${it.key} ${if (it.value == MuscleRole.PRIMARY) "birincil" else "ikincil"}" }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(Side.FRONT to "Ön", Side.BACK to "Arka").forEach { (side, label) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Figure(
                        side = side,
                        highlight = highlight,
                        colors = colors,
                        silhouette = silhouette,
                        outline = outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.5f)
                            .semantics { contentDescription = "$label görünüm. Çalışan kaslar: $description" },
                    )
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private enum class Side { FRONT, BACK }

private data class MuscleColors(val primary: Color, val secondary: Color, val rest: Color)

private enum class Kind { ELLIPSE, ROUND_RECT }

/** Normalize koordinatlar: x 0..1 (genişlik), y 0..1 (yükseklik). mirror=true ise x eksenine göre simetriği de çizilir. */
private data class Region(
    val code: String,
    val side: Side,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val kind: Kind = Kind.ELLIPSE,
    val mirror: Boolean = true,
)

private val regions = listOf(
    // ÖN
    Region("SIDE_DELT", Side.FRONT, 0.295f, 0.205f, 0.055f, 0.065f),
    Region("FRONT_DELT", Side.FRONT, 0.345f, 0.195f, 0.065f, 0.055f),
    Region("CHEST", Side.FRONT, 0.435f, 0.245f, 0.115f, 0.075f),
    Region("BICEPS", Side.FRONT, 0.285f, 0.31f, 0.05f, 0.095f),
    Region("FOREARMS", Side.FRONT, 0.255f, 0.43f, 0.045f, 0.105f),
    Region("ABS", Side.FRONT, 0.5f, 0.38f, 0.085f, 0.17f, Kind.ROUND_RECT, mirror = false),
    Region("OBLIQUES", Side.FRONT, 0.405f, 0.385f, 0.045f, 0.105f),
    Region("QUADS", Side.FRONT, 0.43f, 0.635f, 0.085f, 0.175f),
    Region("ADDUCTORS", Side.FRONT, 0.482f, 0.6f, 0.028f, 0.095f),
    Region("CALVES", Side.FRONT, 0.43f, 0.835f, 0.045f, 0.09f),
    // ARKA
    Region("TRAPS", Side.BACK, 0.5f, 0.215f, 0.17f, 0.085f, Kind.ROUND_RECT, mirror = false),
    Region("REAR_DELT", Side.BACK, 0.32f, 0.205f, 0.065f, 0.06f),
    Region("TRICEPS", Side.BACK, 0.285f, 0.31f, 0.05f, 0.095f),
    Region("FOREARMS", Side.BACK, 0.255f, 0.43f, 0.045f, 0.105f),
    Region("RHOMBOIDS", Side.BACK, 0.465f, 0.275f, 0.04f, 0.06f),
    Region("LATS", Side.BACK, 0.405f, 0.33f, 0.075f, 0.12f),
    Region("LOWER_BACK", Side.BACK, 0.5f, 0.43f, 0.08f, 0.09f, Kind.ROUND_RECT, mirror = false),
    Region("GLUTES", Side.BACK, 0.44f, 0.53f, 0.075f, 0.07f),
    Region("HAMSTRINGS", Side.BACK, 0.435f, 0.665f, 0.07f, 0.15f),
    Region("CALVES", Side.BACK, 0.435f, 0.835f, 0.055f, 0.09f),
)

@Composable
private fun Figure(
    side: Side,
    highlight: MuscleHighlight,
    colors: MuscleColors,
    silhouette: Color,
    outline: Color,
    modifier: Modifier,
) {
    val sideRegions = regions.filter { it.side == side }
    // Her bölge için renk geçişi animasyonlu (gün değişince yumuşak geçiş).
    val animated = sideRegions.map { r ->
        val target = when (highlight.roleOf(r.code)) {
            MuscleRole.PRIMARY -> colors.primary
            MuscleRole.SECONDARY -> colors.secondary
            null -> colors.rest
        }
        val c by animateColorAsState(target, tween(450), label = "muscle_${r.code}_$side")
        r to c
    }
    Canvas(modifier) {
        drawSilhouette(silhouette, outline)
        animated.forEach { (r, c) ->
            drawRegion(r, c)
            if (r.mirror) drawRegion(r.copy(cx = 1f - r.cx), c)
        }
    }
}

private fun DrawScope.drawRegion(r: Region, color: Color) {
    val w = size.width
    val h = size.height
    val topLeft = Offset((r.cx - r.w / 2f) * w, (r.cy - r.h / 2f) * h)
    val sz = Size(r.w * w, r.h * h)
    when (r.kind) {
        Kind.ELLIPSE -> drawOval(color, topLeft, sz)
        Kind.ROUND_RECT -> drawRoundRect(color, topLeft, sz, CornerRadius(sz.width * 0.3f, sz.width * 0.3f))
    }
}

/** Baş, gövde, kollar ve bacaklardan oluşan yumuşak hatlı siluet. */
private fun DrawScope.drawSilhouette(fill: Color, outline: Color) {
    val w = size.width
    val h = size.height
    fun p(x: Float, y: Float) = Offset(x * w, y * h)

    drawCircle(fill, radius = 0.075f * w, center = p(0.5f, 0.075f))
    drawCircle(outline, radius = 0.075f * w, center = p(0.5f, 0.075f), style = Stroke(1.dp.toPx()))

    val body = Path().apply {
        moveTo(p(0.455f, 0.13f).x, p(0.455f, 0.13f).y)
        lineTo(p(0.545f, 0.13f).x, p(0.545f, 0.13f).y)
        // sağ omuz ve kol
        cubicTo(0.6f * w, 0.155f * h, 0.69f * w, 0.16f * h, 0.72f * w, 0.2f * h)
        cubicTo(0.76f * w, 0.28f * h, 0.78f * w, 0.4f * h, 0.775f * w, 0.5f * h)
        lineTo(0.735f * w, 0.5f * h)
        cubicTo(0.72f * w, 0.42f * h, 0.69f * w, 0.33f * h, 0.665f * w, 0.29f * h)
        // sağ gövde -> kalça
        cubicTo(0.655f * w, 0.36f * h, 0.63f * w, 0.42f * h, 0.635f * w, 0.5f * h)
        // sağ bacak
        cubicTo(0.65f * w, 0.62f * h, 0.6f * w, 0.78f * h, 0.585f * w, 0.95f * h)
        lineTo(0.515f * w, 0.95f * h)
        cubicTo(0.515f * w, 0.8f * h, 0.51f * w, 0.66f * h, 0.5f * w, 0.56f * h)
        // sol bacak
        cubicTo(0.49f * w, 0.66f * h, 0.485f * w, 0.8f * h, 0.485f * w, 0.95f * h)
        lineTo(0.415f * w, 0.95f * h)
        cubicTo(0.4f * w, 0.78f * h, 0.35f * w, 0.62f * h, 0.365f * w, 0.5f * h)
        // sol gövde -> kol
        cubicTo(0.37f * w, 0.42f * h, 0.345f * w, 0.36f * h, 0.335f * w, 0.29f * h)
        cubicTo(0.31f * w, 0.33f * h, 0.28f * w, 0.42f * h, 0.265f * w, 0.5f * h)
        lineTo(0.225f * w, 0.5f * h)
        cubicTo(0.22f * w, 0.4f * h, 0.24f * w, 0.28f * h, 0.28f * w, 0.2f * h)
        cubicTo(0.31f * w, 0.16f * h, 0.4f * w, 0.155f * h, 0.455f * w, 0.13f * h)
        close()
    }
    drawPath(body, fill)
    drawPath(body, outline, style = Stroke(1.dp.toPx()))
}
