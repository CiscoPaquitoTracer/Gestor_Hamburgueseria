package com.example.hamgurgueseria_monchosos.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamgurgueseria_monchosos.auth.Usuario
import com.example.hamgurgueseria_monchosos.data.formatearCantidad
import com.example.hamgurgueseria_monchosos.data.formatearDinero
import com.example.hamgurgueseria_monchosos.ui.inventario.InventarioViewModel

/** Resumen rápido del negocio, al estilo del "balance" de Treinta. */
@Composable
fun InicioScreen(usuario: Usuario, vm: InventarioViewModel) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Hola, ${usuario.nombre.substringBefore(' ')} 👋", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tarjeta("Insumos registrados", estado.insumos.size.toString(), Modifier.weight(1f))
            Tarjeta("Valor del inventario", formatearDinero(estado.valorTotal), Modifier.weight(1f))
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Por agotarse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (estado.porAgotarse.isEmpty()) {
                    Text("Todo en orden ✅")
                } else {
                    estado.porAgotarse.forEach {
                        Text("• ${it.nombre}: quedan ${formatearCantidad(it.cantidad, it.unidad)}")
                    }
                }
            }
        }

        Text(
            "Próximamente: ventas, gastos y balance del día.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Tarjeta(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium)
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
