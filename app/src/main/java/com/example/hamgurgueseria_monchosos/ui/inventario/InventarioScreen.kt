package com.example.hamgurgueseria_monchosos.ui.inventario

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamgurgueseria_monchosos.data.Categoria
import com.example.hamgurgueseria_monchosos.data.Insumo
import com.example.hamgurgueseria_monchosos.data.formatearCantidad
import com.example.hamgurgueseria_monchosos.data.formatearDinero

@Composable
fun InventarioScreen(vm: InventarioViewModel) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var editando by remember { mutableStateOf<Insumo?>(null) }
    var creando by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
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
                        "Aún no hay insumos.\nAgrega pan, lechuga, jitomate, cajas…",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(estado.visibles, key = { it.id }) { insumo ->
                        InsumoFila(
                            insumo = insumo,
                            onClick = { editando = insumo },
                            onMenos = { vm.sumar(insumo, -1) },
                            onMas = { vm.sumar(insumo, +1) },
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { creando = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Text("+ Agregar insumo") }
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
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = if (insumo.bajoStock) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else CardDefaults.cardColors(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(insumo.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    buildString {
                        append(insumo.categoria.etiqueta)
                        if (insumo.costoUnitario > 0) {
                            append(" · ${formatearDinero(insumo.costoUnitario)}/${insumo.unidad.abreviatura}")
                        }
                        if (insumo.bajoStock) append(" · ¡Por agotarse!")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onMenos, modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) {
                Text("−")
            }
            Text(
                formatearCantidad(insumo.cantidad, insumo.unidad),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            FilledTonalButton(onClick = onMas, modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) {
                Text("+")
            }
        }
    }
}
