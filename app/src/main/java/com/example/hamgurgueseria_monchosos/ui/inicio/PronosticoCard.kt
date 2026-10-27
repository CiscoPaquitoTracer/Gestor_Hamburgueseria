package com.example.hamgurgueseria_monchosos.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hamgurgueseria_monchosos.data.pronostico.Pronostico
import com.example.hamgurgueseria_monchosos.data.pronostico.nombreDelDia

/** "Se viene el fin de semana": pedidos esperados y qué comprar para cubrirlo. */
@Composable
fun PronosticoCard(p: Pronostico) {
    val evento = p.evento
    val sinHistorial = p.dias.all { it.diasHistorial == 0 }
    Card(
        Modifier.fillMaxWidth(),
        colors = if (p.porComprar.isNotEmpty()) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        } else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (evento != null) "📅 Se viene ${evento.nombre}" else "📅 Próximos días",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
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
                Row {
                    Text(
                        nombreDelDia(d.fecha).replaceFirstChar { it.uppercase() } + (d.festivo?.let { " · $it" } ?: ""),
                        Modifier.weight(1f),
                    )
                    Text(
                        d.pedidosEsperados?.let { "~${it.toInt()} pedidos" + if (d.fuerte) " ↑" else "" } ?: "sin datos",
                        fontWeight = if (d.fuerte) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
            evento?.aumento?.takeIf { evento.fuerte }?.let {
                Text("Sueles vender ${(it * 100).toInt()} % más que un día normal.", style = MaterialTheme.typography.bodySmall)
            }

            if (p.porComprar.isEmpty()) {
                Text("Tus insumos alcanzan ✅", Modifier.padding(top = 4.dp))
            } else {
                Text("Conviene comprar:", Modifier.padding(top = 4.dp), fontWeight = FontWeight.SemiBold)
                p.porComprar.forEach { Text("• ${it.texto}") }
            }
        }
    }
}
