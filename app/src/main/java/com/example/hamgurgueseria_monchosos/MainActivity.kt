package com.example.hamgurgueseria_monchosos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                    val usuario by app.sesion.usuario.collectAsStateWithLifecycle()
                    val scope = rememberCoroutineScope()

                    val actual = usuario
                    if (actual == null) {
                        LoginScreen(onSesionIniciada = app.sesion::iniciar)
                    } else {
                        AppPrincipal(
                            usuario = actual,
                            repo = app.inventario,
                            onCerrarSesion = {
                                scope.launch { GoogleAuthClient(this@MainActivity).cerrarSesion() }
                                app.sesion.cerrar()
                            },
                        )
                    }
                }
            }
        }
    }
}
