package com.example.hamgurgueseria_monchosos

import com.example.hamgurgueseria_monchosos.data.resumen.Resumen
import com.example.hamgurgueseria_monchosos.data.resumen.VentaDiaSemana
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumenTest {
    private fun resumen(ventas: Long = 405_00, estimadas: Long = 150_00, anterior: Long = 300_00, pedidos: Int = 2) = Resumen(
        ventas = ventas, ventasEstimadas = estimadas, ventasMesAnterior = anterior, pedidos = pedidos,
        productos = 5.0, diasAbiertos = 3, descuentos = 20_00, porCobrar = 95_00, cobrado = emptyMap(),
        costoInsumos = 21_00, insumosSinCosto = 0, gastos = 200_00, compras = 0,
        topProductos = emptyList(), porDiaSemana = emptyList(),
    )

    @Test
    fun ganancia_y_promedios() {
        val r = resumen()
        assertEquals(184_00L, r.ganancia)                 // 405 − 21 − 200
        assertEquals(127_50L, r.ticketPromedio)           // (405 − 150 del Excel) / 2 pedidos
        assertEquals(0.35, r.cambioVsAnterior!!, 0.0001)  // 405 / 300 − 1
    }

    @Test
    fun sin_mes_anterior_ni_pedidos() {
        val r = resumen(anterior = 0, pedidos = 0)
        assertNull(r.cambioVsAnterior)
        assertNull(r.ticketPromedio)
    }

    @Test
    fun mes_vacio() {
        assertTrue(resumen(ventas = 0, estimadas = 0, pedidos = 0).copy(gastos = 0).sinDatos)
    }

    @Test
    fun promedio_por_dia() {
        assertEquals(155_00L, VentaDiaSemana(5, 2, 310_00).promedio)
        assertEquals(0L, VentaDiaSemana(1, 0, 0).promedio)
    }
}
