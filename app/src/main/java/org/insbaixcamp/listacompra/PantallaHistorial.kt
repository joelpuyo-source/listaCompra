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
import java.util.Locale

// Aqui se ven las compras guardadas.
@Composable
fun PantallaHistorial(historial: List<ListaGuardada>, cargando: Boolean, error: String) {
    val locale = LocalLocale.current.platformLocale
    if (cargando) CircularProgressIndicator()
    if (error.isNotEmpty()) Text(error)
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
        items(historial) { lista ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(lista.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(SimpleDateFormat("dd/MM/yyyy", locale).format(Date(lista.fecha)))
                    lista.productos.forEach { Text("${it.nombre} × ${it.cantidad} — ${it.precio * it.cantidad} €") }
                    Text("Total: ${lista.productos.sumOf { it.precio * it.cantidad }} €")
                }
            }
        }
    }
}
