package com.example.hamgurgueseria_monchosos.data.pedidos

/** [db] = valor en Supabase (ventas.tipo_entrega). */
enum class TipoEntrega(val etiqueta: String, val emoji: String, val db: String) {
    LOCAL("Comer aquí", "🍽️", "local"),
    PARA_LLEVAR("Para llevar", "🥡", "para_llevar"),
    DOMICILIO("Domicilio", "🛵", "domicilio"),
}

/** [db] = valor en Supabase (ventas.estado). El estado lo calcula la base de datos. */
enum class EstadoPedido(val etiqueta: String, val db: String) {
    ABIERTO("Por cobrar", "abierta"),
    PAGADO("Pagado", "pagada"),
    CANCELADO("Cancelado", "cancelada"),
}

/** [db] = valor en Supabase (dominio metodo_pago). */
enum class MetodoPago(val etiqueta: String, val db: String) {
    EFECTIVO("Efectivo", "efectivo"),
    TRANSFERENCIA("Transferencia", "transferencia"),
    TARJETA("Tarjeta / Mercado Pago", "tarjeta"),
}

/** Una comanda. Los importes van en centavos. */
data class Pedido(
    val id: Long = 0,
    val creadoEn: Long = System.currentTimeMillis(),
    val tipoEntrega: TipoEntrega = TipoEntrega.LOCAL,
    val cliente: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val notas: String = "",
    val estado: EstadoPedido = EstadoPedido.ABIERTO,
    val descuento: Long = 0,
)

/** Algo elegido dentro de un combo, con los nombres que usa la base de datos. */
data class Parte(
    val categoriaDb: String,
    val productoDb: String,
    val variante: String? = null,
    val sabor: String? = null,
)

/**
 * Un renglón del pedido. Guarda nombre y precio CONGELADOS al momento de la venta:
 * si el menú cambia después, el pedido no se altera.
 */
data class Renglon(
    val productoId: String,
    /** Nombre de la categoría y del producto en Supabase (para guardar). */
    val categoriaDb: String,
    val productoDb: String,
    /** Nombre para mostrar ("Burger Junior"). */
    val nombre: String,
    val variante: String? = null,
    val sabor: String? = null,
    /** Lo elegido dentro de un combo, en texto: "Hula Hula, Bonchosa, Papas Criminales…". */
    val componentes: String? = null,
    val partes: List<Parte> = emptyList(),
    val cantidad: Int,
    val precioUnitario: Long,
    val notas: String = "",
) {
    val subtotal: Long get() = cantidad * precioUnitario

    /** Mismo producto con las mismas elecciones → se suma la cantidad en vez de duplicar el renglón. */
    fun mismoArticuloQue(otro: Renglon): Boolean =
        categoriaDb == otro.categoriaDb && productoDb == otro.productoDb && variante == otro.variante &&
            sabor == otro.sabor && partes == otro.partes && notas == otro.notas &&
            precioUnitario == otro.precioUnitario
}

data class Pago(
    val id: Long = 0,
    val pedidoId: Long,
    val metodo: MetodoPago,
    val monto: Long,
    val creadoEn: Long = System.currentTimeMillis(),
)

/** Para la lista: el pedido con sus totales (calculados por la base de datos). */
data class PedidoConTotales(
    val pedido: Pedido,
    val total: Long,
    val pagado: Long,
    val articulos: Int,
) {
    val pendiente: Long get() = (total - pagado).coerceAtLeast(0)
}

/** Para el detalle: el pedido con renglones y pagos. */
data class PedidoCompleto(
    val pedido: Pedido,
    val renglones: List<Renglon>,
    val pagos: List<Pago>,
) {
    val subtotal: Long get() = renglones.sumOf { it.subtotal }
    val total: Long get() = (subtotal - pedido.descuento).coerceAtLeast(0)
    val pagado: Long get() = pagos.sumOf { it.monto }
    val pendiente: Long get() = (total - pagado).coerceAtLeast(0)
}

/** Resumen tipo "cierre de turno" de las comandas. */
data class ResumenDia(
    val pedidos: Int = 0,
    val ventas: Long = 0,
    val porCobrar: Int = 0,
    val pendiente: Long = 0,
    val cobradoPorMetodo: Map<MetodoPago, Long> = emptyMap(),
) {
    val cobrado: Long get() = cobradoPorMetodo.values.sum()
}

/**
 * [pagos] deben ser solo de pedidos no cancelados. [desde] = inicio del día en milisegundos.
 * Los pedidos por cobrar se cuentan aunque sean de días anteriores (siguen pendientes).
 */
fun resumenDelDia(pedidos: List<PedidoConTotales>, pagos: List<Pago>, desde: Long): ResumenDia {
    val vigentes = pedidos.filter { it.pedido.estado != EstadoPedido.CANCELADO }
    val deHoy = vigentes.filter { it.pedido.creadoEn >= desde }
    val abiertos = vigentes.filter { it.pedido.estado == EstadoPedido.ABIERTO }
    return ResumenDia(
        pedidos = deHoy.size,
        ventas = deHoy.sumOf { it.total },
        porCobrar = abiertos.size,
        pendiente = abiertos.sumOf { it.pendiente },
        cobradoPorMetodo = pagos.filter { it.creadoEn >= desde }
            .groupBy { it.metodo }.mapValues { (_, l) -> l.sumOf { it.monto } },
    )
}
