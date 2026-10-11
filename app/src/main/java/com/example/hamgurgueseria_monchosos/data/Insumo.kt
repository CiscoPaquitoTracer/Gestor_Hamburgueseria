package com.example.hamgurgueseria_monchosos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Unidad en la que se cuenta un insumo. */
enum class Unidad(val etiqueta: String, val abreviatura: String) {
    KILOGRAMO("Kilogramo", "kg"),
    PIEZA("Pieza", "pz"),
}

/** Tipo de insumo; cada uno sugiere una unidad por defecto. */
enum class Categoria(val etiqueta: String, val unidadSugerida: Unidad) {
    INGREDIENTE("Ingrediente", Unidad.KILOGRAMO),
    DESECHABLE("Desechable", Unidad.PIEZA),
    OTRO("Otro", Unidad.PIEZA),
}

/**
 * Algo que se compra y se gasta en el negocio:
 * ingredientes (pan, lechuga, jitomate... normalmente por kg)
 * o desechables (cajas, vasos, servilletas... por pieza).
 */
@Entity(tableName = "insumos")
data class Insumo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val categoria: Categoria,
    val unidad: Unidad,
    val cantidad: Double,
    /** Por debajo de esta cantidad se marca como "por agotarse". 0 = sin alerta. */
    val stockMinimo: Double = 0.0,
    /** Costo por kg o por pieza, según [unidad]. */
    val costoUnitario: Double = 0.0,
) {
    val bajoStock: Boolean get() = stockMinimo > 0 && cantidad <= stockMinimo
    val valorTotal: Double get() = cantidad * costoUnitario
}

/** "2.5 kg", "40 pz". Las piezas se muestran sin decimales. */
fun formatearCantidad(cantidad: Double, unidad: Unidad): String {
    val numero = when (unidad) {
        Unidad.PIEZA -> cantidad.toLong().toString()
        Unidad.KILOGRAMO -> "%.3f".format(java.util.Locale.US, cantidad).trimEnd('0').trimEnd('.')
    }
    return "$numero ${unidad.abreviatura}"
}

fun formatearDinero(monto: Double): String = "$" + "%,.2f".format(java.util.Locale.US, monto)
