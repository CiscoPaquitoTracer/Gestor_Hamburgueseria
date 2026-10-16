package com.example.hamgurgueseria_monchosos.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamgurgueseria_monchosos.data.menu.CategoriaMenu
import com.example.hamgurgueseria_monchosos.data.menu.Menu
import com.example.hamgurgueseria_monchosos.data.menu.MenuRepository
import com.example.hamgurgueseria_monchosos.data.menu.ProductoMenu
import com.example.hamgurgueseria_monchosos.data.menu.Variante
import com.example.hamgurgueseria_monchosos.data.remoto.mensajeDeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MenuUiState(
    val productos: List<ProductoMenu> = emptyList(),
    val categoria: CategoriaMenu = CategoriaMenu.BURGERS,
) {
    val visibles: List<ProductoMenu> get() = productos.filter { it.categoria == categoria }
}

/** Lo que se está editando: un producto, o uno de sus tamaños. */
data class EditandoPrecio(val producto: ProductoMenu, val variante: Variante? = null) {
    val precio: Long get() = variante?.precio ?: producto.precio ?: 0
    val disponible: Boolean get() = variante?.activo ?: producto.activo
    val titulo: String get() = listOfNotNull(Menu.nombreCompleto(producto), variante?.nombre).joinToString(" · ")
}

class MenuViewModel(private val repo: MenuRepository) : ViewModel() {
    private val categoria = MutableStateFlow(CategoriaMenu.BURGERS)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val estado: StateFlow<MenuUiState> =
        combine(Menu.catalogo, categoria) { lista, c -> MenuUiState(lista, c) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState())

    fun elegirCategoria(c: CategoriaMenu) {
        categoria.value = c
    }

    fun refrescar() = ejecutar { repo.refrescar() }

    /** Guarda solo lo que cambió (cada cambio de precio queda en el historial). */
    fun guardar(edicion: EditandoPrecio, precio: Long, disponible: Boolean) = ejecutar {
        if (precio != edicion.precio) repo.cambiarPrecio(edicion.producto, edicion.variante, precio)
        if (disponible != edicion.disponible) repo.cambiarDisponible(edicion.producto, edicion.variante, disponible)
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
