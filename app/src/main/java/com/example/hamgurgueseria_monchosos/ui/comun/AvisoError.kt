package com.example.hamgurgueseria_monchosos.ui.comun

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Franja roja con el error y botones para reintentar o descartar. No hace nada si [mensaje] es null. */
@Composable
fun AvisoError(
    mensaje: String?,
    onDescartar: () -> Unit,
    modifier: Modifier = Modifier,
    onReintentar: (() -> Unit)? = null,
) {
    if (mensaje == null) return
    Card(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                mensaje,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            )
            onReintentar?.let { TextButton(onClick = it) { Text("Reintentar") } }
            TextButton(onClick = onDescartar) { Text("OK") }
        }
    }
}

/** Vuelve a consultar el servidor cada [cadaMs] mientras la pantalla esté visible. */
@Composable
fun ActualizarPeriodicamente(cadaMs: Long = 20_000, actualizar: () -> Unit) {
    LaunchedEffect(Unit) {
        while (true) {
            actualizar()
            delay(cadaMs)
        }
    }
}
