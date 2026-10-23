package org.insbaixcamp.listacompra

import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun PantallaHistorial(
    historial: List<ListaGuardada>,
    cargando: Boolean,
    error: String
) {
    val localizacion = LocalLocale.current.platformLocale

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator()
        }

        if (error.isNotEmpty()) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(historial) { lista ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = lista.nombre,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = SimpleDateFormat("dd/MM/yyyy", localizacion).format(Date(lista.fecha)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        lista.productos.forEach { producto ->
                            Text(
                                text = "${producto.nombre} × ${producto.cantidad} — ${"%.2f".format(producto.precio * producto.cantidad)} €",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val precioTotalLista = lista.productos.sumOf { it.precio * it.cantidad }
                        Text(
                            text = "Total: ${"%.2f".format(precioTotalLista)} €",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}
