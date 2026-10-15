package com.example.hamgurgueseria_monchosos.data.pedidos

import com.example.hamgurgueseria_monchosos.data.inicioDelDia
import com.example.hamgurgueseria_monchosos.data.menu.Menu
import com.example.hamgurgueseria_monchosos.data.remoto.aCentavos
import com.example.hamgurgueseria_monchosos.data.remoto.aIso
import com.example.hamgurgueseria_monchosos.data.remoto.aPesos
import com.example.hamgurgueseria_monchosos.data.remoto.desdeIso
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Pedidos en Supabase (tablas ventas, venta_detalle, venta_componentes y pagos).
 * Las pantallas leen [pedidos] y [pagosHoy]; cada cambio vuelve a consultar al servidor,
 * así los dos teléfonos ven lo mismo.
 */
class PedidosRepository(private val supabase: SupabaseClient) {
    private val _pedidos = MutableStateFlow<List<PedidoConTotales>>(emptyList())
    val pedidos: StateFlow<List<PedidoConTotales>> = _pedidos.asStateFlow()

    private val _pagosHoy = MutableStateFlow<List<Pago>>(emptyList())
    /** Pagos de hoy de pedidos no cancelados (para el resumen del día). */
    val pagosHoy: StateFlow<List<Pago>> = _pagosHoy.asStateFlow()

    /** Sube cada vez que algo cambia, para que los detalles abiertos se vuelvan a consultar. */
    private val version = MutableStateFlow(0)

    suspend fun refrescar() = coroutineScope {
        val lista = async {
            supabase.from("v_ventas").select {
                order("fecha", Order.DESCENDING)
                limit(300)
            }.decodeList<VentaFila>().map { it.aModelo() }
        }
        val pagos = async {
            supabase.from("pagos").select(Columns.raw("id, venta_id, metodo, monto, creado_en, ventas!inner(estado)")) {
                filter {
                    gte("creado_en", aIso(inicioDelDia()))
                    neq("ventas.estado", EstadoPedido.CANCELADO.db)
                }
            }.decodeList<PagoFila>().map { it.aModelo() }
        }
        _pedidos.value = lista.await()
        _pagosHoy.value = pagos.await()
    }

    /** Emite el pedido (o null si ya no existe) y se vuelve a consultar con cada cambio. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observar(id: Long): Flow<Result<PedidoCompleto?>> = version.mapLatest { runCatching { obtener(id) } }

    suspend fun obtener(id: Long): PedidoCompleto? =
        supabase.from("ventas").select(Columns.raw(COLUMNAS_COMPLETO)) {
            filter { eq("id", id) }
        }.decodeSingleOrNull<VentaCompletaFila>()?.aModelo()

    /** Crea o reemplaza el pedido completo en una sola transacción (función guardar_venta). */
    suspend fun guardar(pedido: Pedido, renglones: List<Renglon>): Long {
        val datos = VentaDto(
            id = pedido.id.takeIf { it != 0L },
            tipoEntrega = pedido.tipoEntrega.db,
            cliente = pedido.cliente, telefono = pedido.telefono,
            direccion = pedido.direccion, notas = pedido.notas,
            renglones = renglones.map { r ->
                RenglonDto(
                    categoria = r.categoriaDb, producto = r.productoDb, variante = r.variante, sabor = r.sabor,
                    cantidad = r.cantidad, precioUnitario = aPesos(r.precioUnitario), notas = r.notas,
                    componentes = r.partes.map { ParteDto(it.categoriaDb, it.productoDb, it.variante, it.sabor) },
                )
            },
        )
        val id = supabase.postgrest.rpc(
            "guardar_venta",
            buildJsonObject { put("datos", json.encodeToJsonElement(VentaDto.serializer(), datos)) },
        ).decodeAs<Long>()
        huboCambio()
        return id
    }

    suspend fun cobrar(pedidoId: Long, metodo: MetodoPago, monto: Long) {
        supabase.from("pagos").insert(PagoNuevo(pedidoId, metodo.db, aPesos(monto)))
        huboCambio()
    }

    suspend fun borrarPago(pago: Pago) {
        supabase.from("pagos").delete { filter { eq("id", pago.id) } }
        huboCambio()
    }

    suspend fun cancelar(id: Long) = cambiarEstado(id, EstadoPedido.CANCELADO)

    /** Al reabrir, la base de datos recalcula si queda pagado o por cobrar. */
    suspend fun reabrir(id: Long) = cambiarEstado(id, EstadoPedido.ABIERTO)

    suspend fun eliminar(id: Long) {
        supabase.from("ventas").delete { filter { eq("id", id) } }
        huboCambio()
    }

    private suspend fun cambiarEstado(id: Long, estado: EstadoPedido) {
        supabase.from("ventas").update({ set("estado", estado.db) }) { filter { eq("id", id) } }
        huboCambio()
    }

    /** Fuerza a que los detalles abiertos se vuelvan a consultar (reintento o sincronización). */
    fun volverAConsultar() {
        version.update { it + 1 }
    }

    private suspend fun huboCambio() {
        version.update { it + 1 }
        refrescar()
    }

    private companion object {
        val json = Json { explicitNulls = true }

        const val PRODUCTO = "productos(nombre, categorias_menu(nombre)), variantes(nombre), sabores(nombre)"
        const val COLUMNAS_COMPLETO =
            "id, fecha, tipo_entrega, cliente, telefono, direccion, notas, estado, descuento, " +
                "venta_detalle(id, cantidad, precio_unitario, notas, $PRODUCTO, " +
                "venta_componentes(id, cantidad, $PRODUCTO)), " +
                "pagos(id, venta_id, metodo, monto, creado_en)"
    }
}

// ---------------------------------------------------------------- Filas de Supabase

@Serializable
private data class Nombre(val nombre: String)

@Serializable
private data class ProductoRef(val nombre: String, @SerialName("categorias_menu") val categoria: Nombre)

@Serializable
private data class VentaFila(
    val id: Long,
    val fecha: String,
    @SerialName("tipo_entrega") val tipoEntrega: String,
    val cliente: String? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    val notas: String? = null,
    val estado: String,
    val descuento: Double = 0.0,
    val total: Double = 0.0,
    val pagado: Double = 0.0,
    val articulos: Int = 0,
) {
    fun aModelo() = PedidoConTotales(
        pedido = pedidoDesde(id, fecha, tipoEntrega, cliente, telefono, direccion, notas, estado, descuento),
        total = aCentavos(total), pagado = aCentavos(pagado), articulos = articulos,
    )
}

@Serializable
private data class ComponenteFila(
    val id: Long,
    val cantidad: Int,
    val productos: ProductoRef,
    val variantes: Nombre? = null,
    val sabores: Nombre? = null,
)

@Serializable
private data class DetalleFila(
    val id: Long,
    val cantidad: Int,
    @SerialName("precio_unitario") val precioUnitario: Double,
    val notas: String? = null,
    val productos: ProductoRef,
    val variantes: Nombre? = null,
    val sabores: Nombre? = null,
    @SerialName("venta_componentes") val componentes: List<ComponenteFila> = emptyList(),
) {
    fun aModelo(): Renglon {
        val enMenu = Menu.porNombreDb(productos.categoria.nombre, productos.nombre)
        val partes = componentes.sortedBy { it.id }.flatMap { c ->
            List(c.cantidad) { Parte(c.productos.categoria.nombre, c.productos.nombre, c.variantes?.nombre, c.sabores?.nombre) }
        }
        return Renglon(
            productoId = enMenu?.id.orEmpty(),
            categoriaDb = productos.categoria.nombre,
            productoDb = productos.nombre,
            nombre = enMenu?.let(Menu::nombreCompleto) ?: productos.nombre,
            variante = variantes?.nombre,
            sabor = sabores?.nombre,
            componentes = partes.takeIf { it.isNotEmpty() }?.joinToString(", ") { p ->
                val nombre = Menu.porNombreDb(p.categoriaDb, p.productoDb)?.let(Menu::nombreCompleto) ?: p.productoDb
                listOfNotNull(nombre, p.variante).joinToString(" ") + (p.sabor?.let { " ($it)" } ?: "")
            },
            partes = partes,
            cantidad = cantidad,
            precioUnitario = aCentavos(precioUnitario),
            notas = notas.orEmpty(),
        )
    }
}

@Serializable
private data class PagoFila(
    val id: Long,
    @SerialName("venta_id") val ventaId: Long,
    val metodo: String,
    val monto: Double,
    @SerialName("creado_en") val creadoEn: String,
) {
    fun aModelo() = Pago(
        id = id, pedidoId = ventaId,
        metodo = MetodoPago.entries.first { it.db == metodo },
        monto = aCentavos(monto), creadoEn = desdeIso(creadoEn),
    )
}

@Serializable
private data class VentaCompletaFila(
    val id: Long,
    val fecha: String,
    @SerialName("tipo_entrega") val tipoEntrega: String,
    val cliente: String? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    val notas: String? = null,
    val estado: String,
    val descuento: Double = 0.0,
    @SerialName("venta_detalle") val detalles: List<DetalleFila> = emptyList(),
    val pagos: List<PagoFila> = emptyList(),
) {
    fun aModelo() = PedidoCompleto(
        pedido = pedidoDesde(id, fecha, tipoEntrega, cliente, telefono, direccion, notas, estado, descuento),
        renglones = detalles.sortedBy { it.id }.map { it.aModelo() },
        pagos = pagos.sortedBy { it.creadoEn }.map { it.aModelo() },
    )
}

private fun pedidoDesde(
    id: Long, fecha: String, tipo: String, cliente: String?, telefono: String?,
    direccion: String?, notas: String?, estado: String, descuento: Double,
) = Pedido(
    id = id,
    creadoEn = desdeIso(fecha),
    tipoEntrega = TipoEntrega.entries.firstOrNull { it.db == tipo } ?: TipoEntrega.LOCAL,
    cliente = cliente.orEmpty(), telefono = telefono.orEmpty(),
    direccion = direccion.orEmpty(), notas = notas.orEmpty(),
    estado = EstadoPedido.entries.firstOrNull { it.db == estado } ?: EstadoPedido.ABIERTO,
    descuento = aCentavos(descuento),
)

// ---------------------------------------------------------------- Lo que se envía

@Serializable
private data class ParteDto(
    val categoria: String,
    val producto: String,
    val variante: String?,
    val sabor: String?,
    val cantidad: Int = 1,
)

@Serializable
private data class RenglonDto(
    val categoria: String,
    val producto: String,
    val variante: String?,
    val sabor: String?,
    val cantidad: Int,
    @SerialName("precio_unitario") val precioUnitario: Double,
    val notas: String,
    val componentes: List<ParteDto>,
)

@Serializable
private data class VentaDto(
    val id: Long?,
    @SerialName("tipo_entrega") val tipoEntrega: String,
    val cliente: String,
    val telefono: String,
    val direccion: String,
    val notas: String,
    val renglones: List<RenglonDto>,
)

@Serializable
private data class PagoNuevo(
    @SerialName("venta_id") val ventaId: Long,
    val metodo: String,
    val monto: Double,
)
