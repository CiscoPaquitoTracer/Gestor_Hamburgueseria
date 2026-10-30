package com.example.hamgurgueseria_monchosos.ui.inventario

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamgurgueseria_monchosos.data.Categoria
import com.example.hamgurgueseria_monchosos.data.Insumo
import com.example.hamgurgueseria_monchosos.data.formatearCantidad
import com.example.hamgurgueseria_monchosos.data.formatearDinero
import com.example.hamgurgueseria_monchosos.ui.comun.ActualizarPeriodicamente
import com.example.hamgurgueseria_monchosos.ui.comun.AvisoError
import com.example.hamgurgueseria_monchosos.ui.comun.EncabezadoSeccion
import com.example.hamgurgueseria_monchosos.ui.comun.Panel

@Composable
fun InventarioScreen(vm: InventarioViewModel) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var editando by remember { mutableStateOf<Insumo?>(null) }
    var creando by remember { mutableStateOf(false) }
    // Insumo y dirección (−1 / +1) del ajuste que se está confirmando.
    var ajustando by remember { mutableStateOf<Pair<Insumo, Int>?>(null) }
    ActualizarPeriodicamente(cadaMs = 30_000) { vm.refrescar() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AvisoError(error, onDescartar = vm::descartarError, onReintentar = vm::refrescar)
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(estado.filtro == null, { vm.filtrar(null) }, { Text("Todos") })
                Categoria.entries.forEach { c ->
                    FilterChip(estado.filtro == c, { vm.filtrar(c) }, { Text(c.etiqueta) })
                }
            }

            if (estado.visibles.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        estado.filtro?.let { "No hay insumos en ${it.etiqueta.lowercase()}." }
                            ?: "Aún no hay insumos.\nAgrega pan, lechuga, jitomate, cajas…",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Una sección por categoría (carnes, lácteos…), en el orden del enum.
                    estado.visibles.groupBy { it.categoria }.toSortedMap().forEach { (categoria, insumos) ->
                        item(key = "seccion-${categoria.db}") {
                            EncabezadoSeccion("${categoria.etiqueta} · ${insumos.size}")
                        }
                        items(insumos, key = { it.id }) { insumo ->
                            InsumoFila(
                                insumo = insumo,
                                onClick = { editando = insumo },
                                onMenos = { ajustando = insumo to -1 },
                                onMas = { ajustando = insumo to +1 },
                            )
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { creando = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Agregar insumo") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    ajustando?.let { (insumo, signo) ->
        AjusteDialog(
            insumo = insumo,
            signo = signo,
            onGuardar = { vm.ajustar(insumo, it); ajustando = null },
            onCerrar = { ajustando = null },
        )
    }

    if (creando || editando != null) {
        InsumoDialog(
            inicial = editando,
            onGuardar = { vm.guardar(it); creando = false; editando = null },
            onEliminar = { vm.eliminar(it); editando = null },
            onCerrar = { creando = false; editando = null },
        )
    }
}

@Composable
private fun InsumoFila(insumo: Insumo, onClick: () -> Unit, onMenos: () -> Unit, onMas: () -> Unit) {
    Panel(
        Modifier.fillMaxWidth(),
        acento = if (insumo.bajoStock) MaterialTheme.colorScheme.error else null,
        onClick = onClick,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // El nombre se lleva el espacio que sobra; los controles tienen ancho fijo para que
            // los − / + queden alineados en columna sin importar el largo del nombre.
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    insumo.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (insumo.costoUnitario > 0) {
                    Text(
                        "${formatearDinero(insumo.costoUnitario)}/${insumo.unidad.abreviatura}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (insumo.bajoStock) {
                    Text("Por agotarse", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            FilledTonalIconButton(onClick = onMenos, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Uno menos", modifier = Modifier.size(18.dp))
            }
            Text(
                formatearCantidad(insumo.cantidad, insumo.unidad),
                modifier = Modifier.width(76.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            FilledTonalIconButton(onClick = onMas, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Uno más", modifier = Modifier.size(18.dp))
            }
        }
    }
}
