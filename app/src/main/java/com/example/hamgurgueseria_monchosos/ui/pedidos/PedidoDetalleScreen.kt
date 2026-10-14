package com.example.hamgurgueseria_monchosos.ui.pedidos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamgurgueseria_monchosos.data.formatearCentavos
import com.example.hamgurgueseria_monchosos.data.formatearFechaHora
import com.example.hamgurgueseria_monchosos.data.formatearHora
import com.example.hamgurgueseria_monchosos.data.parsearPesos
import com.example.hamgurgueseria_monchosos.data.pedidos.EstadoPedido
import com.example.hamgurgueseria_monchosos.data.pedidos.MetodoPago
import com.example.hamgurgueseria_monchosos.data.pedidos.PedidoCompleto
import com.example.hamgurgueseria_monchosos.data.remoto.mensajeDeError
import com.example.hamgurgueseria_monchosos.ui.comun.ActualizarPeriodicamente
import com.example.hamgurgueseria_monchosos.ui.comun.AvisoError

@Composable
fun PedidoDetalleScreen(
    pedidoId: Long,
    vm: PedidosViewModel,
    onEditar: () -> Unit,
    onEliminado: () -> Unit,
) {
    val flujo = remember(pedidoId) { vm.observar(pedidoId) }
    // null = todavía no llega la primera respuesta.
    val resultado by flujo.collectAsStateWithLifecycle(initialValue = null)
    val errorAccion by vm.error.collectAsStateWithLifecycle()
    // Si una consulta falla (sin internet), se sigue mostrando lo último que se vio.
    var ultimo by remember { mutableStateOf<PedidoCompleto?>(null) }
    val actual = resultado?.getOrNull()
    LaunchedEffect(actual) { if (actual != null) ultimo = actual }

    // Se vuelve a consultar cada 20 s por si el otro teléfono cobró o editó.
    ActualizarPeriodicamente { vm.volverAConsultar() }

    val r = resultado
    when {
        r == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        r.isSuccess && actual == null -> LaunchedEffect(Unit) { onEliminado() }   // lo borraron
        else -> Column(Modifier.fillMaxSize()) {
            AvisoError(r.exceptionOrNull()?.let(::mensajeDeError), onDescartar = {}, onReintentar = vm::volverAConsultar)
            AvisoError(errorAccion, onDescartar = vm::descartarError)
            (actual ?: ultimo)?.let { DetalleContenido(it, vm, onEditar) }
        }
    }
}

@Composable
private fun DetalleContenido(p: PedidoCompleto, vm: PedidosViewModel, onEditar: () -> Unit) {
    val pedido = p.pedido
    val cancelado = pedido.estado == EstadoPedido.CANCELADO
    var cobrando by remember { mutableStateOf(false) }
    var confirmarCancelar by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Encabezado ---------------------------------------------------------
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${pedido.tipoEntrega.emoji} ${pedido.tipoEntrega.etiqueta}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    EstadoTexto(pedido.estado)
                }
                Text(formatearFechaHora(pedido.creadoEn), style = MaterialTheme.typography.bodySmall)
                if (pedido.cliente.isNotBlank()) Text("👤 ${pedido.cliente}")
                if (pedido.telefono.isNotBlank()) Text("📞 ${pedido.telefono}")
                if (pedido.direccion.isNotBlank()) Text("📍 ${pedido.direccion}")
                if (pedido.notas.isNotBlank()) Text("📝 ${pedido.notas}")
            }
        }

        // Productos ----------------------------------------------------------
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                p.renglones.forEach { r ->
                    Row {
                        Column(Modifier.weight(1f)) { DescripcionRenglon(r, conCantidad = true) }
                        Text(formatearCentavos(r.subtotal), style = MaterialTheme.typography.titleMedium)
                    }
                }
                HorizontalDivider()
                Fila("Total", formatearCentavos(p.total), negritas = true)
                Fila("Pagado", formatearCentavos(p.pagado))
                if (!cancelado) Fila("Pendiente", formatearCentavos(p.pendiente), negritas = p.pendiente > 0)
            }
        }

        // Pagos ----------------------------------------------------------------
        if (p.pagos.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Pagos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    p.pagos.forEach { pago ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${pago.metodo.etiqueta} · ${formatearHora(pago.creadoEn)}",
                                modifier = Modifier.weight(1f),
                            )
                            Text(formatearCentavos(pago.monto))
                            if (!cancelado) TextButton(onClick = { vm.borrarPago(pago) }) { Text("Quitar") }
                        }
                    }
                }
            }
        }

        // Acciones -------------------------------------------------------------
        if (!cancelado && p.pendiente > 0) {
            Button(onClick = { cobrando = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Cobrar ${formatearCentavos(p.pendiente)}")
            }
        }
        if (!cancelado) {
            OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth()) { Text("Editar pedido") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cancelado) {
                TextButton(onClick = { vm.reabrir(pedido.id) }) { Text("Reabrir pedido") }
            } else {
                TextButton(onClick = {
                    if (confirmarCancelar) vm.cancelar(pedido.id) else confirmarCancelar = true
                }) {
                    Text(if (confirmarCancelar) "Toca otra vez para cancelar" else "Cancelar pedido",
                        color = MaterialTheme.colorScheme.error)
                }
            }
            TextButton(onClick = {
                // Al borrarse, el flujo emite null y la pantalla se cierra sola.
                if (confirmarEliminar) vm.eliminar(pedido.id) else confirmarEliminar = true
            }) {
                Text(if (confirmarEliminar) "Toca otra vez para eliminar" else "Eliminar",
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (cobrando) {
        CobrarDialog(
            pendiente = p.pendiente,
            onCobrar = { metodo, monto -> vm.cobrar(pedido.id, metodo, monto); cobrando = false },
            onCerrar = { cobrando = false },
        )
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String, negritas: Boolean = false) {
    val peso = if (negritas) FontWeight.Bold else FontWeight.Normal
    Row {
        Text(etiqueta, fontWeight = peso, modifier = Modifier.weight(1f))
        Text(valor, fontWeight = peso)
    }
}

/** Registrar un pago. En efectivo calcula el cambio a partir de "con cuánto paga". */
@Composable
private fun CobrarDialog(pendiente: Long, onCobrar: (MetodoPago, Long) -> Unit, onCerrar: () -> Unit) {
    var metodo by remember { mutableStateOf(MetodoPago.EFECTIVO) }
    var montoTexto by remember { mutableStateOf((pendiente / 100.0).let { if (pendiente % 100 == 0L) "${pendiente / 100}" else "$it" }) }
    var pagaCon by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val monto = parsearPesos(montoTexto)
    val recibido = parsearPesos(pagaCon)

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cobrar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetodoPago.entries.forEach { m ->
                        FilterChip(selected = metodo == m, onClick = { metodo = m }, label = { Text(m.etiqueta) })
                    }
                }
                OutlinedTextField(
                    value = montoTexto,
                    onValueChange = { montoTexto = it },
                    label = { Text("Monto a cobrar ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (metodo == MetodoPago.EFECTIVO) {
                    OutlinedTextField(
                        value = pagaCon,
                        onValueChange = { pagaCon = it },
                        label = { Text("¿Con cuánto paga? (opcional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (monto != null && recibido != null && recibido >= monto) {
                        Text(
                            "Cambio: ${formatearCentavos(recibido - monto)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    monto == null || monto <= 0 -> "Escribe un monto válido."
                    monto > pendiente -> "El monto no puede ser mayor a lo pendiente (${formatearCentavos(pendiente)})."
                    metodo == MetodoPago.EFECTIVO && recibido != null && recibido < monto -> "Paga con menos de lo que se cobra."
                    else -> null
                }
                if (error == null && monto != null) onCobrar(metodo, monto)
            }) { Text("Registrar pago") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}
