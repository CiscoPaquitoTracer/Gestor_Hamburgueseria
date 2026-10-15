package com.example.hamgurgueseria_monchosos.data.remoto

import com.example.hamgurgueseria_monchosos.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Cliente único de Supabase. Usa la llave pública (publishable): la seguridad real
 * está en la base de datos (RLS, ver supabase/README.md).
 */
fun crearSupabase(): SupabaseClient = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
) {
    defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
    install(Auth)        // guarda y renueva la sesión sola
    install(Postgrest)
}

/** Mensaje entendible para mostrar en pantalla. */
fun mensajeDeError(e: Throwable): String = when (e) {
    is HttpRequestException, is IOException -> "Sin conexión a internet. Revisa el Wi-Fi o los datos e intenta de nuevo."
    // Los RAISE EXCEPTION de la base de datos ya vienen en español (ej. "Falta la dirección de entrega").
    is RestException -> e.error.ifBlank { "Error del servidor" }.let { msg ->
        if ("permission denied" in msg || "row-level security" in msg) "Tu cuenta no tiene permiso para hacer esto." else msg
    }
    else -> e.message ?: "Ocurrió un error inesperado."
}

private val ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

/** Milisegundos → texto ISO-8601 para filtrar por fecha en Supabase. */
fun aIso(millis: Long): String = synchronized(ISO) { ISO.format(Date(millis)) }

/** "2026-10-13T19:44:05.399489+00:00" (timestamptz de Postgres) → milisegundos. */
@OptIn(kotlin.time.ExperimentalTime::class)
fun desdeIso(texto: String): Long = kotlin.time.Instant.parse(texto).toEpochMilliseconds()

/** numeric de Postgres (pesos) → centavos, sin errores de redondeo. */
fun aCentavos(pesos: Double): Long = Math.round(pesos * 100)

fun aPesos(centavos: Long): Double = centavos / 100.0
