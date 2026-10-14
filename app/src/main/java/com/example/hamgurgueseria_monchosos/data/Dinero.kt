package com.example.hamgurgueseria_monchosos.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 16000 → "$160", 15050 → "$150.50", 149000 → "$1,490". */
fun formatearCentavos(centavos: Long): String {
    val pesos = centavos / 100
    val resto = kotlin.math.abs(centavos % 100)
    val signo = if (centavos < 0) "-" else ""
    val entero = "%,d".format(Locale.US, kotlin.math.abs(pesos))
    return if (resto == 0L) "$signo$$entero" else "$signo$$entero.%02d".format(resto)
}

/** "160", "160.5", "$1,490" → centavos (la coma es separador de miles). null si no es un monto válido. */
fun parsearPesos(texto: String): Long? {
    val limpio = texto.trim().removePrefix("$").replace(",", "").trim()
    if (limpio.isEmpty()) return null
    val valor = limpio.toBigDecimalOrNull() ?: return null
    if (valor.signum() < 0 || valor.scale() > 2) return null
    return valor.movePointRight(2).toLong()
}

private val LOCALE_MX: Locale = Locale.forLanguageTag("es-MX")

fun formatearHora(millis: Long): String = SimpleDateFormat("HH:mm", LOCALE_MX).format(Date(millis))

fun formatearFechaHora(millis: Long): String =
    SimpleDateFormat("EEE d 'de' MMM · HH:mm", LOCALE_MX).format(Date(millis))

/** Medianoche de hoy (hora del teléfono) en milisegundos. */
fun inicioDelDia(ahora: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance().apply {
        timeInMillis = ahora
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
