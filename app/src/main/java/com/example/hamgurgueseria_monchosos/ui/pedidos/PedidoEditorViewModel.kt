package com.example.hamgurgueseria_monchosos.ui.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamgurgueseria_monchosos.data.menu.CategoriaMenu
import com.example.hamgurgueseria_monchosos.data.pedidos.EstadoPedido
import com.example.hamgurgueseria_monchosos.data.pedidos.Pedido
import com.example.hamgurgueseria_monchosos.data.pedidos.PedidosRepository
import com.example.hamgurgueseria_monchosos.data.pedidos.Renglon
import com.example.hamgurgueseria_monchosos.data.pedidos.TipoEntrega
import com.example.hamgurgueseria_monchosos.data.remoto.mensajeDeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PasoEditor { MENU, RESUMEN }

data class EditorUiState(
    val pedido: Pedido = Pedido(),
    val renglones: List<Renglon> = emptyList(),
    val categoria: CategoriaMenu = CategoriaMenu.BURGERS,
    val paso: PasoEditor = PasoEditor.MENU,
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    /** Como estaba al abrir el editor, para saber si hay algo sin guardar. */
    val pedidoOriginal: Pedido = Pedido(),
    val renglonesOriginales: List<Renglon> = emptyList(),
) {
    val hayCambios: Boolean get() = pedido != pedidoOriginal || renglones != renglonesOriginales
    val total: Long get() = renglones.sumOf { it.subtotal }
    val articulos: Int get() = renglones.sumOf { it.cantidad }
}

/** Arma un pedido nuevo o edita uno existente ([pedidoId] != null). */
class PedidoEditorViewModel(
    private val repo: PedidosRepository,
    pedidoId: Long?,
) : ViewModel() {
    private val _estado = MutableStateFlow(EditorUiState(cargando = pedidoId != null))
    val estado: StateFlow<EditorUiState> = _estado.asStateFlow()

    init {
        if (pedidoId != null) {
            viewModelScope.launch {
                val resultado = runCatching { repo.obtener(pedidoId) }
                val existente = resultado.getOrNull()
                _estado.update {
                    when {
                        resultado.isFailure -> it.copy(cargando = false, error = mensajeDeError(resultado.exceptionOrNull()!!))
                        existente == null -> it.copy(cargando = false, error = "El pedido ya no existe.")
                        else -> it.copy(
                            pedido = existente.pedido,
                            renglones = existente.renglones,
                            pedidoOriginal = existente.pedido,
                            renglonesOriginales = existente.renglones,
                            paso = PasoEditor.RESUMEN,
                            cargando = false,
                        )
                    }
                }
            }
        }
    }

    fun elegirCategoria(c: CategoriaMenu) = _estado.update { it.copy(categoria = c) }

    fun irA(paso: PasoEditor) = _estado.update { it.copy(paso = paso, error = null) }

    /** Si ya hay un renglón igual, suma la cantidad en lugar de duplicarlo. */
    fun agregar(nuevo: Renglon) = _estado.update { s ->
        val i = s.renglones.indexOfFirst { it.mismoArticuloQue(nuevo) }
        val lista = if (i >= 0) {
            s.renglones.toMutableList().also { it[i] = it[i].copy(cantidad = it[i].cantidad + nuevo.cantidad) }
        } else s.renglones + nuevo
        s.copy(renglones = lista, error = null)
    }

    fun cambiarCantidad(indice: Int, delta: Int) = _estado.update { s ->
        val r = s.renglones.getOrNull(indice) ?: return@update s
        val nueva = r.cantidad + delta
        s.copy(
            renglones = if (nueva <= 0) s.renglones.filterIndexed { i, _ -> i != indice }
            else s.renglones.toMutableList().also { it[indice] = r.copy(cantidad = nueva) }
        )
    }

    fun cambiarNotas(indice: Int, notas: String) = _estado.update { s ->
        val r = s.renglones.getOrNull(indice) ?: return@update s
        s.copy(renglones = s.renglones.toMutableList().also { it[indice] = r.copy(notas = notas.trim()) })
    }

    fun actualizarPedido(cambio: (Pedido) -> Pedido) = _estado.update { it.copy(pedido = cambio(it.pedido)) }

    fun guardar(onListo: (Long) -> Unit) {
        val s = _estado.value
        val error = when {
            s.renglones.isEmpty() -> "Agrega al menos un producto."
            s.pedido.tipoEntrega == TipoEntrega.DOMICILIO && s.pedido.direccion.isBlank() ->
                "Escribe la dirección de entrega."
            s.pedido.estado == EstadoPedido.CANCELADO -> "No se puede editar un pedido cancelado."
            else -> null
        }
        if (error != null) {
            _estado.update { it.copy(error = error, paso = PasoEditor.RESUMEN) }
            return
        }
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val pedido = s.pedido.copy(
                cliente = s.pedido.cliente.trim(),
                telefono = s.pedido.telefono.trim(),
                direccion = if (s.pedido.tipoEntrega == TipoEntrega.DOMICILIO) s.pedido.direccion.trim() else "",
                notas = s.pedido.notas.trim(),
            )
            runCatching { repo.guardar(pedido, s.renglones) }
                .onSuccess { id ->
                    _estado.update { it.copy(guardando = false) }
                    onListo(id)
                }
                .onFailure { e -> _estado.update { it.copy(guardando = false, error = mensajeDeError(e)) } }
        }
    }
}
