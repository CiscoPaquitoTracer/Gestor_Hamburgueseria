package com.example.hamgurgueseria_monchosos.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamgurgueseria_monchosos.data.formatearCantidad
import com.example.hamgurgueseria_monchosos.data.formatearCentavos
import com.example.hamgurgueseria_monchosos.data.formatearDinero
import com.example.hamgurgueseria_monchosos.data.formatearFechaLarga
import com.example.hamgurgueseria_monchosos.data.pedidos.MetodoPago
import com.example.hamgurgueseria_monchosos.ui.comun.ActualizarPeriodicamente
import com.example.hamgurgueseria_monchosos.ui.comun.AvisoError
import com.example.hamgurgueseria_monchosos.ui.comun.Dato
import com.example.hamgurgueseria_monchosos.ui.comun.EncabezadoSeccion
import com.example.hamgurgueseria_monchosos.ui.comun.Panel
import com.example.hamgurgueseria_monchosos.ui.comun.PanelDestacado
import com.example.hamgurgueseria_monchosos.ui.comun.TextoConIcono
import com.example.hamgurgueseria_monchosos.ui.inventario.InventarioViewModel
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidosViewModel
import com.example.hamgurgueseria_monchosos.ui.theme.Exito

/** Resumen rápido del negocio, al estilo del "balance" de Treinta. */
@Composable
fun InicioScreen(
    vm: InventarioViewModel,
    pedidosVm: PedidosViewModel,
    pronosticoVm: PronosticoViewModel,
    onNuevoPedido: () -> Unit,
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val hoy by pedidosVm.resumenHoy.collectAsStateWithLifecycle()
    val error by pedidosVm.error.collectAsStateWithLifecycle()
    val pronostico by pronosticoVm.pronostico.collectAsStateWithLifecycle()
    ActualizarPeriodicamente(cadaMs = 300_000) { pronosticoVm.refrescar() }
    ActualizarPeriodicamente {
        pedidosVm.refrescar()
        vm.refrescar()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvisoError(error, onDescartar = pedidosVm::descartarError, onReintentar = pedidosVm::refrescar)
        Text(
            formatearFechaLarga(System.currentTimeMillis()).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = onNuevoPedido, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Tomar pedido", style = MaterialTheme.typography.titleMedium)
        }

        EncabezadoSeccion("Hoy")
        PanelDestacado(Modifier.fillMaxWidth()) {
            Text("Vendido", style = MaterialTheme.typography.bodySmall)
            Text(formatearCentavos(hoy.ventas), style = MaterialTheme.typography.displaySmall)
            Text(
                "${hoy.pedidos} pedido${if (hoy.pedidos == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text("Cobrado", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(formatearCentavos(hoy.cobrado), style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider()
                MetodoPago.entries.forEach { m ->
                    Row {
                        Text(m.etiqueta, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatearCentavos(hoy.cobradoPorMetodo[m] ?: 0))
                    }
                }
                if (hoy.porCobrar > 0) {
                    TextoConIcono(
                        Icons.Outlined.Schedule,
                        "${hoy.porCobrar} pedido${if (hoy.porCobrar == 1) "" else "s"} por cobrar (${formatearCentavos(hoy.pendiente)})",
                        Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        pronostico?.let { PronosticoCard(it) }

        EncabezadoSeccion("Inventario")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Dato("Insumos registrados", estado.insumos.size.toString(), Modifier.weight(1f))
            Dato("Valor del inventario", formatearDinero(estado.valorTotal), Modifier.weight(1f))
        }

        Panel(
            Modifier.fillMaxWidth(),
            acento = if (estado.porAgotarse.isEmpty()) null else MaterialTheme.colorScheme.error,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (estado.porAgotarse.isEmpty()) {
                    TextoConIcono(Icons.Outlined.CheckCircle, "Nada por agotarse", color = Exito)
                } else {
                    Row {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Text("Por agotarse", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
                    }
                    estado.porAgotarse.forEach {
                        Row {
                            Text(it.nombre, Modifier.weight(1f))
                            Text(formatearCantidad(it.cantidad, it.unidad), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
