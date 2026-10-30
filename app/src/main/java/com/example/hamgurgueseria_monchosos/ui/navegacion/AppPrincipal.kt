package com.example.hamgurgueseria_monchosos.ui.navegacion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import com.example.hamgurgueseria_monchosos.data.pronostico.PronosticoRepository
import com.example.hamgurgueseria_monchosos.data.resumen.ResumenRepository
import com.example.hamgurgueseria_monchosos.ui.comun.ActualizarPeriodicamente
import com.example.hamgurgueseria_monchosos.ui.inicio.InicioScreen
import com.example.hamgurgueseria_monchosos.ui.inicio.PronosticoViewModel
import com.example.hamgurgueseria_monchosos.ui.inventario.InventarioScreen
import com.example.hamgurgueseria_monchosos.ui.inventario.InventarioViewModel
import com.example.hamgurgueseria_monchosos.ui.menu.MenuScreen
import com.example.hamgurgueseria_monchosos.ui.menu.MenuViewModel
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidoDetalleScreen
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidoEditorScreen
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidoEditorViewModel
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidosScreen
import com.example.hamgurgueseria_monchosos.ui.pedidos.PedidosViewModel
import com.example.hamgurgueseria_monchosos.ui.resumen.ResumenScreen
import com.example.hamgurgueseria_monchosos.ui.resumen.ResumenViewModel

/**
 * Secciones de la barra inferior (solo íconos; el nombre va en la barra de arriba). Para agregar
 * una basta con sumarla aquí y en el NavHost.
 */
private enum class Seccion(val ruta: String, val titulo: String, val icono: ImageVector, val iconoActivo: ImageVector) {
    INICIO("inicio", "Inicio", Icons.Outlined.Home, Icons.Filled.Home),
    PEDIDOS("pedidos", "Pedidos", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    RESUMEN("resumen", "Resumen", Icons.Outlined.BarChart, Icons.Filled.BarChart),
    INVENTARIO("inventario", "Inventario", Icons.Outlined.Inventory2, Icons.Filled.Inventory2),
    MENU("menu", "Menú", Icons.Outlined.RestaurantMenu, Icons.Filled.RestaurantMenu),
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
    pronostico: PronosticoRepository,
    resumen: ResumenRepository,
    onCerrarSesion: () -> Unit,
) {
    val nav = rememberNavController()
    val entrada = nav.currentBackStackEntryAsState().value
    val ruta = entrada?.destination?.route
    val seccion = Seccion.entries.firstOrNull { it.ruta == ruta }
    val idActual = entrada?.arguments?.getLong("id")
    val despachador = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val inventarioVm: InventarioViewModel = viewModel { InventarioViewModel(inventario) }
    val pedidosVm: PedidosViewModel = viewModel { PedidosViewModel(pedidos) }
    val menuVm: MenuViewModel = viewModel { MenuViewModel(menu) }
    val pronosticoVm: PronosticoViewModel = viewModel { PronosticoViewModel(pronostico) }
    val resumenVm: ResumenViewModel = viewModel { ResumenViewModel(resumen) }
    PedirPermisoNotificaciones()
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
                    if (seccion == null) {
                        // Pasa por el sistema para que cada pantalla pueda interceptarlo (ej. el editor
                        // pregunta antes de descartar un pedido sin guardar).
                        IconButton(onClick = { despachador?.onBackPressed() ?: nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                },
                actions = {
                    if (seccion != null) BotonPerfil(usuario, onCerrarSesion)
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
                            icon = { Icon(if (s == seccion) s.iconoActivo else s.icono, contentDescription = s.titulo) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Seccion.INICIO.ruta, modifier = Modifier.padding(padding)) {
            composable(Seccion.INICIO.ruta) {
                InicioScreen(inventarioVm, pedidosVm, pronosticoVm, onNuevoPedido = { nav.navigate(Rutas.PEDIDO_NUEVO) })
            }
            composable(Seccion.PEDIDOS.ruta) {
                PedidosScreen(
                    pedidosVm,
                    onNuevo = { nav.navigate(Rutas.PEDIDO_NUEVO) },
                    onAbrir = { nav.navigate(Rutas.pedido(it)) },
                )
            }
            composable(Seccion.RESUMEN.ruta) { ResumenScreen(resumenVm) }
            composable(Seccion.INVENTARIO.ruta) { InventarioScreen(inventarioVm) }
            composable(Seccion.MENU.ruta) { MenuScreen(menuVm) }

            composable(Rutas.PEDIDO_NUEVO) {
                val vm: PedidoEditorViewModel = viewModel { PedidoEditorViewModel(pedidos, null) }
                PedidoEditorScreen(
                    vm,
                    onGuardado = { id ->
                        // Al guardar se abre el detalle (para cobrar) en lugar del editor.
                        nav.navigate(Rutas.pedido(id)) { popUpTo(Rutas.PEDIDO_NUEVO) { inclusive = true } }
                    },
                    onSalir = { nav.popBackStack() },
                )
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
                PedidoEditorScreen(vm, onGuardado = { nav.popBackStack() }, onSalir = { nav.popBackStack() })
            }
        }
    }
}

/** Android 13+ pide permiso para notificar; se pregunta una vez al entrar (avisos de reabasto). */
@Composable
private fun PedirPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val contexto = LocalContext.current
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val concedido = ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!concedido) lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
