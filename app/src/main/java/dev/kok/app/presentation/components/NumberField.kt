package dev.kok.app.presentation.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/** Ondalık sayı girişi (virgül ve nokta kabul edilir). */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    decimal: Boolean = true,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val allowed = if (decimal) input.filter { it.isDigit() || it == ',' || it == '.' } else input.filter(Char::isDigit)
            onValueChange(allowed.take(8))
        },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    )
}
