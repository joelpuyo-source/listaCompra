package org.insbaixcamp.listacompra

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(autenticacion: FirebaseAuth) {
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    val contexto = LocalContext.current

    val clienteInicioSesionGoogle = remember {
        val opciones = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(contexto.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(contexto, opciones)
    }

    val lanzador = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val tarea = GoogleSignIn.getSignedInAccountFromIntent(resultado.data)
        try {
            val cuenta = tarea.getResult(Exception::class.java)
            val tokenIdentidad = cuenta?.idToken
            if (tokenIdentidad != null) {
                cargando = true
                val credencial = GoogleAuthProvider.getCredential(tokenIdentidad, null)
                autenticacion.signInWithCredential(credencial)
                    .addOnCompleteListener { tareaAuth ->
                        cargando = false
                        if (tareaAuth.isSuccessful) {
                            Toast.makeText(contexto, "¡Sesión iniciada con Google!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(contexto, "Error Firebase: ${tareaAuth.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(contexto, "Error: No se pudo obtener el token de Google", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(contexto, "Acceso con Google cancelado o fallido: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Acceso de Usuario",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (cargando) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (correo.isBlank() || contrasena.isBlank()) {
                        Toast.makeText(contexto, "Introduce correo y contraseña", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    cargando = true
                    autenticacion.signInWithEmailAndPassword(correo, contrasena)
                        .addOnCompleteListener { tarea ->
                            cargando = false
                            if (tarea.isSuccessful) {
                                Toast.makeText(contexto, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                            } else {
                                autenticacion.createUserWithEmailAndPassword(correo, contrasena)
                                    .addOnCompleteListener { tareaRegistro ->
                                        if (tareaRegistro.isSuccessful) {
                                            Toast.makeText(contexto, "Cuenta creada y conectada", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(contexto, "Error: ${tareaRegistro.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar Sesión / Registrarse con Correo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    clienteInicioSesionGoogle.signOut().addOnCompleteListener {
                        val intencionInicioSesion = clienteInicioSesionGoogle.signInIntent
                        lanzador.launch(intencionInicioSesion)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar sesión con Google")
            }
        }
    }
}
