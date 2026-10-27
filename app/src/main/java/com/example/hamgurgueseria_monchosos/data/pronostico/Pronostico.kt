package com.example.hamgurgueseria_monchosos.data.pronostico

import com.example.hamgurgueseria_monchosos.data.Unidad
import com.example.hamgurgueseria_monchosos.data.formatearCantidad
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil

/** Un día del pronóstico (pronostico_dias en Supabase). [fecha] = "yyyy-MM-dd". */
data class DiaPronostico(
    val fecha: String,
    /** 1 = lunes … 7 = domingo. */
    val diaSemana: Int,
    val festivo: String? = null,
    /** null = sin historial para estimar. */
    val productosEsperados: Double? = null,
    /** Promedio de un día cualquiera, para comparar. */
    val productosPromedio: Double? = null,
    val diasHistorial: Int = 0,
) {
    val finDeSemana: Boolean get() = diaSemana >= 6
    val especial: Boolean get() = festivo != null || finDeSemana

    /** Se espera vender claramente más que un día normal (15 % o más). */
    val fuerte: Boolean
        get() = productosEsperados != null && productosPromedio != null && productosPromedio > 0 &&
            productosEsperados >= productosPromedio * UMBRAL_FUERTE

    companion object {
        const val UMBRAL_FUERTE = 1.15
    }
}

/** Lo que conviene comprar para cubrir los próximos días (pronostico_insumos). */
data class InsumoPorComprar(
    val nombre: String,
    val unidad: Unidad,
    val existencia: Double,
    val consumoEsperado: Double,
    val porComprar: Double,
) {
    /** Las piezas se redondean hacia arriba (2.3 panes → comprar 3). */
    val texto: String
        get() = "$nombre: ${formatearCantidad(if (unidad == Unidad.PIEZA) ceil(porComprar) else porComprar, unidad)}"
}

/** Fin de semana y/o festivo(s) que se acercan: días consecutivos "especiales". */
data class Evento(val dias: List<DiaPronostico>) {
    val hasta: String get() = dias.last().fecha
    val fuerte: Boolean get() = dias.any { it.fuerte }
    val conHistorial: Boolean get() = dias.any { it.diasHistorial > 0 }

    /** "el fin de semana", "Día de Muertos", "Día de la Independencia y el fin de semana"… */
    val nombre: String
        get() {
            val festivos = dias.mapNotNull { it.festivo }.distinct()
            val conFinde = dias.any { it.finDeSemana && it.festivo == null }
            return when {
                festivos.isEmpty() -> "el fin de semana"
                conFinde -> festivos.joinToString(", ") + " y el fin de semana"
                else -> festivos.joinToString(", ")
            }
        }

    val productosEsperados: Double get() = dias.sumOf { it.productosEsperados ?: 0.0 }

    /** Cuánto más que días normales (0.25 = 25 % más), si hay datos. */
    val aumento: Double?
        get() {
            val conDatos = dias.filter { it.productosEsperados != null && (it.productosPromedio ?: 0.0) > 0 }
            if (conDatos.isEmpty()) return null
            return conDatos.sumOf { it.productosEsperados!! } / conDatos.sumOf { it.productosPromedio!! } - 1
        }
}

/**
 * Busca un fin de semana o festivo que empiece mañana o pasado mañana, y lo extiende a los
 * días especiales seguidos (ej. festivo en lunes + sábado y domingo = un solo "puente").
 * [dias] empieza hoy. Hoy no cuenta: el aviso es para comprar con tiempo.
 */
fun proximoEvento(dias: List<DiaPronostico>): Evento? {
    val inicio = (1..2).firstOrNull { it < dias.size && dias[it].especial } ?: return null
    var fin = inicio
    while (fin + 1 < dias.size && dias[fin + 1].especial) fin++
    return Evento(dias.subList(inicio, fin + 1))
}

/** Texto de la notificación, o null si no hay nada que valga la pena avisar. */
fun mensajeDeAviso(evento: Evento, porComprar: List<InsumoPorComprar>): Pair<String, String>? {
    if (porComprar.isEmpty() && !evento.fuerte) return null
    val titulo = "Se viene ${evento.nombre}"
    val cuerpo = buildString {
        evento.aumento?.takeIf { evento.fuerte }?.let {
            append("Sueles vender ${(it * 100).toInt()} % más. ")
        }
        if (evento.conHistorial) append("Esperamos vender ~${evento.productosEsperados.toInt()} productos. ")
        if (porComprar.isEmpty()) {
            append("Tus insumos alcanzan ✅")
        } else {
            append("Te falta: ")
            append(porComprar.take(4).joinToString(", ") { it.texto })
            if (porComprar.size > 4) append(" y ${porComprar.size - 4} más")
            append(".")
        }
    }
    return titulo to cuerpo.trim()
}

private val ISO = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val BONITO = SimpleDateFormat("EEEE d", Locale.forLanguageTag("es-MX"))

/** "2026-10-31" → "sábado 31". */
fun nombreDelDia(fecha: String): String =
    runCatching { synchronized(ISO) { BONITO.format(ISO.parse(fecha)!!) } }.getOrDefault(fecha)
