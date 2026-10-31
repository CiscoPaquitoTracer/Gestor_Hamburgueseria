package com.example.hamgurgueseria_monchosos.ui.comun

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Pregunta antes de una acción que cambia datos o no se puede deshacer. [peligroso] pinta el botón
 * de confirmar en rojo (eliminar, cancelar…).
 */
@Composable
fun DialogoConfirmar(
    titulo: String,
    texto: String,
    accion: String,
    onConfirmar: () -> Unit,
    onCerrar: () -> Unit,
    peligroso: Boolean = false,
    cancelar: String = "Cancelar",
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = { onCerrar(); onConfirmar() }) {
                Text(accion, color = if (peligroso) MaterialTheme.colorScheme.error else Color.Unspecified)
            }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text(cancelar) } },
    )
}
