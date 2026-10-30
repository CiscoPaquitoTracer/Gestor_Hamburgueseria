package com.example.hamgurgueseria_monchosos.ui.comun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.TakeoutDining
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.hamgurgueseria_monchosos.data.pedidos.TipoEntrega

/**
 * Contenedor base de la app: blanco, con borde fino y sin sombra. [acento] pinta una franja a la
 * izquierda para marcar un estado (por agotarse, por cobrar…) sin teñir todo el panel.
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    acento: Color? = null,
    onClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val forma = MaterialTheme.shapes.medium
    Surface(
        modifier = modifier.clip(forma).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = forma,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            if (acento != null) Box(Modifier.width(4.dp).fillMaxHeight().background(acento))
            Column(Modifier.weight(1f), content = contenido)
        }
    }
}

/** Panel en el color de la marca para la cifra principal de una pantalla. */
@Composable
fun PanelDestacado(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(Modifier.padding(16.dp), content = contenido)
    }
}

/** "HOY", "INVENTARIO"…: separa bloques sin meterlos en otra tarjeta. */
@Composable
fun EncabezadoSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 8.dp),
    )
}

/** Una cifra con su etiqueta encima (ventas, pedidos, días abiertos…). */
@Composable
fun Dato(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Panel(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Ícono pequeño seguido de texto, para datos de contacto, avisos y similares. */
@Composable
fun TextoConIcono(
    icono: ImageVector,
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icono,
            contentDescription = null,
            tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color,
            modifier = Modifier.size(18.dp),
        )
        Text(texto, color = color, modifier = Modifier.padding(start = 8.dp))
    }
}

val TipoEntrega.icono: ImageVector
    get() = when (this) {
        TipoEntrega.LOCAL -> Icons.Outlined.Restaurant
        TipoEntrega.PARA_LLEVAR -> Icons.Outlined.TakeoutDining
        TipoEntrega.DOMICILIO -> Icons.Outlined.DeliveryDining
    }
