package com.example.hamgurgueseria_monchosos

import com.example.hamgurgueseria_monchosos.data.Categoria
import com.example.hamgurgueseria_monchosos.data.Insumo
import com.example.hamgurgueseria_monchosos.data.Unidad
import com.example.hamgurgueseria_monchosos.data.formatearCantidad
import com.example.hamgurgueseria_monchosos.ui.inventario.FormularioInsumo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventarioTest {
    @Test
    fun formulario_valido_por_kilo_acepta_coma_decimal() {
        val insumo = FormularioInsumo(nombre = " Jitomate ", cantidad = "2,5", stockMinimo = "1").validar().getOrThrow()
        assertEquals("Jitomate", insumo.nombre)
        assertEquals(Unidad.KILOGRAMO, insumo.unidad)
        assertEquals(2.5, insumo.cantidad, 0.0)
    }

    @Test
    fun formulario_rechaza_nombre_vacio_y_numeros_invalidos() {
        assertTrue(FormularioInsumo(nombre = "", cantidad = "1").validar().isFailure)
        assertTrue(FormularioInsumo(nombre = "Pan", cantidad = "abc").validar().isFailure)
        assertTrue(FormularioInsumo(nombre = "Pan", cantidad = "-3").validar().isFailure)
    }

    @Test
    fun piezas_deben_ser_enteras() {
        val form = FormularioInsumo(nombre = "Caja", categoria = Categoria.DESECHABLE, unidad = Unidad.PIEZA)
        assertTrue(form.copy(cantidad = "1.5").validar().isFailure)
        assertTrue(form.copy(cantidad = "100").validar().isSuccess)
    }

    @Test
    fun bajo_stock_solo_si_hay_minimo() {
        val pan = Insumo(nombre = "Pan", categoria = Categoria.PANES, unidad = Unidad.PIEZA, cantidad = 5.0)
        assertFalse(pan.bajoStock)
        assertTrue(pan.copy(stockMinimo = 10.0).bajoStock)
    }

    @Test
    fun formato_de_cantidades() {
        assertEquals("40 pz", formatearCantidad(40.0, Unidad.PIEZA))
        assertEquals("2.5 kg", formatearCantidad(2.5, Unidad.KILOGRAMO))
        assertEquals("3 kg", formatearCantidad(3.0, Unidad.KILOGRAMO))
    }
}
