package com.example.hamgurgueseria_monchosos.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamgurgueseria_monchosos.data.Categoria
import com.example.hamgurgueseria_monchosos.data.Insumo
import com.example.hamgurgueseria_monchosos.data.InventarioRepository
import com.example.hamgurgueseria_monchosos.data.Unidad
import com.example.hamgurgueseria_monchosos.data.remoto.mensajeDeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventarioUiState(
    val insumos: List<Insumo> = emptyList(),
    /** null = mostrar todas las categorías. */
    val filtro: Categoria? = null,
) {
    val visibles: List<Insumo> get() = filtro?.let { f -> insumos.filter { it.categoria == f } } ?: insumos
    val porAgotarse: List<Insumo> get() = insumos.filter { it.bajoStock }
    val valorTotal: Double get() = insumos.sumOf { it.valorTotal }
}

class InventarioViewModel(private val repo: InventarioRepository) : ViewModel() {
    private val filtro = MutableStateFlow<Categoria?>(null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val estado: StateFlow<InventarioUiState> =
        combine(repo.insumos, filtro) { lista, f -> InventarioUiState(lista, f) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventarioUiState())

    fun filtrar(categoria: Categoria?) {
        filtro.value = categoria
    }

    fun refrescar() = ejecutar { repo.refrescar() }

    fun guardar(insumo: Insumo) = ejecutar { repo.guardar(insumo) }

    fun eliminar(insumo: Insumo) = ejecutar { repo.eliminar(insumo) }

    /** Botones rápidos: ±1 pieza o ±0.5 kg / litro. */
    fun sumar(insumo: Insumo, signo: Int) {
        val paso = if (insumo.unidad == Unidad.PIEZA) 1.0 else 0.5
        ejecutar { repo.ajustarCantidad(insumo, paso * signo) }
    }

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
