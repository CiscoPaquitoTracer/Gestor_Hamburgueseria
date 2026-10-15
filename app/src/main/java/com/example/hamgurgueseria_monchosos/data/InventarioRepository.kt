package com.example.hamgurgueseria_monchosos.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Inventario en Supabase. La existencia nunca se edita directo: cada cambio pasa por
 * movimientos_inventario (funciones guardar_insumo y ajustar_existencia), así la bitácora cuadra.
 */
class InventarioRepository(private val supabase: SupabaseClient) {
    private val _insumos = MutableStateFlow<List<Insumo>>(emptyList())
    val insumos: StateFlow<List<Insumo>> = _insumos.asStateFlow()

    suspend fun refrescar() {
        _insumos.value = supabase.from("insumos").select {
            filter { eq("activo", true) }
            order("categoria", Order.ASCENDING)
            order("nombre", Order.ASCENDING)
        }.decodeList<InsumoFila>().map { it.aModelo() }
    }

    suspend fun guardar(insumo: Insumo) {
        supabase.postgrest.rpc("guardar_insumo", buildJsonObject {
            put("datos", buildJsonObject {
                if (insumo.id != 0L) put("id", insumo.id)
                put("nombre", insumo.nombre)
                put("categoria", insumo.categoria.db)
                put("unidad", insumo.unidad.db)
                put("cantidad", insumo.cantidad)
                put("stock_minimo", insumo.stockMinimo)
                put("costo_unitario", insumo.costoUnitario)
            })
        })
        refrescar()
    }

    /** No se borra (tiene historial y recetas): se desactiva. */
    suspend fun eliminar(insumo: Insumo) {
        supabase.from("insumos").update({ set("activo", false) }) { filter { eq("id", insumo.id) } }
        refrescar()
    }

    suspend fun ajustarCantidad(insumo: Insumo, delta: Double) {
        supabase.postgrest.rpc("ajustar_existencia", buildJsonObject {
            put("p_insumo_id", insumo.id)
            put("p_delta", delta)
        })
        refrescar()
    }
}

@Serializable
private data class InsumoFila(
    val id: Long,
    val nombre: String,
    val categoria: String,
    val unidad: String,
    val cantidad: Double,
    @SerialName("stock_minimo") val stockMinimo: Double = 0.0,
    @SerialName("costo_unitario") val costoUnitario: Double = 0.0,
) {
    fun aModelo() = Insumo(
        id = id,
        nombre = nombre,
        categoria = Categoria.entries.firstOrNull { it.db == categoria } ?: Categoria.OTRO,
        unidad = Unidad.entries.firstOrNull { it.db == unidad } ?: Unidad.PIEZA,
        cantidad = cantidad,
        stockMinimo = stockMinimo,
        costoUnitario = costoUnitario,
    )
}
