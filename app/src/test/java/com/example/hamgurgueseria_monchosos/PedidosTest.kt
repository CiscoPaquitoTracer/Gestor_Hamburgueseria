package com.example.hamgurgueseria_monchosos

import com.example.hamgurgueseria_monchosos.data.formatearCentavos
import com.example.hamgurgueseria_monchosos.data.menu.CategoriaMenu
import com.example.hamgurgueseria_monchosos.data.menu.Eleccion
import com.example.hamgurgueseria_monchosos.data.menu.Menu
import com.example.hamgurgueseria_monchosos.data.menu.OpcionCombo
import com.example.hamgurgueseria_monchosos.data.menu.armarRenglon
import com.example.hamgurgueseria_monchosos.data.parsearPesos
import com.example.hamgurgueseria_monchosos.data.pedidos.EstadoPedido
import com.example.hamgurgueseria_monchosos.data.pedidos.MetodoPago
import com.example.hamgurgueseria_monchosos.data.pedidos.Pago
import com.example.hamgurgueseria_monchosos.data.pedidos.Parte
import com.example.hamgurgueseria_monchosos.data.pedidos.Pedido
import com.example.hamgurgueseria_monchosos.data.pedidos.PedidoConTotales
import com.example.hamgurgueseria_monchosos.data.pedidos.resumenDelDia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuTest {
    @Test
    fun menu_completo_igual_que_el_documento() {
        assertEquals(35, Menu.productos.size)
        assertEquals(Menu.productos.size, Menu.productos.map { it.id }.toSet().size)
        assertEquals(7, Menu.deCategoria(CategoriaMenu.BURGERS).size)
        assertEquals(6, Menu.deCategoria(CategoriaMenu.DOGGOS).size)
        assertEquals(4, Menu.deCategoria(CategoriaMenu.COMBOS).size)
        assertEquals(16000L, Menu.producto("combo-solitario").precio)
        assertEquals(13000L, Menu.producto("boneless").variante("boneless-10")!!.precio)
        assertTrue(Menu.deCategoria(CategoriaMenu.BURGERS).all { it.incluyePapas })
    }

    @Test
    fun todas_las_opciones_de_combo_existen() {
        Menu.productos.filter { it.esCombo }.flatMap { it.grupos }.flatMap { it.opciones }.forEach { op ->
            val p = Menu.productoONull(op.productoId)
            assertTrue("No existe ${op.productoId}", p != null)
            if (op.varianteId != null) assertTrue("Variante ${op.varianteId}", p!!.variante(op.varianteId) != null)
        }
    }

    @Test
    fun nombres_sin_ambiguedad() {
        assertEquals("Burger Junior", Menu.nombreCompleto(Menu.producto("burger-junior")))
        assertEquals("Doggo Junior", Menu.nombreCompleto(Menu.producto("doggo-junior")))
        assertEquals("Hula Hula", Menu.nombreCompleto(Menu.producto("burger-hula-hula")))
        assertEquals("Aderezo Hula Hula", Menu.nombreCompleto(Menu.producto("aderezo-2")))
        val nombres = Menu.productos.map(Menu::nombreCompleto)
        assertEquals(nombres.size, nombres.toSet().size)
    }
}

class ArmarRenglonTest {
    @Test
    fun producto_simple() {
        val r = armarRenglon(Menu.producto("burger-sweet-bacon"), cantidad = 2, notas = " sin cebolla ").getOrThrow()
        assertEquals(9000L, r.precioUnitario)
        assertEquals(18000L, r.subtotal)
        assertEquals("sin cebolla", r.notas)
    }

    @Test
    fun alitas_exigen_tamano_y_sabor() {
        val alitas = Menu.producto("alitas")
        assertTrue(armarRenglon(alitas).isFailure)
        assertTrue(armarRenglon(alitas, "alitas-10").isFailure)
        val r = armarRenglon(alitas, "alitas-10", "BBQ").getOrThrow()
        assertEquals(14000L, r.precioUnitario)
        assertEquals("10 piezas", r.variante)
        assertEquals("BBQ", r.sabor)
    }

    @Test
    fun combo_incompleto_falla_y_completo_arma_componentes() {
        val desmadre = Menu.producto("combo-desmadre")
        assertTrue(armarRenglon(desmadre, elecciones = listOf(null, null, null, null, null)).isFailure)

        val elecciones = listOf(
            Eleccion(OpcionCombo("burger-hula-hula")),
            Eleccion(OpcionCombo("burger-bonchosa")),
            Eleccion(OpcionCombo("papas-criminales")),
            Eleccion(OpcionCombo("refresco-coca")),
            Eleccion(OpcionCombo("refresco-jarritos")),
        )
        val r = armarRenglon(desmadre, elecciones = elecciones).getOrThrow()
        assertEquals(32500L, r.precioUnitario)
        assertEquals("Hula Hula, Bonchosa, Papas Criminales, Coca Cola 600 ml, Jarritos 600 ml", r.componentes)
    }

    @Test
    fun combo_rechaza_opcion_no_permitida_y_exige_sabor() {
        val desmadre = Menu.producto("combo-desmadre")
        val conGlotona = listOf(
            Eleccion(OpcionCombo("burger-glotona")), Eleccion(OpcionCombo("burger-bonchosa")),
            Eleccion(OpcionCombo("papas-criminales")), Eleccion(OpcionCombo("refresco-coca")),
            Eleccion(OpcionCombo("refresco-coca")),
        )
        assertTrue(armarRenglon(desmadre, elecciones = conGlotona).isFailure)

        val bajon = Menu.producto("combo-bajon")
        val sinSabor = listOf(
            Eleccion(OpcionCombo("boneless", "boneless-10")), Eleccion(OpcionCombo("papas-junior")),
            Eleccion(OpcionCombo("aderezo-1")), Eleccion(OpcionCombo("aderezo-3")),
        )
        assertTrue(armarRenglon(bajon, elecciones = sinSabor).isFailure)
        val conSabor = sinSabor.toMutableList().also { it[0] = it[0].copy(sabor = "Parmesano") }
        val r = armarRenglon(bajon, elecciones = conSabor).getOrThrow()
        assertEquals("Boneless 10 piezas (Parmesano), Papas Junior, Aderezo De la casa, Aderezo Cheddar", r.componentes)
    }

    @Test
    fun renglon_lleva_los_nombres_de_supabase() {
        val r = armarRenglon(
            Menu.producto("combo-pandora"),
            elecciones = listOf(
                Eleccion(OpcionCombo("burger-toxica")), Eleccion(OpcionCombo("burger-junior")),
                Eleccion(OpcionCombo("doggo-callejero")), Eleccion(OpcionCombo("doggo-pica-pica")),
                Eleccion(OpcionCombo("alitas", "alitas-5"), "BBQ"), Eleccion(OpcionCombo("boneless", "boneless-5"), "Parmesano"),
            ),
        ).getOrThrow()
        assertEquals("Combos" to "Caja de Pandora", r.categoriaDb to r.productoDb)
        assertEquals(Parte("Burgers", "Junior"), r.partes[1])
        assertEquals(Parte("Doggos Jumbo", "Pica Pica"), r.partes[3])
        assertEquals(Parte("Alitas", "Alitas", "5 piezas", "BBQ"), r.partes[4])
        // Todas las categorías de la app existen en Supabase con su nombre de BD.
        assertEquals(
            setOf("Burgers", "Doggos Jumbo", "Alitas", "Boneless", "Papas", "Combos", "Aderezos Extra", "Refrescos"),
            CategoriaMenu.entries.map { it.db }.toSet(),
        )
        assertEquals(Menu.producto("aderezo-2"), Menu.porNombreDb("Aderezos Extra", "Hula Hula"))
    }
}

class DineroYEstadoTest {
    @Test
    fun formato_y_lectura_de_pesos() {
        assertEquals("$160", formatearCentavos(16000))
        assertEquals("$1,490", formatearCentavos(149000))
        assertEquals("$150.50", formatearCentavos(15050))
        assertEquals(16000L, parsearPesos("160"))
        assertEquals(15050L, parsearPesos("150.5"))
        assertEquals(149000L, parsearPesos("$1,490"))
        assertNull(parsearPesos("abc"))
        assertNull(parsearPesos("-5"))
        assertNull(parsearPesos("1.234"))
    }

    @Test
    fun resumen_del_dia_como_cierre_de_turno() {
        val hoy = 1_000_000L
        fun p(id: Long, creado: Long, total: Long, pagado: Long, estado: EstadoPedido) =
            PedidoConTotales(Pedido(id = id, creadoEn = creado, estado = estado), total, pagado, 1)
        val pedidos = listOf(
            p(1, hoy + 1, 23000, 23000, EstadoPedido.PAGADO),
            p(2, hoy + 2, 21000, 10000, EstadoPedido.ABIERTO),
            p(3, hoy + 3, 12000, 0, EstadoPedido.CANCELADO),
            p(4, hoy - 5, 5000, 0, EstadoPedido.ABIERTO), // de ayer, sigue por cobrar
        )
        val pagos = listOf(
            Pago(pedidoId = 1, metodo = MetodoPago.EFECTIVO, monto = 23000, creadoEn = hoy + 1),
            Pago(pedidoId = 2, metodo = MetodoPago.TRANSFERENCIA, monto = 10000, creadoEn = hoy + 2),
        )
        val r = resumenDelDia(pedidos, pagos, hoy)
        assertEquals(2, r.pedidos)
        assertEquals(44000L, r.ventas)
        assertEquals(2, r.porCobrar)
        assertEquals(11000L + 5000L, r.pendiente)
        assertEquals(23000L, r.cobradoPorMetodo[MetodoPago.EFECTIVO])
        assertEquals(33000L, r.cobrado)
    }
}
