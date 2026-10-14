package com.example.hamgurgueseria_monchosos.ui.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamgurgueseria_monchosos.data.inicioDelDia
import com.example.hamgurgueseria_monchosos.data.pedidos.EstadoPedido
import com.example.hamgurgueseria_monchosos.data.pedidos.MetodoPago
import com.example.hamgurgueseria_monchosos.data.pedidos.Pago
import com.example.hamgurgueseria_monchosos.data.pedidos.PedidoCompleto
import com.example.hamgurgueseria_monchosos.data.pedidos.PedidoConTotales
import com.example.hamgurgueseria_monchosos.data.pedidos.PedidosRepository
import com.example.hamgurgueseria_monchosos.data.pedidos.ResumenDia
import com.example.hamgurgueseria_monchosos.data.pedidos.resumenDelDia
import com.example.hamgurgueseria_monchosos.data.remoto.mensajeDeError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FiltroPedidos(val etiqueta: String) {
    POR_COBRAR("Por cobrar"),
    HOY("Hoy"),
    TODOS("Todos"),
}

data class PedidosUiState(
    val pedidos: List<PedidoConTotales> = emptyList(),
    val filtro: FiltroPedidos = FiltroPedidos.POR_COBRAR,
    val porCobrar: Int = 0,
)

class PedidosViewModel(private val repo: PedidosRepository) : ViewModel() {
    private val filtro = MutableStateFlow(FiltroPedidos.POR_COBRAR)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val estado: StateFlow<PedidosUiState> =
        combine(repo.pedidos, filtro) { todos, f ->
            val hoy = inicioDelDia()
            PedidosUiState(
                pedidos = when (f) {
                    FiltroPedidos.POR_COBRAR -> todos.filter { it.pedido.estado == EstadoPedido.ABIERTO }
                    FiltroPedidos.HOY -> todos.filter { it.pedido.creadoEn >= hoy }
                    FiltroPedidos.TODOS -> todos
                },
                filtro = f,
                porCobrar = todos.count { it.pedido.estado == EstadoPedido.ABIERTO },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PedidosUiState())

    val resumenHoy: StateFlow<ResumenDia> =
        combine(repo.pedidos, repo.pagosHoy) { pedidos, pagos ->
            resumenDelDia(pedidos, pagos, inicioDelDia())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumenDia())

    fun filtrar(f: FiltroPedidos) {
        filtro.value = f
    }

    fun observar(id: Long): Flow<Result<PedidoCompleto?>> = repo.observar(id)

    fun refrescar() = ejecutar { repo.refrescar() }

    fun volverAConsultar() = repo.volverAConsultar()

    fun cobrar(pedidoId: Long, metodo: MetodoPago, monto: Long) = ejecutar { repo.cobrar(pedidoId, metodo, monto) }

    fun borrarPago(pago: Pago) = ejecutar { repo.borrarPago(pago) }

    fun cancelar(id: Long) = ejecutar { repo.cancelar(id) }

    fun reabrir(id: Long) = ejecutar { repo.reabrir(id) }

    fun eliminar(id: Long) = ejecutar { repo.eliminar(id) }

    fun descartarError() {
        _error.value = null
    }

    private fun ejecutar(accion: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { accion() }
                .onSuccess { _error.value = null }
                .onFailure { _error.value = mensajeDeError(it) }
        }
    }
}
