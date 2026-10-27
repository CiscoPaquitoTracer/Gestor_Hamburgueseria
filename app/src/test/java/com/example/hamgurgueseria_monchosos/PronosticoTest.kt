package com.example.hamgurgueseria_monchosos

import com.example.hamgurgueseria_monchosos.data.Unidad
import com.example.hamgurgueseria_monchosos.data.pronostico.DiaPronostico
import com.example.hamgurgueseria_monchosos.data.pronostico.InsumoPorComprar
import com.example.hamgurgueseria_monchosos.data.pronostico.mensajeDeAviso
import com.example.hamgurgueseria_monchosos.data.pronostico.nombreDelDia
import com.example.hamgurgueseria_monchosos.data.pronostico.proximoEvento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PronosticoTest {
    /** Semana de 7 días empezando en [diaSemanaHoy]; [festivos] = índice → nombre. */
    private fun semana(
        diaSemanaHoy: Int,
        festivos: Map<Int, String> = emptyMap(),
        productos: (DiaPronostico) -> Double? = { if (it.especial) 30.0 else 20.0 },
    ): List<DiaPronostico> = (0 until 7).map { i ->
        val base = DiaPronostico("2026-11-%02d".format(i + 1), (diaSemanaHoy - 1 + i) % 7 + 1, festivos[i])
        base.copy(productosEsperados = productos(base), productosPromedio = 22.0, diasHistorial = 4)
    }

    private val pan = InsumoPorComprar("Pan para hamburguesa", Unidad.PIEZA, 10.0, 32.3, 22.3)

    @Test
    fun jueves_avisa_del_fin_de_semana() {
        val e = proximoEvento(semana(diaSemanaHoy = 4))!!
        assertEquals(listOf(6, 7), e.dias.map { it.diaSemana })
        assertEquals("el fin de semana", e.nombre)
        assertTrue(e.fuerte)
    }

    @Test
    fun lunes_sin_festivo_no_hay_evento() {
        assertNull(proximoEvento(semana(diaSemanaHoy = 1)))
    }

    @Test
    fun festivo_en_lunes_hace_puente_con_el_fin_de_semana() {
        // Hoy viernes: sábado, domingo y lunes festivo.
        val e = proximoEvento(semana(diaSemanaHoy = 5, festivos = mapOf(3 to "Revolución Mexicana")))!!
        assertEquals(3, e.dias.size)
        assertEquals("Revolución Mexicana y el fin de semana", e.nombre)
        assertEquals("2026-11-04", e.hasta)
    }

    @Test
    fun festivo_entre_semana() {
        val e = proximoEvento(semana(diaSemanaHoy = 1, festivos = mapOf(2 to "Día de Muertos")))!!
        assertEquals("Día de Muertos", e.nombre)
        assertEquals(1, e.dias.size)
    }

    @Test
    fun mensaje_con_faltantes_y_aumento() {
        val e = proximoEvento(semana(diaSemanaHoy = 4))!!
        val (titulo, cuerpo) = mensajeDeAviso(e, listOf(pan))!!
        assertEquals("Se viene el fin de semana", titulo)
        assertTrue(cuerpo, "36 % más" in cuerpo)          // 60 esperados / 44 normales
        assertTrue(cuerpo, "~60 productos" in cuerpo)
        assertTrue(cuerpo, "Pan para hamburguesa: 23 pz" in cuerpo)   // 22.3 piezas → 23
    }

    @Test
    fun sin_faltantes_y_dia_normal_no_molesta() {
        val tranquilo = semana(diaSemanaHoy = 4) { 20.0 }
        val e = proximoEvento(tranquilo)!!
        assertNull(mensajeDeAviso(e, emptyList()))
        // Pero si faltan insumos, sí avisa aunque se venda lo normal.
        assertTrue(mensajeDeAviso(e, listOf(pan))!!.second.contains("Te falta"))
    }

    @Test
    fun sin_historial_no_inventa_numeros() {
        val vacio = semana(diaSemanaHoy = 4) { null }.map { it.copy(productosPromedio = null, diasHistorial = 0) }
        val e = proximoEvento(vacio)!!
        assertNull(e.aumento)
        val (_, cuerpo) = mensajeDeAviso(e, listOf(pan))!!
        assertTrue(cuerpo, "productos" !in cuerpo)
    }

    @Test
    fun nombre_del_dia_en_espanol() {
        assertEquals("sábado 31", nombreDelDia("2026-10-31"))
        assertEquals("no-es-fecha", nombreDelDia("no-es-fecha"))
    }
}
