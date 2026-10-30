package com.example.hamgurgueseria_monchosos.ui.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hamgurgueseria_monchosos.auth.Usuario
import com.example.hamgurgueseria_monchosos.ui.comun.DialogoConfirmar

/**
 * Inicial del usuario en un círculo; al tocarla abre su perfil (nombre, correo y cerrar sesión).
 * Aquí se pueden sumar más opciones de cuenta cuando haga falta.
 */
@Composable
fun BotonPerfil(usuario: Usuario, onCerrarSesion: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { abierto = true }) { Avatar(usuario) }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).widthIn(max = 260.dp)) {
                Text(usuario.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    usuario.correo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("Cerrar sesión") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                onClick = { abierto = false; confirmar = true },
            )
        }
    }

    // Para volver a entrar hay que pasar otra vez por Google: mejor confirmar.
    if (confirmar) {
        DialogoConfirmar(
            titulo = "¿Cerrar sesión?",
            texto = "Tendrás que volver a entrar con tu cuenta de Google.",
            accion = "Cerrar sesión",
            onConfirmar = onCerrarSesion,
            onCerrar = { confirmar = false },
        )
    }
}

@Composable
private fun Avatar(usuario: Usuario) {
    Box(
        Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            usuario.nombre.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
