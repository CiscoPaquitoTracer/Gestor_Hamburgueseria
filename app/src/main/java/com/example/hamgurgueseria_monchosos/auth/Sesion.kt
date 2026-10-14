package com.example.hamgurgueseria_monchosos.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class Usuario(val nombre: String, val correo: String, val fotoUrl: String? = null)

sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data object SinSesion : EstadoSesion
    data class Activa(val usuario: Usuario) : EstadoSesion
}

/** La sesión la guarda y renueva Supabase Auth; aquí solo se traduce para la UI. */
class Sesion(supabase: SupabaseClient, scope: CoroutineScope) {
    val estado: StateFlow<EstadoSesion> = supabase.auth.sessionStatus
        .map { s ->
            when (s) {
                is SessionStatus.Authenticated -> s.session.user?.let { EstadoSesion.Activa(it.aUsuario()) }
                    ?: EstadoSesion.SinSesion
                is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure -> EstadoSesion.SinSesion
                SessionStatus.Initializing -> EstadoSesion.Cargando
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, EstadoSesion.Cargando)
}

private fun UserInfo.aUsuario(): Usuario {
    fun meta(clave: String) = userMetadata?.get(clave)?.jsonPrimitive?.contentOrNull
    val correo = email.orEmpty()
    return Usuario(
        nombre = meta("full_name") ?: meta("name") ?: correo,
        correo = correo,
        fotoUrl = meta("avatar_url") ?: meta("picture"),
    )
}
