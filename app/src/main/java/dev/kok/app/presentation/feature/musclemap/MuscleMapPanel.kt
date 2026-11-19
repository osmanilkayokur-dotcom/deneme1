package dev.kok.app.presentation.feature.musclemap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.kok.app.domain.model.MuscleGroup
import dev.kok.app.domain.model.MuscleRole
import dev.kok.app.domain.usecase.MuscleHighlight
import dev.kok.app.presentation.theme.KokTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleMapPanel(highlight: MuscleHighlight, muscles: List<MuscleGroup>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val modelAvailable = remember { hasAnatomyModel(context) }
    var show3d by rememberSaveable { mutableStateOf(modelAvailable) }
    val nature = KokTheme.nature

    Column(modifier.fillMaxWidth()) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !show3d, onClick = { show3d = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("2D") }
            SegmentedButton(selected = show3d, onClick = { show3d = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("3D 360°") }
        }
        Spacer(Modifier.height(12.dp))
        if (show3d && modelAvailable) {
            MuscleMap3D(
                highlight = highlight,
                muscles = muscles,
                primary = nature.clay,
                secondary = nature.clayLight,
                rest = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Text(
                "Parmağınla döndür, iki parmakla yakınlaştır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            if (show3d) {
                Text(
                    "3D anatomi modeli henüz eklenmedi; şimdilik 2D harita gösteriliyor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            BodyMap2D(highlight)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(nature.clay, "Birincil")
            Legend(nature.clayLight, "İkincil")
            Legend(MaterialTheme.colorScheme.surfaceContainerHighest, "Dinlenen")
        }
        val worked = muscles.filter { highlight.roleOf(it.code) == MuscleRole.PRIMARY }.joinToString { it.displayName }
        if (worked.isNotBlank()) {
            Text(
                "Bugün odak: $worked",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
