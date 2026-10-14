package com.example.hamgurgueseria_monchosos.data

/** Unidad en la que se cuenta un insumo. [db] = valor en Supabase. */
enum class Unidad(val etiqueta: String, val abreviatura: String, val db: String) {
    KILOGRAMO("Kilogramo", "kg", "kg"),
    PIEZA("Pieza", "pz", "pieza"),
    LITRO("Litro", "L", "litro"),
}

/** Tipo de insumo; cada uno sugiere una unidad por defecto. [db] = valor en Supabase. */
enum class Categoria(val etiqueta: String, val unidadSugerida: Unidad, val db: String) {
    INGREDIENTE("Ingrediente", Unidad.KILOGRAMO, "ingrediente"),
    DESECHABLE("Desechable", Unidad.PIEZA, "desechable"),
    OTRO("Otro", Unidad.PIEZA, "otro"),
}

/**
 * Algo que se compra y se gasta en el negocio:
 * ingredientes (pan, lechuga, jitomate... normalmente por kg)
 * o desechables (cajas, vasos, servilletas... por pieza).
 */
data class Insumo(
    val id: Long = 0,
    val nombre: String,
    val categoria: Categoria,
    val unidad: Unidad,
    val cantidad: Double,
    /** Por debajo de esta cantidad se marca como "por agotarse". 0 = sin alerta. */
    val stockMinimo: Double = 0.0,
    /** Costo por kg, pieza o litro, según [unidad]. */
    val costoUnitario: Double = 0.0,
) {
    val bajoStock: Boolean get() = stockMinimo > 0 && cantidad <= stockMinimo
    val valorTotal: Double get() = cantidad * costoUnitario
}

/** "2.5 kg", "40 pz", "1.5 L". Las piezas se muestran sin decimales. */
fun formatearCantidad(cantidad: Double, unidad: Unidad): String {
    val numero = when (unidad) {
        Unidad.PIEZA -> cantidad.toLong().toString()
        Unidad.KILOGRAMO, Unidad.LITRO -> "%.3f".format(java.util.Locale.US, cantidad).trimEnd('0').trimEnd('.')
    }
    return "$numero ${unidad.abreviatura}"
}

fun formatearDinero(monto: Double): String = "$" + "%,.2f".format(java.util.Locale.US, monto)
