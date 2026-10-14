package org.insbaixcamp.listacompra

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

// Datos sencillos que usamos en la aplicacion.
data class Compra(val nombre: String, val categoria: String, val precio: Double, val cantidad: Int = 1, val marcada: Boolean = false)
data class ListaGuardada(val fecha: Long, val productos: List<Compra>, val nombre: String = "Compra")
data class ListaUsuario(val nombre: String, val productos: List<Compra> = emptyList(), val creada: Long = System.currentTimeMillis())

fun guardarListas(contexto: Context, listas: List<ListaUsuario>) {
    contexto.getSharedPreferences("app", 0).edit { putString("listas", JSONArray().also { a ->
        listas.forEach { l -> a.put(JSONObject().put("nombreLista", l.nombre).put("creada", l.creada).put("productos", JSONArray().also { p ->
            l.productos.forEach { p.put(JSONObject().put("nombre", it.nombre).put("categoria", it.categoria).put("precio", it.precio).put("cantidad", it.cantidad).put("marcada", it.marcada)) }
        })) }
    }.toString()) }
}

fun leerListas(contexto: Context): List<ListaUsuario> = try {
    val a = JSONArray(contexto.getSharedPreferences("app", 0).getString("listas", "[]"))
    List(a.length()) { i -> a.getJSONObject(i).let { l ->
        val p = l.getJSONArray("productos")
        ListaUsuario(l.getString("nombreLista"), List(p.length()) { n -> p.getJSONObject(n).let { Compra(it.getString("nombre"), it.getString("categoria"), it.getDouble("precio"), it.getInt("cantidad"), it.optBoolean("marcada")) } }, l.optLong("creada", System.currentTimeMillis()))
    } }
} catch (_: Exception) { emptyList() }

fun guardarHistorial(contexto: Context, historial: List<ListaGuardada>) {
    val json = JSONArray()
    historial.forEach { l -> json.put(JSONObject().put("nombre", l.nombre).put("fecha", l.fecha).put("productos", JSONArray().also { a -> l.productos.forEach { a.put(JSONObject().put("nombre", it.nombre).put("categoria", it.categoria).put("precio", it.precio).put("cantidad", it.cantidad)) } })) }
    contexto.getSharedPreferences("app", 0).edit { putString("historial", json.toString()) }
}

fun leerHistorial(contexto: Context): List<ListaGuardada> = try {
    val json = JSONArray(contexto.getSharedPreferences("app", 0).getString("historial", "[]"))
    List(json.length()) { i -> json.getJSONObject(i).let { l ->
        val productos = l.getJSONArray("productos")
        ListaGuardada(l.getLong("fecha"), List(productos.length()) { n -> productos.getJSONObject(n).let { Compra(it.getString("nombre"), it.getString("categoria"), it.getDouble("precio"), it.getInt("cantidad")) } }, l.optString("nombre", "Compra"))
    } }
} catch (_: Exception) { emptyList() }

fun archivarListasCaducadas(contexto: Context) {
    val ahora = System.currentTimeMillis()
    val listas = leerListas(contexto)
    val caducadas = listas.filter { ahora - it.creada >= 72 * 60 * 60 * 1000L }
    if (caducadas.isNotEmpty()) {
        guardarHistorial(contexto, leerHistorial(contexto) + caducadas.map { ListaGuardada(it.creada, it.productos, it.nombre) })
        guardarListas(contexto, listas - caducadas.toSet())
    }
}
