package org.insbaixcamp.listacompra

import com.google.firebase.firestore.FirebaseFirestore

data class Producto(val nombre: String, val categoria: String, val precio: Double)

object ProductosFirebase {
    private fun categoria(dato: Map<*, *>): String =
        ((dato["categories"] as? List<*>)?.firstOrNull() as? Map<*, *>)?.let { categoria(it) }
            ?: dato["name"]?.toString().orEmpty()

    fun cargar(ok: (List<Producto>) -> Unit, error: (Exception) -> Unit) {
        FirebaseFirestore.getInstance().collection("products").get()
            .addOnSuccessListener { docs ->
                FirebaseFirestore.getInstance().collection("categories").get()
                    .addOnSuccessListener { categorias ->
                        val nombres = categorias.associate { it.id to it.getString("name").orEmpty() }
                        ok(docs.mapNotNull { d ->
                            val precios = d.get("price_instructions") as? Map<*, *> ?: emptyMap<Any, Any>()
                            val precio = (precios["bulk_price"] ?: precios["unit_price"])?.toString()?.toDoubleOrNull() ?: return@mapNotNull null
                            val categoria = (d.get("categories") as? List<*>)?.firstOrNull() as? Map<*, *>
                            Producto(d.getString("display_name") ?: d.getString("name") ?: d.id,
                                categoria?.let { categoria(it) }?.ifBlank { null } ?: "Otros", precio)
                        })
                    }.addOnFailureListener(error)
            }.addOnFailureListener(error)
    }
}
