package com.example.hamgurgueseria_monchosos.ui.navegacion

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hamgurgueseria_monchosos.auth.Usuario
import com.example.hamgurgueseria_monchosos.data.InventarioRepository
import com.example.hamgurgueseria_monchosos.ui.inicio.InicioScreen
import com.example.hamgurgueseria_monchosos.ui.inventario.InventarioScreen
import com.example.hamgurgueseria_monchosos.ui.inventario.InventarioViewModel

/** Secciones de la barra inferior. Para agregar una (ej. Ventas) basta con sumarla aquí y en el NavHost. */
private enum class Seccion(val ruta: String, val titulo: String, val icono: String) {
    INICIO("inicio", "Inicio", "🏠"),
    INVENTARIO("inventario", "Inventario", "📦"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPrincipal(usuario: Usuario, repo: InventarioRepository, onCerrarSesion: () -> Unit) {
    val nav = rememberNavController()
    val rutaActual = nav.currentBackStackEntryAsState().value?.destination?.route
    val inventarioVm: InventarioViewModel = viewModel { InventarioViewModel(repo) }
    val seccion = Seccion.entries.firstOrNull { it.ruta == rutaActual } ?: Seccion.INICIO

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(seccion.titulo) },
                actions = { TextButton(onClick = onCerrarSesion) { Text("Salir") } },
            )
        },
        bottomBar = {
            NavigationBar {
                Seccion.entries.forEach { s ->
                    NavigationBarItem(
                        selected = s == seccion,
                        onClick = {
                            nav.navigate(s.ruta) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(s.icono) },
                        label = { Text(s.titulo) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Seccion.INICIO.ruta, modifier = Modifier.padding(padding)) {
            composable(Seccion.INICIO.ruta) { InicioScreen(usuario, inventarioVm) }
            composable(Seccion.INVENTARIO.ruta) { InventarioScreen(inventarioVm) }
        }
    }
}
