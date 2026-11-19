package dev.kok.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Organik köşeler: tam simetrik "hap" formlar yerine çakıl taşı gibi
 * hafif asimetrik yuvarlatmalar.
 */
val KokShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(topStart = 10.dp, topEnd = 14.dp, bottomEnd = 10.dp, bottomStart = 12.dp),
    medium = RoundedCornerShape(topStart = 18.dp, topEnd = 24.dp, bottomEnd = 18.dp, bottomStart = 22.dp),
    large = RoundedCornerShape(topStart = 26.dp, topEnd = 32.dp, bottomEnd = 24.dp, bottomStart = 30.dp),
    extraLarge = RoundedCornerShape(topStart = 32.dp, topEnd = 40.dp, bottomEnd = 32.dp, bottomStart = 36.dp),
)

/** Yaprak formlu rozet: iki köşe sivri, iki köşe yuvarlak. */
val LeafShape = RoundedCornerShape(topStart = 2.dp, topEnd = 18.dp, bottomEnd = 2.dp, bottomStart = 18.dp)
