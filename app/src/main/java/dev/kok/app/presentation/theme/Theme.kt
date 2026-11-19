package dev.kok.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.kok.app.presentation.theme.NaturePalette as P

private val LightScheme = lightColorScheme(
    primary = P.Forest,
    onPrimary = P.Linen,
    primaryContainer = P.MossLight,
    onPrimaryContainer = P.Forest,
    secondary = P.Earth,
    onSecondary = P.Linen,
    secondaryContainer = P.Sand,
    onSecondaryContainer = P.OakBark,
    tertiary = P.Stream,
    onTertiary = P.Linen,
    tertiaryContainer = P.Sky,
    onTertiaryContainer = P.OakBark,
    error = P.Berry,
    onError = P.Linen,
    background = P.Linen,
    onBackground = P.InkDark,
    surface = P.Linen,
    onSurface = P.InkDark,
    surfaceVariant = P.Sand,
    onSurfaceVariant = P.InkMuted,
    surfaceContainerLowest = P.Linen,
    surfaceContainerLow = Color(0xFFF0EADF),
    surfaceContainer = P.Sand,
    surfaceContainerHigh = P.SandDeep,
    surfaceContainerHighest = P.SandDeep,
    outline = P.Earth,
    outlineVariant = P.SandDeep,
)

private val DarkScheme = darkColorScheme(
    primary = P.MossLight,
    onPrimary = P.ForestSoil,
    primaryContainer = P.Forest,
    onPrimaryContainer = P.ParchmentText,
    secondary = Color(0xFFC4A585),
    onSecondary = P.ForestSoil,
    secondaryContainer = P.OakBark,
    onSecondaryContainer = P.ParchmentText,
    tertiary = P.Sky,
    onTertiary = P.ForestSoil,
    tertiaryContainer = P.Stream,
    onTertiaryContainer = P.ParchmentText,
    error = P.BerryLight,
    onError = P.ForestSoil,
    background = P.ForestSoil,
    onBackground = P.ParchmentText,
    surface = P.ForestSoil,
    onSurface = P.ParchmentText,
    surfaceVariant = P.ForestSoilRaised,
    onSurfaceVariant = P.ParchmentMuted,
    surfaceContainerLowest = Color(0xFF161E19),
    surfaceContainerLow = P.ForestSoil,
    surfaceContainer = P.ForestSoilRaised,
    surfaceContainerHigh = P.ForestSoilHigh,
    surfaceContainerHighest = Color(0xFF35443A),
    outline = Color(0xFF7D8A7F),
    outlineVariant = P.ForestSoilHigh,
)

/** Material şemasında karşılığı olmayan doğa-özel tonlar. */
@Immutable
data class NatureColors(
    val water: Color,
    val waterLight: Color,
    val clay: Color,
    val clayLight: Color,
    val bark: Color,
    val root: Color,
    val leaf: Color,
    val skyTop: Color,
    val horizon: Color,
    val protein: Color,
    val carb: Color,
    val fat: Color,
    val fiber: Color,
    val micro: Color,
    val trackEmpty: Color,
)

private val LightNature = NatureColors(
    water = P.Stream,
    waterLight = P.Sky,
    clay = P.Clay,
    clayLight = P.ClayLight,
    bark = P.OakBark,
    root = P.Earth,
    leaf = P.Moss,
    skyTop = Color(0xFFDCE8EC),
    horizon = P.Linen,
    protein = P.Clay,
    carb = Color(0xFFB8923F),
    fat = P.Earth,
    fiber = P.Moss,
    micro = P.Stream,
    trackEmpty = P.Sand,
)

private val DarkNature = NatureColors(
    water = P.Sky,
    waterLight = P.Stream,
    clay = P.ClayLight,
    clayLight = P.Clay,
    bark = Color(0xFFC4A585),
    root = Color(0xFFA88B6E),
    leaf = P.MossLight,
    skyTop = Color(0xFF243540),
    horizon = P.ForestSoil,
    protein = P.ClayLight,
    carb = Color(0xFFD8B566),
    fat = Color(0xFFC4A585),
    fiber = P.MossLight,
    micro = P.Sky,
    trackEmpty = P.ForestSoilHigh,
)

val LocalNatureColors = staticCompositionLocalOf { LightNature }

@Composable
fun KokTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme: ColorScheme = if (darkTheme) DarkScheme else LightScheme
    val nature = if (darkTheme) DarkNature else LightNature
    CompositionLocalProvider(LocalNatureColors provides nature) {
        MaterialTheme(
            colorScheme = scheme,
            typography = KokTypography,
            shapes = KokShapes,
            content = content,
        )
    }
}

object KokTheme {
    val nature: NatureColors
        @Composable get() = LocalNatureColors.current
}
