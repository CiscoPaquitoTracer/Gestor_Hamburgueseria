package com.example.hamgurgueseria_monchosos.ui.inventario

import com.example.hamgurgueseria_monchosos.data.Categoria
import com.example.hamgurgueseria_monchosos.data.Insumo
import com.example.hamgurgueseria_monchosos.data.Unidad

/** Lo que el usuario escribe en el formulario, todavía como texto. */
data class FormularioInsumo(
    val id: Long = 0,
    val nombre: String = "",
    val categoria: Categoria = Categoria.INGREDIENTE,
    val unidad: Unidad = Categoria.INGREDIENTE.unidadSugerida,
    val cantidad: String = "",
    val stockMinimo: String = "",
    val costoUnitario: String = "",
) {
    /** Convierte a [Insumo] o devuelve el primer error encontrado. */
    fun validar(): Result<Insumo> {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty()) return falla("Escribe el nombre del insumo.")

        val cant = numero(cantidad) ?: return falla("La cantidad no es un número válido.")
        val minimo = numero(stockMinimo, vacio = 0.0) ?: return falla("El stock mínimo no es un número válido.")
        val costo = numero(costoUnitario, vacio = 0.0) ?: return falla("El costo no es un número válido.")
        if (cant < 0 || minimo < 0 || costo < 0) return falla("No se permiten números negativos.")
        if (unidad == Unidad.PIEZA && (cant % 1 != 0.0 || minimo % 1 != 0.0)) {
            return falla("Las piezas deben ser números enteros.")
        }

        return Result.success(Insumo(id, nombreLimpio, categoria, unidad, cant, minimo, costo))
    }

    companion object {
        fun desde(insumo: Insumo) = FormularioInsumo(
            id = insumo.id,
            nombre = insumo.nombre,
            categoria = insumo.categoria,
            unidad = insumo.unidad,
            cantidad = texto(insumo.cantidad),
            stockMinimo = if (insumo.stockMinimo == 0.0) "" else texto(insumo.stockMinimo),
            costoUnitario = if (insumo.costoUnitario == 0.0) "" else texto(insumo.costoUnitario),
        )

        private fun texto(n: Double) = if (n % 1 == 0.0) n.toLong().toString() else n.toString()

        /** Acepta "1,5" y "1.5". Texto vacío → [vacio] (null = obligatorio). */
        private fun numero(texto: String, vacio: Double? = null): Double? {
            val t = texto.trim().replace(',', '.')
            if (t.isEmpty()) return vacio
            return t.toDoubleOrNull()?.takeIf { it.isFinite() }
        }

        private fun falla(mensaje: String) = Result.failure<Insumo>(IllegalArgumentException(mensaje))
    }
}
