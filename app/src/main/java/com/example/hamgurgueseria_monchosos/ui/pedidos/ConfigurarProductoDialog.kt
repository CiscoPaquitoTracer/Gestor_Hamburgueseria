package com.example.hamgurgueseria_monchosos.ui.pedidos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hamgurgueseria_monchosos.data.formatearCentavos
import com.example.hamgurgueseria_monchosos.data.menu.Eleccion
import com.example.hamgurgueseria_monchosos.data.menu.Menu
import com.example.hamgurgueseria_monchosos.data.menu.OpcionCombo
import com.example.hamgurgueseria_monchosos.data.menu.ProductoMenu
import com.example.hamgurgueseria_monchosos.data.menu.armarRenglon
import com.example.hamgurgueseria_monchosos.data.pedidos.Renglon

/** Elegir tamaño, sabor, opciones de combo, cantidad y notas antes de agregar al pedido. */
@Composable
fun ConfigurarProductoDialog(
    producto: ProductoMenu,
    onAgregar: (Renglon) -> Unit,
    onCerrar: () -> Unit,
) {
    var varianteId by remember { mutableStateOf(producto.variantesActivas.singleOrNull()?.id) }
    var sabor by remember { mutableStateOf<String?>(null) }
    var cantidad by remember { mutableIntStateOf(1) }
    var notas by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // Un lugar por cada unidad de cada grupo. Si el grupo tiene una sola opción, ya va elegida.
    val lugares = remember { producto.grupos.flatMap { g -> List(g.cantidad) { i -> g to i } } }
    val elecciones = remember {
        mutableStateListOf<Eleccion?>().apply {
            lugares.forEach { (g, _) -> add(g.opciones.filter(::opcionDisponible).singleOrNull()?.let { Eleccion(it) }) }
        }
    }

    val precioUnitario = producto.variante(varianteId)?.precio ?: producto.precio ?: producto.precioDesde

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(Menu.nombreCompleto(producto)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (producto.descripcion.isNotBlank()) {
                    Text(producto.descripcion, style = MaterialTheme.typography.bodySmall)
                }

                if (producto.variantesActivas.isNotEmpty()) {
                    Etiqueta("Tamaño")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        producto.variantesActivas.forEach { v ->
                            FilterChip(
                                selected = varianteId == v.id,
                                onClick = { varianteId = v.id },
                                label = { Text("${v.nombre} · ${formatearCentavos(v.precio)}") },
                            )
                        }
                    }
                }

                if (producto.conSabor) {
                    Etiqueta("Sabor")
                    SelectorSabor(sabor) { sabor = it }
                }

                lugares.forEachIndexed { i, (grupo, n) ->
                    HorizontalDivider()
                    Etiqueta(if (grupo.cantidad > 1) "${grupo.nombre} ${n + 1} de ${grupo.cantidad}" else grupo.nombre)
                    val actual = elecciones[i]
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        grupo.opciones.filter(::opcionDisponible).forEach { op ->
                            FilterChip(
                                selected = actual?.opcion == op,
                                onClick = { elecciones[i] = Eleccion(op) },
                                label = { Text(Menu.nombreOpcion(op)) },
                            )
                        }
                    }
                    if (actual != null && Menu.producto(actual.opcion.productoId).conSabor) {
                        SelectorSabor(actual.sabor) { elecciones[i] = actual.copy(sabor = it) }
                    }
                }

                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Etiqueta("Cantidad", Modifier.weight(1f))
                    TextButton(onClick = { if (cantidad > 1) cantidad-- }) { Text("−") }
                    Text("$cantidad", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { cantidad++ }) { Text("+") }
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (ej. sin cebolla)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                armarRenglon(producto, varianteId, sabor, elecciones.toList(), cantidad, notas)
                    .onSuccess(onAgregar)
                    .onFailure { error = it.message }
            }) { Text("Agregar ${formatearCentavos(precioUnitario * cantidad)}") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun SelectorSabor(actual: String?, onElegir: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Menu.sabores.forEach { s ->
            FilterChip(selected = actual == s, onClick = { onElegir(s) }, label = { Text(s) })
        }
    }
}

@Composable
private fun Etiqueta(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 4.dp),
    )
}

/** Una opción de combo se ofrece solo si su producto (y su tamaño, si aplica) está disponible. */
private fun opcionDisponible(op: OpcionCombo): Boolean {
    val p = Menu.productoONull(op.productoId) ?: return false
    return p.activo && (op.varianteId == null || p.variante(op.varianteId)?.activo == true)
}
