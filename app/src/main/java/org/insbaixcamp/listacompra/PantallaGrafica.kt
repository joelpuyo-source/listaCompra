package org.insbaixcamp.listacompra

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale

@Composable
fun PantallaGrafica(
    historial: List<ListaGuardada>,
    periodo: String,
    cambiarPeriodo: (String) -> Unit
) {
    val esSemana = periodo == "Semana"

    val fechas = (if (esSemana) 0..6 else 0..3).map { paso ->
        Calendar.getInstance().apply {
            if (esSemana) {
                add(Calendar.DAY_OF_YEAR, paso - 6)
            } else {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, paso - 3)
            }
        }
    }

    val gastos = fechas.map { fecha ->
        val inicio = (fecha.clone() as Calendar).apply {
            if (!esSemana) set(Calendar.DAY_OF_MONTH, 1)
            medianoche()
        }.timeInMillis

        val fin = (fecha.clone() as Calendar).apply {
            if (esSemana) {
                add(Calendar.DAY_OF_YEAR, 1)
            } else {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, 1)
            }
            medianoche()
        }.timeInMillis

        historial.filter { it.fecha in inicio until fin }
            .sumOf { lista -> lista.productos.sumOf { p -> p.precio * p.cantidad } }
    }

    val etiquetas = fechas.map {
        it.getDisplayName(
            if (esSemana) Calendar.DAY_OF_WEEK else Calendar.MONTH,
            Calendar.SHORT,
            Locale.forLanguageTag("es-ES")
        ) ?: ""
    }

    val maximo = (gastos.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val colorPrincipal = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { cambiarPeriodo("Semana") }) {
                Text("Semanal")
            }
            Button(onClick = { cambiarPeriodo("Mes") }) {
                Text("Mensual")
            }
        }

        Text(
            text = "Gasto ${periodo.lowercase()} (€)",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${"%.0f".format(maximo)} €", style = MaterialTheme.typography.bodySmall)
                Text("${"%.0f".format(maximo / 2)} €", style = MaterialTheme.typography.bodySmall)
                Text("0 €", style = MaterialTheme.typography.bodySmall)
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
            ) {
                val puntos = gastos.mapIndexed { index, gasto ->
                    Offset(
                        x = index * size.width / (gastos.size - 1).coerceAtLeast(1),
                        y = size.height * (1 - (gasto / maximo).toFloat())
                    )
                }

                for (i in 0..2) {
                    val y = size.height * i / 2
                    drawLine(
                        color = colorPrincipal.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val area = Path().apply {
                    moveTo(0f, size.height)
                    puntos.forEach { lineTo(it.x, it.y) }
                    lineTo(size.width, size.height)
                    close()
                }

                val linea = Path().apply {
                    puntos.forEachIndexed { index, p ->
                        if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                    }
                }

                drawPath(area, colorPrincipal.copy(alpha = 0.18f), style = Fill)
                drawPath(linea, colorPrincipal, style = Stroke(3.dp.toPx()))
                puntos.forEach { drawCircle(colorPrincipal, 5.dp.toPx(), it) }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            etiquetas.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }

        Text(
            text = "Total del periodo: ${"%.2f".format(gastos.sum())} €",
            style = MaterialTheme.typography.titleMedium
        )

        HorizontalDivider()

        val categorias = historial.flatMap { it.productos }.groupBy { it.categoria }
        val categoriaMasGasto = categorias.maxByOrNull { (_, productos) ->
            productos.sumOf { it.precio * it.cantidad }
        }?.key ?: "-"

        val categoriaMenosGasto = categorias.minByOrNull { (_, productos) ->
            productos.sumOf { it.precio * it.cantidad }
        }?.key ?: "-"

        val fechaMinima = historial.minOfOrNull { it.fecha } ?: System.currentTimeMillis()
        val semanasContadas = ((System.currentTimeMillis() - fechaMinima) / (7 * 24 * 60 * 60 * 1000L)).coerceAtLeast(1)
        val gastoTotal = historial.sumOf { lista -> lista.productos.sumOf { it.precio * it.cantidad } }

        val diaMayorGasto = historial.groupBy { lista ->
            Calendar.getInstance().apply { timeInMillis = lista.fecha }.get(Calendar.DAY_OF_WEEK)
        }.maxByOrNull { (_, listas) ->
            listas.sumOf { lista -> lista.productos.sumOf { it.precio * it.cantidad } }
        }?.key

        val diaMaximoGasto = diaMayorGasto?.let {
            Calendar.getInstance().getDisplayName(it, Calendar.LONG, Locale.forLanguageTag("es-ES"))
        } ?: "-"

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Gastas más en: $categoriaMasGasto")
            Text("Gastas menos en: $categoriaMenosGasto")
            Text("Media semanal: ${"%.2f".format(gastoTotal / semanasContadas)} €")
            Text("Día de más gasto: $diaMaximoGasto")
        }
    }
}

private fun Calendar.medianoche() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
