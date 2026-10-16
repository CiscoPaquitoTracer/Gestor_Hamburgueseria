package com.example.hamgurgueseria_monchosos.data.menu

import com.example.hamgurgueseria_monchosos.data.remoto.aCentavos
import com.example.hamgurgueseria_monchosos.data.remoto.aPesos
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Precios y disponibilidad del menú en Supabase. Cada cambio de precio queda en
 * historial_precios (trigger de la base), y los pedidos ya hechos guardan el precio con
 * que se vendieron, así que cambiar un precio no altera ventas pasadas.
 */
class MenuRepository(private val supabase: SupabaseClient) {

    suspend fun refrescar() {
        val filas = supabase.from("productos")
            .select(Columns.raw("id, nombre, precio, activo, categorias_menu(nombre), variantes(id, nombre, precio, activo)"))
            .decodeList<ProductoFila>()
        Menu.actualizar(filas.map { it.aModelo() })
    }

    /** Cambia el precio del producto, o el de [variante] si el producto se vende por tamaño. */
    suspend fun cambiarPrecio(producto: ProductoMenu, variante: Variante?, centavos: Long) {
        require(centavos > 0) { "El precio debe ser mayor a 0" }
        if (variante != null) {
            supabase.from("variantes").update({ set("precio", aPesos(centavos)) }) {
                filter { eq("id", idDe(variante.dbId)) }
            }
        } else {
            supabase.from("productos").update({ set("precio", aPesos(centavos)) }) {
                filter { eq("id", idDe(producto.dbId)) }
            }
        }
        refrescar()
    }

    suspend fun cambiarDisponible(producto: ProductoMenu, variante: Variante?, disponible: Boolean) {
        if (variante != null) {
            supabase.from("variantes").update({ set("activo", disponible) }) {
                filter { eq("id", idDe(variante.dbId)) }
            }
        } else {
            supabase.from("productos").update({ set("activo", disponible) }) {
                filter { eq("id", idDe(producto.dbId)) }
            }
        }
        refrescar()
    }

    /** dbId = 0 significa que el menú aún no se cargó de Supabase: no se puede editar a ciegas. */
    private fun idDe(dbId: Long): Long {
        check(dbId != 0L) { "El menú aún no carga. Revisa tu conexión e intenta de nuevo." }
        return dbId
    }
}

// ---------------------------------------------------------------- Filas de Supabase

@Serializable
private data class ProductoFila(
    val id: Long,
    val nombre: String,
    val precio: Double? = null,
    val activo: Boolean = true,
    @SerialName("categorias_menu") val categoria: CategoriaFila,
    val variantes: List<VarianteFila> = emptyList(),
) {
    fun aModelo() = PrecioDb(
        id = id,
        categoriaDb = categoria.nombre,
        nombre = nombre,
        precio = precio?.let(::aCentavos),
        activo = activo,
        variantes = variantes.map { VarianteDb(it.id, it.nombre, aCentavos(it.precio), it.activo) },
    )
}

@Serializable
private data class CategoriaFila(val nombre: String)

@Serializable
private data class VarianteFila(val id: Long, val nombre: String, val precio: Double, val activo: Boolean = true)
