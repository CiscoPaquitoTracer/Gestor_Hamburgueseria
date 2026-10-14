package com.example.hamgurgueseria_monchosos.data.menu

import com.example.hamgurgueseria_monchosos.data.pedidos.Parte
import com.example.hamgurgueseria_monchosos.data.pedidos.Renglon

/** Lo que el cliente eligió para un lugar de un combo (ej. "Hamburguesa 1 de 2"). */
data class Eleccion(val opcion: OpcionCombo, val sabor: String? = null)

/**
 * Convierte lo elegido en pantalla en un [Renglon], validando contra el menú:
 * variante obligatoria si existe, sabor obligatorio en alitas/boneless,
 * y en combos cada lugar lleno con una opción permitida.
 *
 * [elecciones] trae un lugar por cada unidad de cada grupo, en orden
 * (ej. Desmadre de Pareja: 2 hamburguesas + 1 papas + 2 refrescos = 5 lugares).
 */
fun armarRenglon(
    producto: ProductoMenu,
    varianteId: String? = null,
    sabor: String? = null,
    elecciones: List<Eleccion?> = emptyList(),
    cantidad: Int = 1,
    notas: String = "",
): Result<Renglon> {
    fun falla(msg: String) = Result.failure<Renglon>(IllegalArgumentException(msg))

    if (cantidad < 1) return falla("La cantidad debe ser al menos 1.")

    val variante = producto.variante(varianteId)
    if (producto.variantes.isNotEmpty() && variante == null) return falla("Elige el tamaño.")
    if (producto.conSabor && sabor !in Menu.sabores) return falla("Elige el sabor.")

    var componentes: String? = null
    var partes: List<Parte> = emptyList()
    if (producto.esCombo) {
        val lugares = producto.grupos.flatMap { g -> List(g.cantidad) { g } }
        if (elecciones.size != lugares.size || elecciones.any { it == null }) {
            return falla("Completa todas las opciones del combo.")
        }
        lugares.zip(elecciones.filterNotNull()).forEach { (grupo, eleccion) ->
            if (eleccion.opcion !in grupo.opciones) return falla("Opción no válida en ${grupo.nombre}.")
            if (Menu.producto(eleccion.opcion.productoId).conSabor && eleccion.sabor !in Menu.sabores) {
                return falla("Elige el sabor de ${Menu.nombreOpcion(eleccion.opcion)}.")
            }
        }
        partes = elecciones.filterNotNull().map { e ->
            val p = Menu.producto(e.opcion.productoId)
            Parte(p.categoria.db, p.nombre, p.variante(e.opcion.varianteId)?.nombre, e.sabor)
        }
        componentes = elecciones.filterNotNull().joinToString(", ") { e ->
            Menu.nombreOpcion(e.opcion) + (e.sabor?.let { " ($it)" } ?: "")
        }
    }

    val precio = variante?.precio ?: producto.precio ?: return falla("El producto no tiene precio.")
    return Result.success(
        Renglon(
            productoId = producto.id,
            categoriaDb = producto.categoria.db,
            productoDb = producto.nombre,
            nombre = Menu.nombreCompleto(producto),
            variante = variante?.nombre,
            sabor = if (producto.conSabor) sabor else null,
            componentes = componentes,
            partes = partes,
            cantidad = cantidad,
            precioUnitario = precio,
            notas = notas.trim(),
        )
    )
}
