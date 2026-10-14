package com.example.hamgurgueseria_monchosos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamgurgueseria_monchosos.auth.EstadoSesion
import com.example.hamgurgueseria_monchosos.auth.GoogleAuthClient
import com.example.hamgurgueseria_monchosos.ui.login.LoginScreen
import com.example.hamgurgueseria_monchosos.ui.navegacion.AppPrincipal
import com.example.hamgurgueseria_monchosos.ui.theme.HamgurgueseriamonchososTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as HamburgueseriaApp

        setContent {
            HamgurgueseriamonchososTheme {
                Surface(Modifier.fillMaxSize()) {
                    val sesion by app.sesion.estado.collectAsStateWithLifecycle()
                    val scope = rememberCoroutineScope()
                    // Credential Manager necesita la Activity para mostrar el selector de cuentas.
                    val auth = remember { GoogleAuthClient(this@MainActivity, app.supabase) }

                    when (val s = sesion) {
                        EstadoSesion.Cargando ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        EstadoSesion.SinSesion -> LoginScreen(auth)
                        is EstadoSesion.Activa -> AppPrincipal(
                            usuario = s.usuario,
                            inventario = app.inventario,
                            pedidos = app.pedidos,
                            onCerrarSesion = { scope.launch { auth.cerrarSesion() } },
                        )
                    }
                }
            }
        }
    }
}
