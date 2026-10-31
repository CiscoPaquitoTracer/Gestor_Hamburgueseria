package com.example.hamgurgueseria_monchosos.data.resumen

import com.example.hamgurgueseria_monchosos.data.pedidos.MetodoPago
import com.example.hamgurgueseria_monchosos.data.remoto.aCentavos
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class ProductoTop(val nombre: String, val cantidad: Double, val total: Long)

/** [diaSemana]: 1 = lunes … 7 = domingo. [total] = todo lo vendido esos días. */
data class VentaDiaSemana(val diaSemana: Int, val dias: Int, val total: Long) {
    val promedio: Long get() = if (dias == 0) 0 else total / dias
}

/** Resumen de un mes (función resumen_mes en Supabase). Importes en centavos. */
data class Resumen(
    val ventas: Long,
    /** Parte de [ventas] que viene del historial importado (Excel), calculada con precios actuales. */
    val ventasEstimadas: Long,
    val ventasMesAnterior: Long,
    val pedidos: Int,
    val productos: Double,
    val diasAbiertos: Int,
    val descuentos: Long,
    val porCobrar: Long,
    val cobrado: Map<MetodoPago, Long>,
    val costoInsumos: Long,
    val insumosSinCosto: Int,
    val gastos: Long,
    val compras: Long,
    val topProductos: List<ProductoTop>,
    val porDiaSemana: List<VentaDiaSemana>,
) {
    /** Ventas − costo de lo vendido (según recetas) − gastos. */
    val ganancia: Long get() = ventas - costoInsumos - gastos

    /** Promedio por pedido de la app (el historial no tiene pedidos). */
    val ticketPromedio: Long? get() = if (pedidos == 0) null else (ventas - ventasEstimadas) / pedidos

    /** 0.12 = 12 % más que el mes anterior; null si el mes anterior no tiene ventas. */
    val cambioVsAnterior: Double? get() = if (ventasMesAnterior <= 0) null else ventas.toDouble() / ventasMesAnterior - 1

    val sinDatos: Boolean get() = ventas == 0L && pedidos == 0 && gastos == 0L && compras == 0L
}

class ResumenRepository(private val supabase: SupabaseClient) {
    /** [mes] = "yyyy-MM-dd" de cualquier día del mes. */
    suspend fun delMes(mes: String): Resumen =
        supabase.postgrest.rpc("resumen_mes", buildJsonObject { put("p_fecha", mes) })
            .decodeAs<ResumenFila>().aModelo()
}

// ---------------------------------------------------------------- Respuesta de Supabase (jsonb)

@Serializable
private data class ResumenFila(
    val ventas: Double,
    @SerialName("ventas_estimadas") val ventasEstimadas: Double,
    @SerialName("ventas_mes_anterior") val ventasMesAnterior: Double,
    val pedidos: Int,
    val productos: Double,
    @SerialName("dias_abiertos") val diasAbiertos: Int,
    val descuentos: Double,
    @SerialName("por_cobrar") val porCobrar: Double,
    val cobrado: Map<String, Double> = emptyMap(),
    @SerialName("costo_insumos") val costoInsumos: Double,
    @SerialName("insumos_sin_costo") val insumosSinCosto: Int,
    val gastos: Double,
    val compras: Double,
    @SerialName("top_productos") val topProductos: List<TopFila> = emptyList(),
    @SerialName("por_dia_semana") val porDiaSemana: List<DiaSemanaFila> = emptyList(),
) {
    fun aModelo() = Resumen(
        ventas = aCentavos(ventas),
        ventasEstimadas = aCentavos(ventasEstimadas),
        ventasMesAnterior = aCentavos(ventasMesAnterior),
        pedidos = pedidos,
        productos = productos,
        diasAbiertos = diasAbiertos,
        descuentos = aCentavos(descuentos),
        porCobrar = aCentavos(porCobrar),
        cobrado = MetodoPago.entries.associateWith { m -> aCentavos(cobrado[m.db] ?: 0.0) },
        costoInsumos = aCentavos(costoInsumos),
        insumosSinCosto = insumosSinCosto,
        gastos = aCentavos(gastos),
        compras = aCentavos(compras),
        topProductos = topProductos.map { ProductoTop(it.nombre, it.cantidad, aCentavos(it.total)) },
        porDiaSemana = porDiaSemana.map { VentaDiaSemana(it.diaSemana, it.dias, aCentavos(it.total)) },
    )
}

@Serializable
private data class TopFila(val nombre: String, val cantidad: Double, val total: Double)

@Serializable
private data class DiaSemanaFila(@SerialName("dia_semana") val diaSemana: Int, val dias: Int, val total: Double)
