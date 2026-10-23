package org.insbaixcamp.listacompra

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

data class Compra(
    val nombre: String,
    val categoria: String,
    val precio: Double,
    val cantidad: Int = 1,
    val marcada: Boolean = false
)

data class ListaGuardada(
    val fecha: Long,
    val productos: List<Compra>,
    val nombre: String = "Compra"
)

data class ListaUsuario(
    val nombre: String,
    val productos: List<Compra> = emptyList(),
    val creada: Long = System.currentTimeMillis()
)

fun guardarListas(contexto: Context, listas: List<ListaUsuario>) {
    val arregloJson = JSONArray()

    listas.forEach { lista ->
        val objetoLista = JSONObject().apply {
            put("nombreLista", lista.nombre)
            put("creada", lista.creada)
            put("productos", JSONArray().apply {
                lista.productos.forEach { producto ->
                    put(JSONObject().apply {
                        put("nombre", producto.nombre)
                        put("categoria", producto.categoria)
                        put("precio", producto.precio)
                        put("cantidad", producto.cantidad)
                        put("marcada", producto.marcada)
                    })
                }
            })
        }
        arregloJson.put(objetoLista)
    }

    contexto.getSharedPreferences("app", Context.MODE_PRIVATE).edit {
        putString("listas", arregloJson.toString())
    }
}

fun leerListas(contexto: Context): List<ListaUsuario> = try {
    val cadenaJson = contexto.getSharedPreferences("app", Context.MODE_PRIVATE).getString("listas", "[]")
    val arregloJson = JSONArray(cadenaJson)

    List(arregloJson.length()) { indice ->
        val objetoJson = arregloJson.getJSONObject(indice)
        val arregloProductos = objetoJson.getJSONArray("productos")

        val productos = List(arregloProductos.length()) { indiceProducto ->
            val objetoProducto = arregloProductos.getJSONObject(indiceProducto)
            Compra(
                nombre = objetoProducto.getString("nombre"),
                categoria = objetoProducto.getString("categoria"),
                precio = objetoProducto.getDouble("precio"),
                cantidad = objetoProducto.getInt("cantidad"),
                marcada = objetoProducto.optBoolean("marcada", false)
            )
        }

        ListaUsuario(
            nombre = objetoJson.getString("nombreLista"),
            productos = productos,
            creada = objetoJson.optLong("creada", System.currentTimeMillis())
        )
    }
} catch (_: Exception) {
    emptyList()
}

fun guardarHistorial(contexto: Context, historial: List<ListaGuardada>) {
    val arregloJson = JSONArray()

    historial.forEach { lista ->
        val objetoLista = JSONObject().apply {
            put("nombre", lista.nombre)
            put("fecha", lista.fecha)
            put("productos", JSONArray().apply {
                lista.productos.forEach { producto ->
                    put(JSONObject().apply {
                        put("nombre", producto.nombre)
                        put("categoria", producto.categoria)
                        put("precio", producto.precio)
                        put("cantidad", producto.cantidad)
                    })
                }
            })
        }
        arregloJson.put(objetoLista)
    }

    contexto.getSharedPreferences("app", Context.MODE_PRIVATE).edit {
        putString("historial", arregloJson.toString())
    }
}

fun leerHistorial(contexto: Context): List<ListaGuardada> = try {
    val cadenaJson = contexto.getSharedPreferences("app", Context.MODE_PRIVATE).getString("historial", "[]")
    val arregloJson = JSONArray(cadenaJson)

    List(arregloJson.length()) { indice ->
        val objetoJson = arregloJson.getJSONObject(indice)
        val arregloProductos = objetoJson.getJSONArray("productos")

        val productos = List(arregloProductos.length()) { indiceProducto ->
            val objetoProducto = arregloProductos.getJSONObject(indiceProducto)
            Compra(
                nombre = objetoProducto.getString("nombre"),
                categoria = objetoProducto.getString("categoria"),
                precio = objetoProducto.getDouble("precio"),
                cantidad = objetoProducto.getInt("cantidad")
            )
        }

        ListaGuardada(
            fecha = objetoJson.getLong("fecha"),
            productos = productos,
            nombre = objetoJson.optString("nombre", "Compra")
        )
    }
} catch (_: Exception) {
    emptyList()
}

fun archivarListasCaducadas(contexto: Context) {
    val tiempoActual = System.currentTimeMillis()
    val listas = leerListas(contexto)
    val caducadas = listas.filter { tiempoActual - it.creada >= 72 * 60 * 60 * 1000L }

    if (caducadas.isNotEmpty()) {
        val historialActual = leerHistorial(contexto)
        val nuevasGuardadas = caducadas.map {
            ListaGuardada(fecha = it.creada, productos = it.productos, nombre = it.nombre)
        }
        guardarHistorial(contexto, nuevasGuardadas + historialActual)
        guardarListas(contexto, listas - caducadas.toSet())
    }
}
