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
import com.example.hamgurgueseria_monchosos.data.remoto.mensajeDeError
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.security.SecureRandom

sealed interface ResultadoLogin {
    data object Exito : ResultadoLogin
    data object Cancelado : ResultadoLogin
    data class Error(val mensaje: String) : ResultadoLogin
}

@Serializable
private data class Permitido(val correo: String)

/**
 * Botón "Iniciar sesión con Google" → Supabase Auth.
 * Google entrega un ID token; Supabase lo verifica en el servidor y abre la sesión.
 * El acceso real lo decide la tabla usuarios_permitidos (RLS).
 */
class GoogleAuthClient(private val context: Context, private val supabase: SupabaseClient) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun iniciarSesion(): ResultadoLogin {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank() || BuildConfig.SUPABASE_URL.isBlank()) {
            return ResultadoLogin.Error("Falta configurar GOOGLE_WEB_CLIENT_ID o SUPABASE_URL en local.properties.")
        }
        // Nonce: Google firma el hash; Supabase comprueba que corresponde al valor original.
        // Así un token interceptado no se puede reutilizar.
        val nonce = nonceAleatorio()
        val opcion = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(sha256(nonce))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(opcion).build()

        val idToken = try {
            val credencial = credentialManager.getCredential(context, request).credential
            if (credencial !is CustomCredential ||
                credencial.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) return ResultadoLogin.Error("Tipo de credencial no reconocido.")
            GoogleIdTokenCredential.createFrom(credencial.data).idToken
        } catch (e: GetCredentialCancellationException) {
            return ResultadoLogin.Cancelado
        } catch (e: NoCredentialException) {
            return ResultadoLogin.Error("No hay cuentas de Google en este teléfono.")
        } catch (e: GetCredentialException) {
            return ResultadoLogin.Error("No se pudo iniciar sesión: ${e.message}")
        } catch (e: GoogleIdTokenParsingException) {
            return ResultadoLogin.Error("Respuesta de Google inválida.")
        }

        return try {
            supabase.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
                this.nonce = nonce
            }
            // RLS solo deja ver tu propio renglón: si no hay, la cuenta no está autorizada.
            val permitido = supabase.from("usuarios_permitidos").select().decodeList<Permitido>().isNotEmpty()
            if (permitido) ResultadoLogin.Exito
            else {
                val correo = supabase.auth.currentUserOrNull()?.email.orEmpty()
                cerrarSesion()
                ResultadoLogin.Error("La cuenta $correo no tiene acceso. Pide que la agreguen en Supabase (usuarios_permitidos).")
            }
        } catch (e: Exception) {
            runCatching { supabase.auth.signOut() }
            ResultadoLogin.Error(mensajeDeError(e))
        }
    }

    suspend fun cerrarSesion() {
        runCatching { supabase.auth.signOut() }
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    private fun nonceAleatorio(): String =
        ByteArray(32).also { aleatorio.nextBytes(it) }.joinToString("") { "%02x".format(it) }

    private fun sha256(texto: String): String =
        MessageDigest.getInstance("SHA-256").digest(texto.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        /** Una sola instancia para toda la app (recomendación de CodeQL). */
        val aleatorio = SecureRandom()
    }
}
