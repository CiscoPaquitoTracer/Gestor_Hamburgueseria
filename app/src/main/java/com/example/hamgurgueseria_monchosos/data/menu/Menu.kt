package com.example.hamgurgueseria_monchosos.data.menu

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Menú de Monchosos (fuente: men_monchosos.md). Aquí vive la ESTRUCTURA (combos, descripciones,
 * categorías); los PRECIOS y la disponibilidad mandan desde Supabase (ver [Menu.actualizar] y
 * MenuRepository). Los precios de este archivo solo se usan mientras carga o si no hay internet.
 * Los precios van en CENTAVOS para no tener errores de redondeo.
 */
/** [db] = nombre de la categoría en Supabase (tabla categorias_menu). */
enum class CategoriaMenu(val etiqueta: String, val emoji: String, val singular: String, val db: String) {
    BURGERS("Burgers", "🍔", "Burger", "Burgers"),
    DOGGOS("Doggos", "🌭", "Doggo", "Doggos Jumbo"),
    ALITAS("Alitas", "🍗", "Alitas", "Alitas"),
    BONELESS("Boneless", "🍗", "Boneless", "Boneless"),
    PAPAS("Papas", "🍟", "Papas", "Papas"),
    COMBOS("Combos", "📦", "Combo", "Combos"),
    ADEREZOS("Aderezos", "🥫", "Aderezo", "Aderezos Extra"),
    REFRESCOS("Refrescos", "🥤", "Refresco", "Refrescos"),
}

/** [dbId] = id en Supabase (0 mientras no se ha cargado). */
/** Precio y disponibilidad de un producto según Supabase (precios en centavos). */
data class PrecioDb(
    val id: Long,
    val categoriaDb: String,
    val nombre: String,
    val precio: Long?,
    val activo: Boolean,
    val variantes: List<VarianteDb> = emptyList(),
)

data class VarianteDb(val id: Long, val nombre: String, val precio: Long, val activo: Boolean)

data class Variante(
    val id: String,
    val nombre: String,
    val precio: Long,
    val activo: Boolean = true,
    val dbId: Long = 0,
)

/** Una opción permitida dentro de un grupo de combo (ej. "Boneless 5 piezas"). */
data class OpcionCombo(val productoId: String, val varianteId: String? = null)

/** "2 hamburguesas a elegir entre…" */
data class GrupoCombo(val nombre: String, val cantidad: Int, val opciones: List<OpcionCombo>)

data class ProductoMenu(
    val id: String,
    val categoria: CategoriaMenu,
    val nombre: String,
    /** null cuando el precio depende de la variante. */
    val precio: Long? = null,
    val descripcion: String = "",
    val variantes: List<Variante> = emptyList(),
    val conSabor: Boolean = false,
    val incluyePapas: Boolean = false,
    val grupos: List<GrupoCombo> = emptyList(),
    /** false = "no disponible hoy": no aparece al tomar pedidos. */
    val activo: Boolean = true,
    val dbId: Long = 0,
) {
    val esCombo: Boolean get() = grupos.isNotEmpty()
    val necesitaConfigurar: Boolean get() = variantes.isNotEmpty() || conSabor || esCombo
    val variantesActivas: List<Variante> get() = variantes.filter { it.activo }
    val precioDesde: Long get() = precio ?: (variantesActivas.ifEmpty { variantes }).minOf { it.precio }
    fun variante(id: String?): Variante? = variantes.firstOrNull { it.id == id }
}

object Menu {
    const val NOTA = "Todas las hamburguesas y hot dogs incluyen una porción de papas."

    val sabores = listOf("Mango-Habanero", "Venenosas", "BBQ", "Parmesano", "Lemon Pepper Crispy")

    private fun p(pesos: Int): Long = pesos * 100L

    private val burgers = listOf(
        ProductoMenu("burger-junior", CategoriaMenu.BURGERS, "Junior", p(75), "Carne de la casa, costra de queso, queso americano, jamón y salsa comeback.", incluyePapas = true),
        ProductoMenu("burger-glotona", CategoriaMenu.BURGERS, "Glotona", p(120), "Carne de la casa rellena de queso, boneless, tocino, costra de queso, piña y aderezo de la casa.", incluyePapas = true),
        ProductoMenu("burger-guacamole", CategoriaMenu.BURGERS, "La Guacamole", p(100), "Carne de la casa, costra de queso, chiles jalapeños, tocino, pico de gallo, guacamole y aderezo venenoso.", incluyePapas = true),
        ProductoMenu("burger-sweet-bacon", CategoriaMenu.BURGERS, "Sweet Bacon", p(90), "Doble carne, tocino, queso americano, costra de queso y aderezo de la casa.", incluyePapas = true),
        ProductoMenu("burger-toxica", CategoriaMenu.BURGERS, "La Tóxica", p(140), "Carne rellena de queso, salchicha parrillera, costra de queso, chile morrón, piña, cebolla caramelizada y aderezo a elección.", incluyePapas = true),
        ProductoMenu("burger-hula-hula", CategoriaMenu.BURGERS, "Hula Hula", p(95), "Carne de la casa, queso manchego, jamón, piña-canela, cebolla caramelizada y aderezo hula hula.", incluyePapas = true),
        ProductoMenu("burger-bonchosa", CategoriaMenu.BURGERS, "Bonchosa", p(90), "Boneless bañados en BBQ, tocino y aderezo cheddar.", incluyePapas = true),
    )

    private val doggos = listOf(
        ProductoMenu("doggo-callejero", CategoriaMenu.DOGGOS, "El Perro Callejero", p(50), "Salchicha jumbo para asar envuelta en tocino, cebolla caramelizada y chiles en vinagre picados.", incluyePapas = true),
        ProductoMenu("doggo-atascado", CategoriaMenu.DOGGOS, "Atascado", p(70), "Salchicha jumbo envuelta en tocino rellena de queso, cubierta con lluvia de papas fritas trituradas y queso cheddar.", incluyePapas = true),
        ProductoMenu("doggo-junior", CategoriaMenu.DOGGOS, "Junior", p(35), "El clásico con jitomate, cebolla y aderezos (ideal para niños).", incluyePapas = true),
        ProductoMenu("doggo-pica-pica", CategoriaMenu.DOGGOS, "Pica Pica", p(60), "Salchicha jumbo, costra de queso, jalapeño, cebolla morada curtida con limón y aderezo venenoso.", incluyePapas = true),
        ProductoMenu("doggo-mexicanisimo", CategoriaMenu.DOGGOS, "Mexicanísimo", p(80), "Salchicha jumbo cubierta con costra de queso manchego, pico de gallo, guacamole y un toque de aderezo In & Out.", incluyePapas = true),
        ProductoMenu("doggo-toxico-guac", CategoriaMenu.DOGGOS, "Tóxico Guac", p(70), "Salchicha jumbo rellena de queso, guacamole, pico de gallo y cebolla caramelizada.", incluyePapas = true),
    )

    private val alitas = ProductoMenu(
        "alitas", CategoriaMenu.ALITAS, "Alitas",
        variantes = listOf(Variante("alitas-5", "5 piezas", p(75)), Variante("alitas-10", "10 piezas", p(140))),
        conSabor = true,
    )

    private val boneless = ProductoMenu(
        "boneless", CategoriaMenu.BONELESS, "Boneless",
        variantes = listOf(Variante("boneless-5", "5 piezas", p(70)), Variante("boneless-10", "10 piezas", p(130))),
        conSabor = true,
    )

    private val papas = listOf(
        ProductoMenu("papas-normalitas", CategoriaMenu.PAPAS, "Normalitas", p(60), "Orden clásica."),
        ProductoMenu("papas-monchosas", CategoriaMenu.PAPAS, "Monchosas", p(110), "Cama de papas a la francesa sazonadas, trozos de boneless, mucho queso cheddar derretido, tocino picado y aderezo ranch."),
        ProductoMenu("papas-junior", CategoriaMenu.PAPAS, "Junior", p(80), "Papas a la francesa con tocino y trozos de salchicha frita, acompañadas de aderezo cheddar."),
        ProductoMenu("papas-criminales", CategoriaMenu.PAPAS, "Criminales", p(100), "Papas fritas, trozos de boneless bañados en salsa búfalo, jalapeños en rodajas, aderezo ranch y un toque de Flamin' Hot."),
        ProductoMenu("papas-bbq-bacon", CategoriaMenu.PAPAS, "BBQ Bacon", p(85), "Papas fritas, trozos de salchicha jumbo, salsa BBQ ahumada, queso fundido y una capa extra de cebolla caramelizada."),
    )

    private val aderezos = listOf("De la casa", "Hula Hula", "Cheddar", "Venenoso", "In & Out", "Comeback")
        .mapIndexed { i, nombre -> ProductoMenu("aderezo-${i + 1}", CategoriaMenu.ADEREZOS, nombre, p(15)) }

    private val refrescos = listOf(
        ProductoMenu("refresco-boing", CategoriaMenu.REFRESCOS, "Boing 500 ml", p(20)),
        ProductoMenu("refresco-coca", CategoriaMenu.REFRESCOS, "Coca Cola 600 ml", p(25)),
        ProductoMenu("refresco-sprite", CategoriaMenu.REFRESCOS, "Sprite 600 ml", p(23)),
        ProductoMenu("refresco-jarritos", CategoriaMenu.REFRESCOS, "Jarritos 600 ml", p(23)),
        ProductoMenu("refresco-fresca", CategoriaMenu.REFRESCOS, "Fresca 600 ml", p(23)),
    )

    private fun opciones(vararg ids: String) = ids.map { OpcionCombo(it) }
    private val cualquierRefresco = refrescos.map { OpcionCombo(it.id) }
    private val cualquierAderezo = aderezos.map { OpcionCombo(it.id) }

    private val combos = listOf(
        ProductoMenu(
            "combo-solitario", CategoriaMenu.COMBOS, "Solitario", p(160),
            "1 hamburguesa Sweet Bacon + 1 porción papas normalitas + 1 refresco.",
            grupos = listOf(
                GrupoCombo("Hamburguesa", 1, opciones("burger-sweet-bacon")),
                GrupoCombo("Papas", 1, opciones("papas-normalitas")),
                GrupoCombo("Refresco", 1, cualquierRefresco),
            ),
        ),
        ProductoMenu(
            "combo-desmadre", CategoriaMenu.COMBOS, "Desmadre de Pareja", p(325),
            "2 hamburguesas (Hula Hula, Sweet Bacon o Bonchosa) + 1 orden papas Criminales + 2 refrescos.",
            grupos = listOf(
                GrupoCombo("Hamburguesa", 2, opciones("burger-hula-hula", "burger-sweet-bacon", "burger-bonchosa")),
                GrupoCombo("Papas", 1, opciones("papas-criminales")),
                GrupoCombo("Refresco", 2, cualquierRefresco),
            ),
        ),
        ProductoMenu(
            "combo-pandora", CategoriaMenu.COMBOS, "Caja de Pandora", p(500),
            "2 hamburguesas (Tóxica, Junior o La Guacamole) + 2 doggos (El Perro Callejero, Pica Pica o Tóxico Guac) + 5 alitas + 5 boneless (cualquier sabor).",
            grupos = listOf(
                GrupoCombo("Hamburguesa", 2, opciones("burger-toxica", "burger-junior", "burger-guacamole")),
                GrupoCombo("Doggo", 2, opciones("doggo-callejero", "doggo-pica-pica", "doggo-toxico-guac")),
                GrupoCombo("Alitas", 1, listOf(OpcionCombo("alitas", "alitas-5"))),
                GrupoCombo("Boneless", 1, listOf(OpcionCombo("boneless", "boneless-5"))),
            ),
        ),
        ProductoMenu(
            "combo-bajon", CategoriaMenu.COMBOS, "Kit Para El Bajón", p(230),
            "10 boneless a elegir + papas junior + 2 aderezos a elegir.",
            grupos = listOf(
                GrupoCombo("Boneless", 1, listOf(OpcionCombo("boneless", "boneless-10"))),
                GrupoCombo("Papas", 1, opciones("papas-junior")),
                GrupoCombo("Aderezo", 2, cualquierAderezo),
            ),
        ),
    )

    /** Menú tal como está escrito arriba (precios de respaldo). */
    private val base: List<ProductoMenu> =
        burgers + doggos + alitas + boneless + papas + combos + aderezos + refrescos

    private val _catalogo = MutableStateFlow(base)

    /** Menú vigente, con precios y disponibilidad de Supabase. Las pantallas lo observan. */
    val catalogo: StateFlow<List<ProductoMenu>> = _catalogo.asStateFlow()

    val productos: List<ProductoMenu> get() = _catalogo.value

    private val nombresRepetidos = base.filter { it.categoria != CategoriaMenu.ADEREZOS }
        .groupingBy { it.nombre }.eachCount().filterValues { it > 1 }.keys

    /**
     * Aplica lo que hay en Supabase sobre el menú base. Se empata por categoría + nombre
     * (y la variante por nombre); lo que no esté en [filas] se queda con su precio de respaldo.
     */
    fun actualizar(filas: List<PrecioDb>) {
        val porNombre = filas.associateBy { it.categoriaDb to it.nombre }
        _catalogo.value = base.map { p ->
            val fila = porNombre[p.categoria.db to p.nombre] ?: return@map p
            p.copy(
                precio = if (p.precio != null) fila.precio ?: p.precio else null,
                activo = fila.activo,
                dbId = fila.id,
                variantes = p.variantes.map { v ->
                    fila.variantes.firstOrNull { it.nombre == v.nombre }
                        ?.let { v.copy(precio = it.precio, activo = it.activo, dbId = it.id) } ?: v
                },
            )
        }
    }

    fun producto(id: String): ProductoMenu = productoONull(id) ?: throw NoSuchElementException(id)

    fun productoONull(id: String): ProductoMenu? = productos.firstOrNull { it.id == id }

    /** Busca por los nombres con que el producto está en Supabase. */
    fun porNombreDb(categoriaDb: String, nombre: String): ProductoMenu? =
        productos.firstOrNull { it.categoria.db == categoriaDb && it.nombre == nombre }

    fun deCategoria(categoria: CategoriaMenu): List<ProductoMenu> = productos.filter { it.categoria == categoria }

    /** "Junior" existe en Burgers, Doggos y Papas: en esos casos se antepone la categoría. */
    fun nombreCompleto(producto: ProductoMenu): String = when {
        producto.categoria == CategoriaMenu.ADEREZOS -> "Aderezo ${producto.nombre}"
        producto.categoria == CategoriaMenu.PAPAS -> "Papas ${producto.nombre}"
        producto.nombre in nombresRepetidos -> "${producto.categoria.singular} ${producto.nombre}"
        else -> producto.nombre
    }

    fun nombreOpcion(opcion: OpcionCombo): String {
        val producto = producto(opcion.productoId)
        val variante = producto.variante(opcion.varianteId)
        return listOfNotNull(nombreCompleto(producto), variante?.nombre).joinToString(" ")
    }
}
