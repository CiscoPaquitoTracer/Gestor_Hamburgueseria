package com.example.hamgurgueseria_monchosos.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.hamgurgueseria_monchosos.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed interface ResultadoLogin {
    data class Exito(val usuario: Usuario) : ResultadoLogin
    data object Cancelado : ResultadoLogin
    data class Error(val mensaje: String) : ResultadoLogin
}

/** Botón "Iniciar sesión con Google" usando Credential Manager. */
class GoogleAuthClient(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun iniciarSesion(): ResultadoLogin {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            return ResultadoLogin.Error(
                "Falta configurar GOOGLE_WEB_CLIENT_ID en local.properties (ver CONTEXTO.md)."
            )
        }
        val opcionGoogle = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(opcionGoogle).build()

        return try {
            val credencial = credentialManager.getCredential(context, request).credential
            if (credencial !is CustomCredential ||
                credencial.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return ResultadoLogin.Error("Tipo de credencial no reconocido.")
            }
            val google = GoogleIdTokenCredential.createFrom(credencial.data)
            val correo = google.id
            if (!correoPermitido(correo)) {
                return ResultadoLogin.Error("La cuenta $correo no tiene acceso a esta app.")
            }
            ResultadoLogin.Exito(
                Usuario(
                    nombre = google.displayName ?: correo,
                    correo = correo,
                    fotoUrl = google.profilePictureUri?.toString(),
                )
            )
        } catch (e: GetCredentialCancellationException) {
            ResultadoLogin.Cancelado
        } catch (e: NoCredentialException) {
            ResultadoLogin.Error("No hay cuentas de Google en este teléfono.")
        } catch (e: GetCredentialException) {
            ResultadoLogin.Error("No se pudo iniciar sesión: ${e.message}")
        } catch (e: GoogleIdTokenParsingException) {
            ResultadoLogin.Error("Respuesta de Google inválida.")
        }
    }

    /** Olvida la cuenta elegida para que la próxima vez vuelva a preguntar. */
    suspend fun cerrarSesion() {
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    private fun correoPermitido(correo: String): Boolean {
        val permitidos = BuildConfig.CORREOS_PERMITIDOS
            .split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        return permitidos.isEmpty() || correo.lowercase() in permitidos
    }
}
