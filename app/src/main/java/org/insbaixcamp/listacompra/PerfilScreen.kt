package org.insbaixcamp.listacompra

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun PerfilScreen(auth: FirebaseAuth) {
    val contexto = LocalContext.current
    val usuarioActual = auth.currentUser
    val uid = usuarioActual?.uid ?: ""

    var nombrePublico by remember { mutableStateOf(usuarioActual?.displayName ?: "") }
    var nombreUsuarioHandle by remember { mutableStateOf("") }
    var handleOriginal by remember { mutableStateOf("") }
    var ultimoCambioHandle by remember { mutableStateOf(0L) }
    var fotoUriStr by remember { mutableStateOf(usuarioActual?.photoUrl?.toString() ?: "") }
    var cargando by remember { mutableStateOf(false) }
    var mostrarDialogoFoto by remember { mutableStateOf(false) }
    val ambito = rememberCoroutineScope()

    val lanzadorGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fotoUriStr = uri.toString()
        }
        mostrarDialogoFoto = false
    }

    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val archivoCache = File(contexto.cacheDir, "perfil_${System.currentTimeMillis()}.jpg")
                val stream = FileOutputStream(archivoCache)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.flush()
                stream.close()
                fotoUriStr = Uri.fromFile(archivoCache).toString()
            } catch (_: Exception) {}
        }
        mostrarDialogoFoto = false
    }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val h = doc.getString("handle") ?: ""
                    nombreUsuarioHandle = h
                    handleOriginal = h
                    ultimoCambioHandle = doc.getLong("ultimoCambioHandle") ?: 0L
                    if (nombrePublico.isBlank()) {
                        nombrePublico = doc.getString("nombre") ?: ""
                    }
                    fotoUriStr = doc.getString("fotoUrl") ?: fotoUriStr
                }
            } catch (_: Exception) {}
        }
    }

    val unMesMs = 30L * 24 * 60 * 60 * 1000L
    val puedeCambiarHandle = handleOriginal.isBlank() || (System.currentTimeMillis() - ultimoCambioHandle >= unMesMs)

    if (mostrarDialogoFoto) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoFoto = false },
            title = { Text("Cambiar foto de perfil") },
            text = { Text("Elige una opción para actualizar tu foto de perfil:") },
            confirmButton = {
                TextButton(onClick = {
                    lanzadorGaleria.launch("image/*")
                }) {
                    Text("Abrir galería")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    lanzadorCamara.launch(null)
                }) {
                    Text("Hacer foto (Cámara)")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Editar Perfil", style = MaterialTheme.typography.headlineMedium)

        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { mostrarDialogoFoto = true },
            contentAlignment = Alignment.Center
        ) {
            if (fotoUriStr.isNotBlank()) {
                AsyncImage(
                    model = fotoUriStr,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar por defecto",
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        OutlinedTextField(
            value = nombrePublico,
            onValueChange = { nombrePublico = it },
            label = { Text("Nombre público") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = nombreUsuarioHandle,
            onValueChange = { if (puedeCambiarHandle) nombreUsuarioHandle = it.trim().removePrefix("@") },
            label = { Text("Nombre de usuario (@usuario)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            prefix = { Text("@") },
            enabled = puedeCambiarHandle,
            supportingText = {
                if (!puedeCambiarHandle) {
                    Text("Solo puedes cambiar tu @usuario una vez al mes.")
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (cargando) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (uid.isEmpty()) return@Button
                    cargando = true
                    ambito.launch {
                        try {
                            val ahora = System.currentTimeMillis()
                            if (nombreUsuarioHandle != handleOriginal) {
                                if (handleOriginal.isNotBlank() && (ahora - ultimoCambioHandle < unMesMs)) {
                                    Toast.makeText(contexto, "Solo puedes cambiar tu @usuario una vez al mes.", Toast.LENGTH_LONG).show()
                                    cargando = false
                                    return@launch
                                }
                            }

                            val profileUpdates = userProfileChangeRequest {
                                displayName = nombrePublico
                            }
                            usuarioActual?.updateProfile(profileUpdates)?.await()

                            val nuevoUltimoCambio = if (nombreUsuarioHandle != handleOriginal) ahora else ultimoCambioHandle

                            val datosUsuario = mapOf(
                                "uid" to uid,
                                "nombre" to nombrePublico,
                                "handle" to nombreUsuarioHandle,
                                "ultimoCambioHandle" to nuevoUltimoCambio,
                                "fotoUrl" to fotoUriStr,
                                "email" to (usuarioActual?.email ?: "")
                            )
                            FirebaseFirestore.getInstance().collection("users").document(uid).set(datosUsuario).await()

                            handleOriginal = nombreUsuarioHandle
                            ultimoCambioHandle = nuevoUltimoCambio

                            cargando = false
                            Toast.makeText(contexto, "¡Perfil guardado con éxito!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            cargando = false
                            val mensaje = if (e.localizedMessage?.contains("PERMISSION_DENIED") == true) {
                                "Error de permisos: Configura las Reglas de Seguridad en tu consola de Firebase Firestore."
                            } else {
                                "Error al guardar: ${e.localizedMessage}"
                            }
                            Toast.makeText(contexto, mensaje, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Cambios")
            }
        }
    }
}
