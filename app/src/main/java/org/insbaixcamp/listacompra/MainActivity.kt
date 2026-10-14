package org.insbaixcamp.listacompra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import org.insbaixcamp.listacompra.ui.theme.ListacompraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        enableEdgeToEdge()
        val auth = FirebaseAuth.getInstance()
        setContent {
            ListacompraTheme {
                var usuario by remember { mutableStateOf(auth.currentUser) }
                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { usuario = it.currentUser }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }
                if (usuario == null) LoginScreen(auth) else CompraApp(auth)
            }
        }
    }
}
