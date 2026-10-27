package com.example.hamgurgueseria_monchosos.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamgurgueseria_monchosos.auth.Usuario
import com.example.hamgurgueseria_monchosos.data.formatearCantidad
import com.example.hamgurgueseria_monchosos.data.formatearDinero
import com.example.hamgurgueseria_monchosos.data.formatearCentavos
import com.example.hamgurgueseria_monchosos.data.pedidos.MetodoPago
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidosViewModel
import com.example.hamgurgueseria_monchosos.ui.comun.ActualizarPeriodicamente
import com.example.hamgurgueseria_monchosos.ui.comun.AvisoError
import com.example.hamgurgueseria_monchosos.ui.inventario.InventarioViewModel

/** Resumen rápido del negocio, al estilo del "balance" de Treinta. */
@Composable
fun InicioScreen(
    usuario: Usuario,
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
        Text("Hola, ${usuario.nombre.substringBefore(' ')} 👋", style = MaterialTheme.typography.headlineSmall)

        Button(onClick = onNuevoPedido, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("🧾 Tomar pedido", style = MaterialTheme.typography.titleMedium)
        }

        Text("Hoy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tarjeta("Ventas", formatearCentavos(hoy.ventas), Modifier.weight(1f))
            Tarjeta("Pedidos", hoy.pedidos.toString(), Modifier.weight(1f))
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cobrado hoy: ${formatearCentavos(hoy.cobrado)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                MetodoPago.entries.forEach { m ->
                    Row {
                        Text(m.etiqueta, Modifier.weight(1f))
                        Text(formatearCentavos(hoy.cobradoPorMetodo[m] ?: 0))
                    }
                }
                if (hoy.porCobrar > 0) {
                    Text(
                        "⚠️ ${hoy.porCobrar} pedido${if (hoy.porCobrar == 1) "" else "s"} por cobrar (${formatearCentavos(hoy.pendiente)})",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        pronostico?.let { PronosticoCard(it) }

        Text("Inventario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tarjeta("Insumos registrados", estado.insumos.size.toString(), Modifier.weight(1f))
            Tarjeta("Valor del inventario", formatearDinero(estado.valorTotal), Modifier.weight(1f))
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Por agotarse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (estado.porAgotarse.isEmpty()) {
                    Text("Todo en orden ✅")
                } else {
                    estado.porAgotarse.forEach {
                        Text("• ${it.nombre}: quedan ${formatearCantidad(it.cantidad, it.unidad)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun Tarjeta(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium)
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
