package com.example.hamgurgueseria_monchosos.ui.resumen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamgurgueseria_monchosos.data.formatearCentavos
import com.example.hamgurgueseria_monchosos.data.pedidos.MetodoPago
import com.example.hamgurgueseria_monchosos.data.resumen.Resumen
import com.example.hamgurgueseria_monchosos.ui.comun.AvisoError
import kotlin.math.abs

private val DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

/** Lo que antes sacaban del Excel: cuánto se vendió, se cobró y se ganó en el mes. */
@Composable
fun ResumenScreen(vm: ResumenViewModel) {
    val s by vm.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refrescar() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.moverMes(-1) }) { Text("◀") }
            Text(
                s.titulo,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = { vm.moverMes(+1) }, enabled = !s.esMesActual) { Text("▶") }
        }
        AvisoError(s.error, onDescartar = vm::descartarError, onReintentar = vm::refrescar)

        val r = s.resumen
        when {
            s.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            r == null -> Unit
            r.sinDatos -> Text(
                "No hay ventas ni gastos registrados en este mes.",
                Modifier.fillMaxWidth().padding(32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> Contenido(r)
        }
    }
}

@Composable
private fun Contenido(r: Resumen) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Vendido en el mes", style = MaterialTheme.typography.labelLarge)
                Text(formatearCentavos(r.ventas), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                r.cambioVsAnterior?.let {
                    val sube = it >= 0
                    Text(
                        "${if (sube) "▲" else "▼"} ${(abs(it) * 100).toInt()} % vs mes anterior (${formatearCentavos(r.ventasMesAnterior)})",
                        color = if (sube) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (r.ventasEstimadas > 0) {
                    Text(
                        "Incluye ${formatearCentavos(r.ventasEstimadas)} estimados del Excel (con los precios de hoy).",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dato("Pedidos", r.pedidos.toString(), Modifier.weight(1f))
            Dato("Por pedido", r.ticketPromedio?.let(::formatearCentavos) ?: "—", Modifier.weight(1f))
            Dato("Días abiertos", r.diasAbiertos.toString(), Modifier.weight(1f))
        }

        Seccion("Cobrado") {
            MetodoPago.entries.forEach { m -> Renglon(m.etiqueta, formatearCentavos(r.cobrado[m] ?: 0)) }
            if (r.porCobrar > 0) Renglon("Falta por cobrar", formatearCentavos(r.porCobrar), MaterialTheme.colorScheme.error)
            if (r.descuentos > 0) Renglon("Descuentos dados", formatearCentavos(r.descuentos))
        }

        Seccion("Ganancia estimada") {
            Renglon("Ventas", formatearCentavos(r.ventas))
            Renglon("− Costo de lo vendido", formatearCentavos(r.costoInsumos))
            Renglon("− Gastos", formatearCentavos(r.gastos))
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Renglon(
                "Ganancia",
                formatearCentavos(r.ganancia),
                if (r.ganancia >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                negrita = true,
            )
            if (r.compras > 0) Renglon("Compras de insumos del mes", formatearCentavos(r.compras))
            if (r.insumosSinCosto > 0) {
                Text(
                    "Algunos insumos no tienen costo registrado: el costo de lo vendido sale más bajo de lo real. " +
                        "Ponles costo en Inventario.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (r.topProductos.isNotEmpty()) {
            Seccion("Lo más vendido") {
                r.topProductos.forEachIndexed { i, p ->
                    Renglon("${i + 1}. ${p.nombre} ×${p.cantidad.toInt()}", formatearCentavos(p.total))
                }
            }
        }

        if (r.porDiaSemana.isNotEmpty()) {
            val maximo = r.porDiaSemana.maxOf { it.promedio }.coerceAtLeast(1)
            Seccion("Promedio por día") {
                r.porDiaSemana.forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(DIAS[d.diaSemana - 1], Modifier.width(88.dp))
                        LinearProgressIndicator(
                            progress = { d.promedio.toFloat() / maximo },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        Text(formatearCentavos(d.promedio))
                    }
                }
                Text(
                    "Promedio vendido en los días que abrieron.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Dato(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium)
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            contenido()
        }
    }
}

@Composable
private fun Renglon(etiqueta: String, valor: String, color: Color = Color.Unspecified, negrita: Boolean = false) {
    Row {
        Text(etiqueta, Modifier.weight(1f), color = color, fontWeight = if (negrita) FontWeight.Bold else null)
        Text(valor, color = color, fontWeight = if (negrita) FontWeight.Bold else null)
    }
}
