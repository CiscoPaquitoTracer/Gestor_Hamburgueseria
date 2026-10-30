package com.example.hamgurgueseria_monchosos.ui.pedidos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamgurgueseria_monchosos.data.formatearCentavos
import com.example.hamgurgueseria_monchosos.data.formatearFechaHora
import com.example.hamgurgueseria_monchosos.data.formatearHora
import com.example.hamgurgueseria_monchosos.data.inicioDelDia
import com.example.hamgurgueseria_monchosos.data.pedidos.EstadoPedido
import com.example.hamgurgueseria_monchosos.data.pedidos.PedidoConTotales
import com.example.hamgurgueseria_monchosos.ui.comun.ActualizarPeriodicamente
import com.example.hamgurgueseria_monchosos.ui.comun.AvisoError
import com.example.hamgurgueseria_monchosos.ui.comun.Panel
import com.example.hamgurgueseria_monchosos.ui.comun.icono
import com.example.hamgurgueseria_monchosos.ui.theme.Exito

@Composable
fun PedidosScreen(vm: PedidosViewModel, onNuevo: () -> Unit, onAbrir: (Long) -> Unit) {
    val s by vm.estado.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    // Cada 20 s: así se ven los pedidos que toma el otro teléfono.
    ActualizarPeriodicamente { vm.refrescar() }

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
                FiltroPedidos.entries.forEach { f ->
                    FilterChip(
                        selected = s.filtro == f,
                        onClick = { vm.filtrar(f) },
                        label = {
                            Text(if (f == FiltroPedidos.POR_COBRAR && s.porCobrar > 0) "${f.etiqueta} (${s.porCobrar})" else f.etiqueta)
                        },
                    )
                }
            }

            if (s.pedidos.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        when (s.filtro) {
                            FiltroPedidos.POR_COBRAR -> "No hay pedidos por cobrar."
                            else -> "Aún no hay pedidos.\nToca \"Nuevo pedido\" para tomar uno."
                        },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(s.pedidos, key = { it.pedido.id }) { p -> PedidoFila(p) { onAbrir(p.pedido.id) } }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNuevo,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nuevo pedido") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun PedidoFila(p: PedidoConTotales, onClick: () -> Unit) {
    val pedido = p.pedido
    val esDeHoy = pedido.creadoEn >= inicioDelDia()
    Panel(
        Modifier.fillMaxWidth(),
        acento = if (pedido.estado == EstadoPedido.ABIERTO) MaterialTheme.colorScheme.error else null,
        onClick = onClick,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                pedido.tipoEntrega.icono,
                contentDescription = pedido.tipoEntrega.etiqueta,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "#${pedido.id} · ${if (esDeHoy) formatearHora(pedido.creadoEn) else formatearFechaHora(pedido.creadoEn)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    listOf(pedido.cliente.ifBlank { pedido.tipoEntrega.etiqueta }, "${p.articulos} artículo${if (p.articulos == 1) "" else "s"}")
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatearCentavos(p.total), style = MaterialTheme.typography.titleMedium)
                EstadoTexto(pedido.estado, if (pedido.estado == EstadoPedido.ABIERTO && p.pagado > 0) p.pendiente else null)
            }
        }
    }
}

@Composable
fun EstadoTexto(estado: EstadoPedido, debe: Long? = null) {
    val color = when (estado) {
        EstadoPedido.ABIERTO -> MaterialTheme.colorScheme.error
        EstadoPedido.PAGADO -> Exito
        EstadoPedido.CANCELADO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        if (debe != null) "Debe ${formatearCentavos(debe)}" else estado.etiqueta,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}
