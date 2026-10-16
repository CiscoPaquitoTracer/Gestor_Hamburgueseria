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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hamgurgueseria_monchosos.auth.Usuario
import com.example.hamgurgueseria_monchosos.data.InventarioRepository
import com.example.hamgurgueseria_monchosos.data.menu.MenuRepository
import com.example.hamgurgueseria_monchosos.data.pedidos.PedidosRepository
import com.example.hamgurgueseria_monchosos.ui.comun.ActualizarPeriodicamente
import com.example.hamgurgueseria_monchosos.ui.inicio.InicioScreen
import com.example.hamgurgueseria_monchosos.ui.inventario.InventarioScreen
import com.example.hamgurgueseria_monchosos.ui.inventario.InventarioViewModel
import com.example.hamgurgueseria_monchosos.ui.menu.MenuScreen
import com.example.hamgurgueseria_monchosos.ui.menu.MenuViewModel
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidoDetalleScreen
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidoEditorScreen
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidoEditorViewModel
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidosScreen
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidosViewModel

/** Secciones de la barra inferior. Para agregar una basta con sumarla aquí y en el NavHost. */
private enum class Seccion(val ruta: String, val titulo: String, val icono: String) {
    INICIO("inicio", "Inicio", "🏠"),
    PEDIDOS("pedidos", "Pedidos", "🧾"),
    INVENTARIO("inventario", "Inventario", "📦"),
    MENU("menu", "Menú", "🍔"),
}

/** Pantallas internas (sin barra inferior, con botón de regreso). */
private object Rutas {
    const val PEDIDO_NUEVO = "pedido-nuevo"
    const val PEDIDO = "pedido/{id}"
    const val PEDIDO_EDITAR = "pedido/{id}/editar"
    fun pedido(id: Long) = "pedido/$id"
    fun editar(id: Long) = "pedido/$id/editar"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPrincipal(
    usuario: Usuario,
    inventario: InventarioRepository,
    pedidos: PedidosRepository,
    menu: MenuRepository,
    onCerrarSesion: () -> Unit,
) {
    val nav = rememberNavController()
    val entrada = nav.currentBackStackEntryAsState().value
    val ruta = entrada?.destination?.route
    val seccion = Seccion.entries.firstOrNull { it.ruta == ruta }
    val idActual = entrada?.arguments?.getLong("id")

    val inventarioVm: InventarioViewModel = viewModel { InventarioViewModel(inventario) }
    val pedidosVm: PedidosViewModel = viewModel { PedidosViewModel(pedidos) }
    val menuVm: MenuViewModel = viewModel { MenuViewModel(menu) }
    // Precios del menú al día en toda la app (los cambia el otro teléfono también).
    ActualizarPeriodicamente(cadaMs = 120_000) { menuVm.refrescar() }

    val titulo = when (ruta) {
        Rutas.PEDIDO_NUEVO -> "Nuevo pedido"
        Rutas.PEDIDO -> "Pedido #$idActual"
        Rutas.PEDIDO_EDITAR -> "Editar pedido #$idActual"
        else -> seccion?.titulo ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    if (seccion == null) TextButton(onClick = { nav.popBackStack() }) { Text("← Atrás") }
                },
                actions = {
                    if (seccion != null) TextButton(onClick = onCerrarSesion) { Text("Salir") }
                },
            )
        },
        bottomBar = {
            if (seccion != null) {
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
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Seccion.INICIO.ruta, modifier = Modifier.padding(padding)) {
            composable(Seccion.INICIO.ruta) {
                InicioScreen(usuario, inventarioVm, pedidosVm, onNuevoPedido = { nav.navigate(Rutas.PEDIDO_NUEVO) })
            }
            composable(Seccion.PEDIDOS.ruta) {
                PedidosScreen(
                    pedidosVm,
                    onNuevo = { nav.navigate(Rutas.PEDIDO_NUEVO) },
                    onAbrir = { nav.navigate(Rutas.pedido(it)) },
                )
            }
            composable(Seccion.INVENTARIO.ruta) { InventarioScreen(inventarioVm) }
            composable(Seccion.MENU.ruta) { MenuScreen(menuVm) }

            composable(Rutas.PEDIDO_NUEVO) {
                val vm: PedidoEditorViewModel = viewModel { PedidoEditorViewModel(pedidos, null) }
                PedidoEditorScreen(vm, onGuardado = { id ->
                    // Al guardar se abre el detalle (para cobrar) en lugar del editor.
                    nav.navigate(Rutas.pedido(id)) { popUpTo(Rutas.PEDIDO_NUEVO) { inclusive = true } }
                })
            }
            composable(Rutas.PEDIDO, listOf(navArgument("id") { type = NavType.LongType })) { e ->
                val id = e.arguments!!.getLong("id")
                PedidoDetalleScreen(
                    id, pedidosVm,
                    onEditar = { nav.navigate(Rutas.editar(id)) },
                    onEliminado = { nav.popBackStack() },
                )
            }
            composable(Rutas.PEDIDO_EDITAR, listOf(navArgument("id") { type = NavType.LongType })) { e ->
                val id = e.arguments!!.getLong("id")
                val vm: PedidoEditorViewModel = viewModel { PedidoEditorViewModel(pedidos, id) }
                PedidoEditorScreen(vm, onGuardado = { nav.popBackStack() })
            }
        }
    }
}
