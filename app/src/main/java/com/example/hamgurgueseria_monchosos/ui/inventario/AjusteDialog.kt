package com.example.hamgurgueseria_monchosos.ui.inventario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hamgurgueseria_monchosos.data.Insumo
import com.example.hamgurgueseria_monchosos.data.Unidad
import com.example.hamgurgueseria_monchosos.data.formatearCantidad
import kotlin.math.abs

/** ±1 pieza o ±0.5 kg / litro por toque. */
fun pasoDe(insumo: Insumo): Double = if (insumo.unidad == Unidad.PIEZA) 1.0 else 0.5

/**
 * Confirma un ajuste de existencia antes de guardarlo. Se abre desde los botones − / + de la lista
 * con un paso ya aplicado ([signo]); aquí se puede seguir sumando o restando antes de guardar.
 */
@Composable
fun AjusteDialog(insumo: Insumo, signo: Int, onGuardar: (Double) -> Unit, onCerrar: () -> Unit) {
    val paso = pasoDe(insumo)
    var delta by remember { mutableDoubleStateOf(paso * signo) }
    val final = insumo.cantidad + delta
    val valido = delta != 0.0 && final >= 0

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(insumo.nombre) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Fila("Hay ahora", formatearCantidad(insumo.cantidad, insumo.unidad))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { delta -= paso }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Quitar ${formatearCantidad(paso, insumo.unidad)}")
                    }
                    Text(
                        (if (delta > 0) "+" else if (delta < 0) "−" else "") + formatearCantidad(abs(delta), insumo.unidad),
                        style = MaterialTheme.typography.headlineSmall,
                        color = when {
                            delta > 0 -> MaterialTheme.colorScheme.primary
                            delta < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(onClick = { delta += paso }) {
                        Icon(Icons.Filled.Add, contentDescription = "Sumar ${formatearCantidad(paso, insumo.unidad)}")
                    }
                }
                HorizontalDivider()
                Fila("Quedarán", if (final >= 0) formatearCantidad(final, insumo.unidad) else "—")
                if (final < 0) {
                    Text("No puedes quitar más de lo que hay.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(delta) }, enabled = valido) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun Fila(etiqueta: String, valor: String) {
    Row {
        Text(etiqueta, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleMedium)
    }
}
