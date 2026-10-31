package com.example.hamgurgueseria_monchosos.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamgurgueseria_monchosos.data.formatearCentavos
import com.example.hamgurgueseria_monchosos.data.menu.CategoriaMenu
import com.example.hamgurgueseria_monchosos.data.menu.ProductoMenu
import com.example.hamgurgueseria_monchosos.data.parsearPesos
import com.example.hamgurgueseria_monchosos.ui.comun.ActualizarPeriodicamente
import com.example.hamgurgueseria_monchosos.ui.comun.AvisoError
import com.example.hamgurgueseria_monchosos.ui.comun.DialogoConfirmar
import com.example.hamgurgueseria_monchosos.ui.comun.Panel

/** Precios y disponibilidad del menú. Los cambios se ven en los dos teléfonos. */
@Composable
fun MenuScreen(vm: MenuViewModel) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var editando by remember { mutableStateOf<EditandoPrecio?>(null) }
    ActualizarPeriodicamente(cadaMs = 60_000) { vm.refrescar() }

    Column(Modifier.fillMaxSize()) {
        AvisoError(error, onDescartar = vm::descartarError, onReintentar = vm::refrescar)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoriaMenu.entries.forEach { c ->
                FilterChip(estado.categoria == c, { vm.elegirCategoria(c) }, { Text(c.etiqueta) })
            }
        }
        Text(
            "Toca un precio para cambiarlo. Los pedidos ya hechos conservan su precio.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(estado.visibles, key = { it.id }) { producto ->
                ProductoPrecios(producto, onEditar = { editando = it })
            }
        }
    }

    editando?.let { e ->
        PrecioDialog(
            edicion = e,
            onGuardar = { precio, disponible -> vm.guardar(e, precio, disponible); editando = null },
            onCerrar = { editando = null },
        )
    }
}

@Composable
private fun ProductoPrecios(producto: ProductoMenu, onEditar: (EditandoPrecio) -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            if (producto.variantes.isEmpty()) {
                FilaPrecio(
                    titulo = producto.nombre,
                    detalle = producto.descripcion,
                    precio = producto.precio,
                    disponible = producto.activo,
                    onClick = { onEditar(EditandoPrecio(producto)) },
                )
            } else {
                Text(
                    producto.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                producto.variantes.forEachIndexed { i, v ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                    FilaPrecio(
                        titulo = v.nombre,
                        detalle = "",
                        precio = v.precio,
                        // Si el producto entero no está disponible, ninguno de sus tamaños lo está.
                        disponible = v.activo && producto.activo,
                        onClick = { onEditar(EditandoPrecio(producto, v)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaPrecio(titulo: String, detalle: String, precio: Long?, disponible: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .alpha(if (disponible) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (!disponible) {
                Text("No disponible", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            } else if (detalle.isNotBlank()) {
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            precio?.let(::formatearCentavos) ?: "—",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
        Icon(
            Icons.Outlined.Edit,
            contentDescription = "Cambiar precio",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp).size(18.dp),
        )
    }
}

@Composable
private fun PrecioDialog(edicion: EditandoPrecio, onGuardar: (Long, Boolean) -> Unit, onCerrar: () -> Unit) {
    var texto by remember { mutableStateOf(formatearCentavos(edicion.precio).removePrefix("$").replace(",", "")) }
    var disponible by remember { mutableStateOf(edicion.disponible) }
    var error by remember { mutableStateOf<String?>(null) }
    var porGuardar by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(edicion.titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it; error = null },
                    label = { Text("Precio") },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Antes: ${formatearCentavos(edicion.precio)}") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Disponible", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Apágalo si se acabó: no aparecerá al tomar pedidos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = disponible, onCheckedChange = { disponible = it })
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val precio = parsearPesos(texto)
                when {
                    precio == null -> error = "Escribe un precio válido (ej. 95 o 95.50)."
                    precio <= 0 -> error = "El precio debe ser mayor a 0."
                    precio == edicion.precio && disponible == edicion.disponible -> onCerrar()
                    else -> porGuardar = precio
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )

    porGuardar?.let { precio ->
        DialogoConfirmar(
            titulo = "¿Guardar cambios en ${edicion.titulo}?",
            texto = buildList {
                if (precio != edicion.precio) {
                    add("Precio: ${formatearCentavos(edicion.precio)} → ${formatearCentavos(precio)}.")
                }
                if (disponible != edicion.disponible) {
                    add(if (disponible) "Vuelve a estar disponible." else "Deja de aparecer al tomar pedidos.")
                }
                add("Se aplica en los dos teléfonos; los pedidos ya hechos conservan su precio.")
            }.joinToString("\n"),
            accion = "Guardar",
            onConfirmar = { onGuardar(precio, disponible) },
            onCerrar = { porGuardar = null },
        )
    }
}
