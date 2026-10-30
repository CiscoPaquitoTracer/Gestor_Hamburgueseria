package com.example.hamgurgueseria_monchosos.ui.pedidos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamgurgueseria_monchosos.data.formatearCentavos
import com.example.hamgurgueseria_monchosos.data.menu.CategoriaMenu
import com.example.hamgurgueseria_monchosos.data.menu.Menu
import com.example.hamgurgueseria_monchosos.data.menu.ProductoMenu
import com.example.hamgurgueseria_monchosos.data.menu.armarRenglon
import com.example.hamgurgueseria_monchosos.data.pedidos.Renglon
import com.example.hamgurgueseria_monchosos.data.pedidos.TipoEntrega
import com.example.hamgurgueseria_monchosos.ui.comun.DialogoConfirmar
import com.example.hamgurgueseria_monchosos.ui.comun.EncabezadoSeccion
import com.example.hamgurgueseria_monchosos.ui.comun.Panel
import com.example.hamgurgueseria_monchosos.ui.comun.icono

@Composable
fun PedidoEditorScreen(vm: PedidoEditorViewModel, onGuardado: (Long) -> Unit, onSalir: () -> Unit) {
    val s by vm.estado.collectAsStateWithLifecycle()
    var confirmarSalir by remember { mutableStateOf(false) }

    if (s.cargando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // Salir con productos o datos sin guardar los perdería: se pregunta antes.
    if (confirmarSalir) {
        DialogoConfirmar(
            titulo = if (s.pedido.id == 0L) "¿Descartar el pedido?" else "¿Descartar los cambios?",
            texto = "Lo que no hayas guardado se perderá.",
            accion = "Descartar",
            cancelar = "Seguir editando",
            peligroso = true,
            onConfirmar = onSalir,
            onCerrar = { confirmarSalir = false },
        )
    }

    when (s.paso) {
        PasoEditor.MENU -> {
            BackHandler(enabled = s.hayCambios) { confirmarSalir = true }
            PasoMenu(s, vm)
        }
        PasoEditor.RESUMEN -> {
            BackHandler { vm.irA(PasoEditor.MENU) }
            PasoResumen(s, vm, onGuardado)
        }
    }
}

// ---------------------------------------------------------------- Paso 1: menú
@Composable
private fun PasoMenu(s: EditorUiState, vm: PedidoEditorViewModel) {
    var configurando by remember { mutableStateOf<ProductoMenu?>(null) }
    // Se observa el catálogo para que un cambio de precio o disponibilidad se vea al momento.
    val catalogo by Menu.catalogo.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoriaMenu.entries.forEach { c ->
                FilterChip(
                    selected = s.categoria == c,
                    onClick = { vm.elegirCategoria(c) },
                    label = { Text(c.etiqueta) },
                )
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (s.categoria == CategoriaMenu.BURGERS || s.categoria == CategoriaMenu.DOGGOS) {
                item {
                    Text(Menu.NOTA, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(catalogo.filter { it.categoria == s.categoria && it.activo }, key = { it.id }) { producto ->
                ProductoFila(producto) {
                    if (producto.necesitaConfigurar) configurando = producto
                    else armarRenglon(producto).onSuccess(vm::agregar)
                }
            }
        }

        BarraCarrito(s.articulos, s.total, "Revisar pedido") { vm.irA(PasoEditor.RESUMEN) }
    }

    configurando?.let { p ->
        ConfigurarProductoDialog(
            producto = p,
            onAgregar = { vm.agregar(it); configurando = null },
            onCerrar = { configurando = null },
        )
    }
}

@Composable
private fun ProductoFila(producto: ProductoMenu, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                if (producto.descripcion.isNotBlank()) {
                    Text(
                        producto.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                (if (producto.precio == null) "desde " else "") + formatearCentavos(producto.precioDesde),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun BarraCarrito(articulos: Int, total: Long, accion: String, onClick: () -> Unit) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$articulos artículo${if (articulos == 1) "" else "s"}", style = MaterialTheme.typography.labelMedium)
                Text(formatearCentavos(total), style = MaterialTheme.typography.titleLarge)
            }
            Button(onClick = onClick, enabled = articulos > 0) {
                Text(accion)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ---------------------------------------------------------- Paso 2: resumen
@Composable
private fun PasoResumen(s: EditorUiState, vm: PedidoEditorViewModel, onGuardado: (Long) -> Unit) {
    var editandoNotas by remember { mutableStateOf<Int?>(null) }
    var quitando by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Titulo("Productos") }
        itemsIndexed(s.renglones) { i, r ->
            RenglonEditable(
                r,
                onMenos = { if (r.cantidad == 1) quitando = i else vm.cambiarCantidad(i, -1) },
                onMas = { vm.cambiarCantidad(i, +1) },
                onNotas = { editandoNotas = i },
            )
        }
        item {
            OutlinedButton(onClick = { vm.irA(PasoEditor.MENU) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Agregar más productos")
            }
        }

        item { Titulo("Entrega") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoEntrega.entries.forEach { t ->
                    FilterChip(
                        selected = s.pedido.tipoEntrega == t,
                        onClick = { vm.actualizarPedido { it.copy(tipoEntrega = t) } },
                        label = { Text(t.etiqueta) },
                        leadingIcon = { Icon(t.icono, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }
        item {
            Campo("Cliente (opcional)", s.pedido.cliente, KeyboardCapitalization.Words) { v ->
                vm.actualizarPedido { it.copy(cliente = v) }
            }
        }
        if (s.pedido.tipoEntrega != TipoEntrega.LOCAL) {
            item {
                Campo("Teléfono", s.pedido.telefono, tipo = KeyboardType.Phone) { v ->
                    vm.actualizarPedido { it.copy(telefono = v) }
                }
            }
        }
        if (s.pedido.tipoEntrega == TipoEntrega.DOMICILIO) {
            item {
                Campo("Dirección (Xochitepec, envío gratis)", s.pedido.direccion, KeyboardCapitalization.Sentences, unaLinea = false) { v ->
                    vm.actualizarPedido { it.copy(direccion = v) }
                }
            }
        }
        item {
            Campo("Notas del pedido", s.pedido.notas, KeyboardCapitalization.Sentences, unaLinea = false) { v ->
                vm.actualizarPedido { it.copy(notas = v) }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Total", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(formatearCentavos(s.total), style = MaterialTheme.typography.titleLarge)
            }
        }
        s.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item {
            Button(
                onClick = { vm.guardar(onGuardado) },
                enabled = !s.guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (s.pedido.id == 0L) "Guardar pedido" else "Guardar cambios", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    quitando?.let { i ->
        s.renglones.getOrNull(i)?.let { r ->
            DialogoConfirmar(
                titulo = "¿Quitar del pedido?",
                texto = listOfNotNull(r.nombre, r.variante).joinToString(" "),
                accion = "Quitar",
                peligroso = true,
                onConfirmar = { vm.cambiarCantidad(i, -1) },
                onCerrar = { quitando = null },
            )
        }
    }

    editandoNotas?.let { i ->
        s.renglones.getOrNull(i)?.let { r ->
            NotasDialog(r.notas, onGuardar = { vm.cambiarNotas(i, it); editandoNotas = null }) {
                editandoNotas = null
            }
        }
    }
}

@Composable
private fun RenglonEditable(r: Renglon, onMenos: () -> Unit, onMas: () -> Unit, onNotas: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), onClick = onNotas) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                DescripcionRenglon(r)
                Text(
                    formatearCentavos(r.subtotal),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onMenos) {
                if (r.cantidad == 1) Icon(Icons.Outlined.Delete, contentDescription = "Quitar")
                else Icon(Icons.Filled.Remove, contentDescription = "Uno menos")
            }
            Text("${r.cantidad}", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onMas) { Icon(Icons.Filled.Add, contentDescription = "Uno más") }
        }
    }
}

/** Nombre + variante/sabor + lo elegido en el combo + notas. Se usa en editor y detalle. */
@Composable
fun DescripcionRenglon(r: Renglon, conCantidad: Boolean = false) {
    Text(
        (if (conCantidad) "${r.cantidad}× " else "") + listOfNotNull(r.nombre, r.variante).joinToString(" "),
        style = MaterialTheme.typography.titleMedium,
    )
    val detalle = listOfNotNull(r.sabor, r.componentes).joinToString(" · ")
    if (detalle.isNotEmpty()) {
        Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (r.notas.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Outlined.Notes,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                r.notas,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun NotasDialog(inicial: String, onGuardar: (String) -> Unit, onCerrar: () -> Unit) {
    var texto by remember { mutableStateOf(inicial) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Notas del producto") },
        text = {
            OutlinedTextField(texto, { texto = it }, label = { Text("Ej. sin cebolla, bien dorada") })
        },
        confirmButton = { TextButton(onClick = { onGuardar(texto) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun Titulo(texto: String) {
    EncabezadoSeccion(texto)
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    mayusculas: KeyboardCapitalization = KeyboardCapitalization.None,
    tipo: KeyboardType = KeyboardType.Text,
    unaLinea: Boolean = true,
    onCambio: (String) -> Unit,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = unaLinea,
        keyboardOptions = KeyboardOptions(capitalization = mayusculas, keyboardType = tipo),
        modifier = Modifier.fillMaxWidth(),
    )
}
