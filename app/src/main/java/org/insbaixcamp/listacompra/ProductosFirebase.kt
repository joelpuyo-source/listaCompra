package org.insbaixcamp.listacompra

import com.google.firebase.firestore.FirebaseFirestore

data class Producto(
    val nombre: String,
    val categoria: String,
    val precio: Double
)

object ProductosFirebase {

    private fun extraerCategoria(dato: Map<*, *>): String {
        val mapaCategoria = (dato["categories"] as? List<*>)?.firstOrNull() as? Map<*, *>
        return mapaCategoria?.let { extraerCategoria(it) } ?: dato["name"]?.toString().orEmpty()
    }

    fun cargar(ok: (List<Producto>) -> Unit, error: (Exception) -> Unit) {
        val baseDatos = FirebaseFirestore.getInstance()

        baseDatos.collection("products").get()
            .addOnSuccessListener { documentosProductos ->
                baseDatos.collection("categories").get()
                    .addOnSuccessListener { categorias ->
                        val productosCargados = documentosProductos.mapNotNull { documentoProducto ->
                            val datosPrecios = documentoProducto.get("price_instructions") as? Map<*, *> ?: emptyMap<Any, Any>()
                            val precioTexto = datosPrecios["bulk_price"] ?: datosPrecios["unit_price"]
                            val precio = precioTexto?.toString()?.toDoubleOrNull() ?: return@mapNotNull null

                            val mapaCategoria = (documentoProducto.get("categories") as? List<*>)?.firstOrNull() as? Map<*, *>
                            val categoriaNombre = mapaCategoria?.let { extraerCategoria(it) }?.ifBlank { null } ?: "Otros"

                            val productoNombre = documentoProducto.getString("display_name") 
                                ?: documentoProducto.getString("name") 
                                ?: documentoProducto.id

                            Producto(
                                nombre = productoNombre,
                                categoria = categoriaNombre,
                                precio = precio
                            )
                        }
                        ok(productosCargados)
                    }
                    .addOnFailureListener(error)
            }
            .addOnFailureListener(error)
    }
}
