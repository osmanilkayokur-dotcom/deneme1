package dev.kok.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.kok.app.R

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight, opsz: Float) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("opsz", opsz),
    ),
)

/** Başlıklar ve öyküsel istatistikler için sıcak serif. */
val Fraunces = FontFamily(
    variableFont(R.font.fraunces, FontWeight.Normal, 72f),
    variableFont(R.font.fraunces, FontWeight.Medium, 72f),
    variableFont(R.font.fraunces, FontWeight.SemiBold, 72f),
    variableFont(R.font.fraunces, FontWeight.Bold, 72f),
)

/** Sayısal veriler ve gövde metni için sans-serif. */
val DmSans = FontFamily(
    variableFont(R.font.dm_sans, FontWeight.Normal, 14f),
    variableFont(R.font.dm_sans, FontWeight.Medium, 14f),
    variableFont(R.font.dm_sans, FontWeight.SemiBold, 14f),
    variableFont(R.font.dm_sans, FontWeight.Bold, 14f),
)

/** Tablolarda/sayaçlarda hizalı rakamlar için. */
const val TABULAR_NUMS = "tnum"

private fun serif(size: Int, line: Int, weight: FontWeight = FontWeight.Medium) =
    TextStyle(fontFamily = Fraunces, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun sans(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) =
    TextStyle(
        fontFamily = DmSans,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
        fontFeatureSettings = TABULAR_NUMS,
    )

val KokTypography = Typography(
    displayLarge = serif(52, 60, FontWeight.SemiBold),
    displayMedium = serif(42, 50, FontWeight.SemiBold),
    displaySmall = serif(34, 42),
    headlineLarge = serif(30, 38),
    headlineMedium = serif(26, 34),
    headlineSmall = serif(22, 30),
    titleLarge = serif(20, 28),
    titleMedium = sans(16, 24, FontWeight.SemiBold),
    titleSmall = sans(14, 20, FontWeight.SemiBold),
    bodyLarge = sans(16, 24),
    bodyMedium = sans(14, 20),
    bodySmall = sans(12, 16),
    labelLarge = sans(14, 20, FontWeight.Medium, 0.1f),
    labelMedium = sans(12, 16, FontWeight.Medium, 0.4f),
    labelSmall = sans(11, 16, FontWeight.Medium, 0.5f),
)
