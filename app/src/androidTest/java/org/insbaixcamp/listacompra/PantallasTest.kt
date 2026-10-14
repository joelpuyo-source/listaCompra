package org.insbaixcamp.listacompra

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Comprobaciones basicas de las pantallas, sin necesitar iniciar sesion.
@RunWith(AndroidJUnit4::class)
class PantallasTest {
    @get:Rule val compose = createComposeRule()

    @Test fun graficaCambiaEntreSemanaYMes() {
        val periodo = mutableStateOf("Semana")
        compose.setContent { PantallaGrafica(emptyList(), periodo.value) { periodo.value = it } }
        compose.onNodeWithText("Gasto semana (€)").assertIsDisplayed()
        compose.onNodeWithText("Mensual").performClick()
        compose.onNodeWithText("Gasto mes (€)").assertIsDisplayed()
        compose.onNodeWithText("Total del periodo: 0.00 €").assertIsDisplayed()
    }

    @Test fun historialMuestraCompraGuardada() {
        val compra = ListaGuardada(0, listOf(Compra("Leche", "Lácteos", 1.5)), "Compra de prueba")
        compose.setContent { PantallaHistorial(listOf(compra), false, "") }
        compose.onNodeWithText("Compra de prueba").assertIsDisplayed()
        compose.onNodeWithText("Leche × 1 — 1.5 €").assertIsDisplayed()
    }
}
