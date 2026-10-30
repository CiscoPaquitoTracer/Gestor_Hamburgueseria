package com.example.hamgurgueseria_monchosos.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hamgurgueseria_monchosos.R
import com.example.hamgurgueseria_monchosos.auth.GoogleAuthClient
import com.example.hamgurgueseria_monchosos.auth.ResultadoLogin
import com.example.hamgurgueseria_monchosos.ui.theme.HamgurgueseriamonchososTheme
import kotlinx.coroutines.launch

/** Al iniciar sesión, Supabase cambia el estado de la sesión y MainActivity cambia de pantalla sola. */
@Composable
fun LoginScreen(auth: GoogleAuthClient) {
    val scope = rememberCoroutineScope()
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LoginContenido(
        cargando = cargando,
        error = error,
        onGoogle = {
            cargando = true
            error = null
            scope.launch {
                when (val r = auth.iniciarSesion()) {
                    ResultadoLogin.Exito, ResultadoLogin.Cancelado -> Unit
                    is ResultadoLogin.Error -> error = r.mensaje
                }
                cargando = false
            }
        },
        onModoPrueba = null,
    )
}

@Composable
private fun LoginContenido(
    cargando: Boolean,
    error: String?,
    onGoogle: () -> Unit,
    onModoPrueba: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // El mismo dibujo del ícono de la app, sobre su fondo café.
        Image(
            painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Monchosos",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Gestor de la hamburguesería",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(48.dp))

        OutlinedButton(
            onClick = onGoogle,
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, Color(0xFFDADCE0)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1F1F1F),
            ),
        ) {
            if (cargando) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("G", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4285F4))
                Spacer(Modifier.width(12.dp))
                Text("Iniciar sesión con Google", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        if (onModoPrueba != null) {
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onModoPrueba, enabled = !cargando) {
                Text("Entrar en modo prueba (solo debug)")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    HamgurgueseriamonchososTheme {
        LoginContenido(cargando = false, error = null, onGoogle = {}, onModoPrueba = {})
    }
}
