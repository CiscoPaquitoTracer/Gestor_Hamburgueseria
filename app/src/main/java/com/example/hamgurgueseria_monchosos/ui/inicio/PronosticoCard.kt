package com.example.hamgurgueseria_monchosos.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hamgurgueseria_monchosos.data.pronostico.Pronostico
import com.example.hamgurgueseria_monchosos.data.pronostico.nombreDelDia
import com.example.hamgurgueseria_monchosos.ui.comun.Panel
import com.example.hamgurgueseria_monchosos.ui.comun.TextoConIcono
import com.example.hamgurgueseria_monchosos.ui.theme.Exito

/** "Se viene el fin de semana": productos que se espera vender y qué comprar para cubrirlo. */
@Composable
fun PronosticoCard(p: Pronostico) {
    val evento = p.evento
    val sinHistorial = p.dias.all { it.diasHistorial == 0 }
    Panel(
        Modifier.fillMaxWidth(),
        acento = if (p.porComprar.isNotEmpty()) MaterialTheme.colorScheme.secondary else null,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
                Text(
                    if (evento != null) "Se viene ${evento.nombre}" else "Próximos días",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (sinHistorial) {
                Text(
                    "Aún no hay suficientes ventas para estimar. Con unas semanas de pedidos verás aquí cuánto " +
                        "sueles vender en fines de semana y festivos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            val dias = evento?.dias ?: p.dias.drop(1).take(3)
            dias.forEach { d ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        nombreDelDia(d.fecha).replaceFirstChar { it.uppercase() } + (d.festivo?.let { " · $it" } ?: ""),
                        Modifier.weight(1f),
                    )
                    // Con historial en otros días, un día sin ventas registradas es un día que no abren.
                    Text(
                        d.productosEsperados?.let { "~${it.toInt()} productos" } ?: "no suele abrir",
                        fontWeight = if (d.fuerte) FontWeight.Bold else FontWeight.Normal,
                        color = if (d.productosEsperados == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    if (d.fuerte) {
                        Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = "Día fuerte",
                            tint = Exito, modifier = Modifier.padding(start = 4.dp).size(18.dp))
                    }
                }
            }
            evento?.aumento?.takeIf { evento.fuerte }?.let {
                Text(
                    "Sueles vender ${(it * 100).toInt()} % más que un día normal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            if (p.porComprar.isEmpty()) {
                TextoConIcono(Icons.Outlined.CheckCircle, "Tus insumos alcanzan", color = Exito)
            } else {
                TextoConIcono(Icons.Outlined.ShoppingCart, "Conviene comprar")
                p.porComprar.forEach {
                    Text(it.texto, Modifier.padding(start = 26.dp))
                }
            }
        }
    }
}
