package org.insbaixcamp.listacompra

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import org.insbaixcamp.listacompra.ui.theme.ListacompraTheme

class MainActivity : ComponentActivity() {
    private lateinit var autenticacion: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autenticacion = FirebaseAuth.getInstance()
        enableEdgeToEdge()
        setContent {
            ListacompraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var usuarioActual by remember { mutableStateOf(autenticacion.currentUser) }

                    DisposableEffect(autenticacion) {
                        val oyente = FirebaseAuth.AuthStateListener { authFirebase ->
                            usuarioActual = authFirebase.currentUser
                        }
                        autenticacion.addAuthStateListener(oyente)
                        onDispose {
                            autenticacion.removeAuthStateListener(oyente)
                        }
                    }

                    if (usuarioActual == null) {
                        LoginScreen(autenticacion = autenticacion)
                    } else {
                        ContenidoPrincipalApp(autenticacion = autenticacion)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContenidoPrincipalApp(autenticacion: FirebaseAuth) {
    val estadoMenu = rememberDrawerState(initialValue = DrawerValue.Closed)
    val ambito = rememberCoroutineScope()
    val correoUsuario = autenticacion.currentUser?.email ?: "Usuario"

    ModalNavigationDrawer(
        drawerState = estadoMenu,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = correoUsuario,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                NavigationDrawerItem(
                    label = { Text("Cerrar sesión") },
                    selected = false,
                    onClick = {
                        ambito.launch { estadoMenu.close() }
                        autenticacion.signOut()
                    },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Lista de Compra") },
                    navigationIcon = {
                        IconButton(onClick = {
                            ambito.launch {
                                if (estadoMenu.isClosed) estadoMenu.open() else estadoMenu.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    }
                )
            }
        ) { rellenoInterno ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(rellenoInterno),
                contentAlignment = Alignment.Center
            ) {
                var num by remember { mutableStateOf(1) }
                Button(
                    onClick = {
                        num++,

                    })
                {
                    Text("Generar Lista")
                }
            }
        }
    }
}
