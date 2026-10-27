package com.example.hamgurgueseria_monchosos.data.pronostico

import com.example.hamgurgueseria_monchosos.data.Unidad
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Pronóstico de la semana y, si se acerca un fin de semana o festivo, qué comprar para cubrirlo. */
data class Pronostico(
    val dias: List<DiaPronostico>,
    val evento: Evento?,
    /** Para cubrir hasta el final del evento (o hasta mañana si no hay evento). */
    val porComprar: List<InsumoPorComprar>,
)

/**
 * Las estimaciones las calcula la base de datos (supabase/migrations/…_pronostico.sql) con el
 * historial de ventas: festivos con festivos anteriores, y cada día de la semana con los mismos días.
 */
class PronosticoRepository(private val supabase: SupabaseClient) {

    suspend fun consultar(dias: Int = 7): Pronostico {
        val proximos = supabase.postgrest.rpc("pronostico_dias", buildJsonObject { put("p_dias", dias) })
            .decodeList<DiaFila>().map { it.aModelo() }
        val evento = proximoEvento(proximos)
        val hasta = evento?.hasta ?: proximos.getOrNull(1)?.fecha ?: proximos.firstOrNull()?.fecha
        val porComprar = if (hasta == null) emptyList() else {
            supabase.postgrest.rpc("pronostico_insumos", buildJsonObject { put("p_hasta", hasta) })
                .decodeList<InsumoFila>().map { it.aModelo() }.filter { it.porComprar > 0 }
        }
        return Pronostico(proximos, evento, porComprar)
    }
}

// ---------------------------------------------------------------- Filas de Supabase

@Serializable
private data class DiaFila(
    val fecha: String,
    @SerialName("dia_semana") val diaSemana: Int,
    val festivo: String? = null,
    @SerialName("pedidos_esperados") val pedidosEsperados: Double? = null,
    @SerialName("pedidos_promedio") val pedidosPromedio: Double? = null,
    @SerialName("dias_historial") val diasHistorial: Int = 0,
) {
    fun aModelo() = DiaPronostico(fecha, diaSemana, festivo, pedidosEsperados, pedidosPromedio, diasHistorial)
}

@Serializable
private data class InsumoFila(
    val nombre: String,
    val unidad: String,
    val existencia: Double,
    @SerialName("consumo_esperado") val consumoEsperado: Double,
    @SerialName("por_comprar") val porComprar: Double,
) {
    fun aModelo() = InsumoPorComprar(
        nombre = nombre,
        unidad = Unidad.entries.firstOrNull { it.db == unidad } ?: Unidad.PIEZA,
        existencia = existencia,
        consumoEsperado = consumoEsperado,
        porComprar = porComprar,
    )
}
