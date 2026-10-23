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
        
        val autenticacion = FirebaseAuth.getInstance()

        setContent {
            ListacompraTheme {
                var usuarioActual by remember { mutableStateOf(autenticacion.currentUser) }

                DisposableEffect(autenticacion) {
                    val oyente = FirebaseAuth.AuthStateListener { 
                        usuarioActual = it.currentUser 
                    }
                    autenticacion.addAuthStateListener(oyente)
                    onDispose { 
                        autenticacion.removeAuthStateListener(oyente) 
                    }
                }

                if (usuarioActual == null) {
                    LoginScreen(autenticacion)
                } else {
                    CompraApp(autenticacion)
                }
            }
        }
    }
}
