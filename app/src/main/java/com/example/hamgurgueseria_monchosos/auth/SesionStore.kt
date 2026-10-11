package com.example.hamgurgueseria_monchosos.auth

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Usuario(val nombre: String, val correo: String, val fotoUrl: String? = null)

/** Recuerda quién inició sesión para no pedir Google cada vez que se abre la app. */
class SesionStore(context: Context) {
    private val prefs = context.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    private val _usuario = MutableStateFlow(leer())
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun iniciar(usuario: Usuario) {
        prefs.edit {
            putString(NOMBRE, usuario.nombre)
            putString(CORREO, usuario.correo)
            putString(FOTO, usuario.fotoUrl)
        }
        _usuario.value = usuario
    }

    fun cerrar() {
        prefs.edit { clear() }
        _usuario.value = null
    }

    private fun leer(): Usuario? {
        val correo = prefs.getString(CORREO, null) ?: return null
        return Usuario(prefs.getString(NOMBRE, null) ?: correo, correo, prefs.getString(FOTO, null))
    }

    private companion object {
        const val NOMBRE = "nombre"
        const val CORREO = "correo"
        const val FOTO = "foto"
    }
}
