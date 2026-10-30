package com.example.hamgurgueseria_monchosos.ui.inventario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.hamgurgueseria_monchosos.data.Categoria
import com.example.hamgurgueseria_monchosos.data.Insumo
import com.example.hamgurgueseria_monchosos.data.Unidad
import com.example.hamgurgueseria_monchosos.data.formatearCantidad
import com.example.hamgurgueseria_monchosos.ui.comun.DialogoConfirmar

/** Alta / edición de un insumo. [inicial] null = nuevo. */
@Composable
fun InsumoDialog(
    inicial: Insumo?,
    onGuardar: (Insumo) -> Unit,
    onEliminar: (Insumo) -> Unit,
    onCerrar: () -> Unit,
) {
    var form by remember { mutableStateOf(inicial?.let(FormularioInsumo::desde) ?: FormularioInsumo()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf<Insumo?>(null) }
    val abrev = form.unidad.abreviatura

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (inicial == null) "Nuevo insumo" else "Editar insumo") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = form.nombre,
                    onValueChange = { form = form.copy(nombre = it) },
                    label = { Text("Nombre (ej. Pan, Lechuga, Caja)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Tipo", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Categoria.entries.forEach { c ->
                        FilterChip(
                            selected = form.categoria == c,
                            onClick = { form = form.copy(categoria = c, unidad = c.unidadSugerida) },
                            label = { Text(c.etiqueta) },
                        )
                    }
                }

                Text("Se cuenta por", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Unidad.entries.forEach { u ->
                        FilterChip(
                            selected = form.unidad == u,
                            onClick = { form = form.copy(unidad = u) },
                            label = { Text(u.etiqueta) },
                        )
                    }
                }

                CampoNumero("Cantidad actual ($abrev)", form.cantidad) { form = form.copy(cantidad = it) }
                CampoNumero("Avisar cuando queden ($abrev), opcional", form.stockMinimo) {
                    form = form.copy(stockMinimo = it)
                }
                CampoNumero("Costo por $abrev, opcional", form.costoUnitario) {
                    form = form.copy(costoUnitario = it)
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                if (inicial != null) {
                    TextButton(onClick = { confirmarBorrado = true }) {
                        Text("Eliminar insumo", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                form.validar()
                    .onSuccess { guardando = it }
                    .onFailure { error = it.message }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )

    guardando?.let { insumo ->
        DialogoConfirmar(
            titulo = if (inicial == null) "¿Agregar ${insumo.nombre}?" else "¿Guardar cambios?",
            texto = if (inicial == null) {
                "Se dará de alta con ${formatearCantidad(insumo.cantidad, insumo.unidad)}."
            } else {
                "Se actualizará ${insumo.nombre} en los dos teléfonos."
            },
            accion = if (inicial == null) "Agregar" else "Guardar",
            onConfirmar = { onGuardar(insumo) },
            onCerrar = { guardando = null },
        )
    }

    if (confirmarBorrado && inicial != null) {
        DialogoConfirmar(
            titulo = "¿Eliminar ${inicial.nombre}?",
            texto = "Dejará de aparecer en el inventario y en el pronóstico de compras.",
            accion = "Eliminar",
            peligroso = true,
            onConfirmar = { onEliminar(inicial) },
            onCerrar = { confirmarBorrado = false },
        )
    }
}

@Composable
private fun CampoNumero(etiqueta: String, valor: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
