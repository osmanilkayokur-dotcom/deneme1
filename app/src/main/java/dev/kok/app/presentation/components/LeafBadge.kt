package dev.kok.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.kok.app.presentation.theme.LeafShape

/** Yaprak formlu küçük rozet (etiket, kaynak, kategori göstergesi). */
@Composable
fun LeafBadge(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = modifier
            .background(container, LeafShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
