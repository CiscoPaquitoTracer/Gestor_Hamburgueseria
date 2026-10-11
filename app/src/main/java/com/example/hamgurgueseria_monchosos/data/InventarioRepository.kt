package com.example.hamgurgueseria_monchosos.data

import kotlinx.coroutines.flow.Flow

/**
 * Punto único de acceso al inventario. Hoy guarda en el teléfono (Room);
 * si después se quiere sincronizar entre dos teléfonos, aquí es donde se conecta la nube.
 */
class InventarioRepository(private val dao: InsumoDao) {
    val insumos: Flow<List<Insumo>> = dao.observarTodos()

    suspend fun guardar(insumo: Insumo) = dao.guardar(insumo)

    suspend fun eliminar(insumo: Insumo) = dao.eliminar(insumo)

    suspend fun ajustarCantidad(insumo: Insumo, delta: Double) = dao.ajustarCantidad(insumo.id, delta)
}
