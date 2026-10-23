package org.insbaixcamp.listacompra

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PublicacionLista(
    val id: String = "",
    val nombre: String = "",
    val autorUid: String = "",
    val autorNombre: String = "",
    val autorHandle: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val productos: List<Map<String, Any>> = emptyList(),
    val likes: List<String> = emptyList(),
    val comentarios: List<Map<String, Any>> = emptyList()
)

@Composable
fun ComunidadScreen(
    auth: FirebaseAuth,
    onGuardarListaEnActivas: (String, List<Compra>) -> Unit
) {
    val contexto = LocalContext.current
    val usuarioActual = auth.currentUser
    val uidActual = usuarioActual?.uid ?: ""

    var publicaciones by remember { mutableStateOf<List<PublicacionLista>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var listaComentandoId by remember { mutableStateOf<String?>(null) }
    var textoComentario by remember { mutableStateOf("") }
    val ambito = rememberCoroutineScope()

    fun cargarPublicaciones() {
        ambito.launch {
            try {
                cargando = true
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("community_lists")
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .get()
                    .await()

                publicaciones = snapshot.documents.mapNotNull { doc ->
                    val pub = doc.toObject(PublicacionLista::class.java)
                    pub?.copy(id = doc.id)
                }
                cargando = false
            } catch (e: Exception) {
                cargando = false
                Toast.makeText(contexto, "Error al cargar comunidad: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        cargarPublicaciones()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Comunidad - Listas Públicas", style = MaterialTheme.typography.titleLarge)

        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        if (publicaciones.isEmpty() && !cargando) {
            Text("No hay listas publicadas todavía. ¡Sé el primero en compartir una desde tus listas!")
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(publicaciones, key = { it.id }) { pub ->
                val yaDioLike = pub.likes.contains(uidActual)
                val numeroLikes = pub.likes.size
                val handleMostrado = if (pub.autorHandle.isNotBlank()) "@${pub.autorHandle}" else "@usuario"

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pub.nombre, style = MaterialTheme.typography.titleMedium)
                            Text("Por: $handleMostrado", style = MaterialTheme.typography.bodySmall)
                        }

                        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-ES")).format(Date(pub.fecha))
                        Text(fechaFormateada, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(4.dp))

                        val comprasPub = pub.productos.map { map ->
                            Compra(
                                nombre = map["nombre"]?.toString() ?: "",
                                categoria = map["categoria"]?.toString() ?: "Otros",
                                precio = (map["precio"] as? Number)?.toDouble() ?: 0.0,
                                cantidad = (map["cantidad"] as? Number)?.toInt() ?: 1
                            )
                        }

                        comprasPub.take(4).forEach { p ->
                            Text("• ${p.nombre} × ${p.cantidad} (${"%.2f".format(p.precio * p.cantidad)} €)", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (comprasPub.size > 4) {
                            Text("...y ${comprasPub.size - 4} productos más", style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (uidActual.isEmpty()) return@TextButton
                                    ambito.launch {
                                        try {
                                            val docRef = FirebaseFirestore.getInstance().collection("community_lists").document(pub.id)
                                            val nuevosLikes = if (yaDioLike) {
                                                pub.likes - uidActual
                                            } else {
                                                pub.likes + uidActual
                                            }
                                            docRef.update("likes", nuevosLikes).await()
                                            cargarPublicaciones()
                                        } catch (e: Exception) {
                                            Toast.makeText(contexto, "Error al dar like: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Text(if (yaDioLike) "❤️ $numeroLikes" else "🤍 $numeroLikes")
                            }

                            TextButton(onClick = { listaComentandoId = pub.id }) {
                                Text("💬 (${pub.comentarios.size})")
                            }

                            Button(
                                onClick = {
                                    onGuardarListaEnActivas(pub.nombre, comprasPub)
                                    Toast.makeText(contexto, "¡Lista guardada en tus listas activas!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Guardar")
                            }
                        }

                        if (listaComentandoId == pub.id) {
                            HorizontalDivider()
                            Text("Comentarios:", style = MaterialTheme.typography.titleSmall)
                            pub.comentarios.forEach { com ->
                                val autor = com["autorHandle"]?.toString()?.let { "@$it" } ?: com["autorNombre"]?.toString() ?: "@usuario"
                                val texto = com["texto"]?.toString() ?: ""
                                Text("• $autor: $texto", style = MaterialTheme.typography.bodySmall)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = textoComentario,
                                    onValueChange = { textoComentario = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Escribe un comentario...") },
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        if (textoComentario.isBlank() || uidActual.isEmpty()) return@Button
                                        ambito.launch {
                                            try {
                                                val userDoc = FirebaseFirestore.getInstance().collection("users").document(uidActual).get().await()
                                                val handleUser = userDoc.getString("handle") ?: "usuario"

                                                val nuevoComentario = mapOf(
                                                    "autorNombre" to (usuarioActual?.displayName ?: "Usuario"),
                                                    "autorHandle" to handleUser,
                                                    "texto" to textoComentario.trim(),
                                                    "fecha" to System.currentTimeMillis()
                                                )
                                                val docRef = FirebaseFirestore.getInstance().collection("community_lists").document(pub.id)
                                                val listaComentariosActualizada = pub.comentarios + nuevoComentario
                                                docRef.update("comentarios", listaComentariosActualizada).await()
                                                textoComentario = ""
                                                cargarPublicaciones()
                                            } catch (e: Exception) {
                                                Toast.makeText(contexto, "Error al comentar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Text("Enviar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
