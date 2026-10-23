package org.insbaixcamp.listacompra

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CompraApp(autenticacion: FirebaseAuth) {
    val actividad = LocalActivity.current
    val contexto = LocalContext.current

    var productos by remember { mutableStateOf<List<Producto>>(emptyList()) }
    var listas by remember { mutableStateOf(archivarListasCaducadas(contexto).let { leerListas(contexto) }) }
    var listaActiva by remember { mutableIntStateOf(0) }
    var listaAbierta by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var mostrarNuevaLista by remember { mutableStateOf(false) }
    var nombreLista by remember { mutableStateOf("") }
    var listaParaEliminar by remember { mutableIntStateOf(-1) }
    var historial by remember { mutableStateOf(leerHistorial(contexto)) }
    var pantalla by remember { mutableStateOf("Listas") }
    var periodo by remember { mutableStateOf("Semana") }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val lista = listas.getOrNull(listaActiva)?.productos.orEmpty()

    fun actualizarLista(productosActualizados: List<Compra>) {
        listas = listas.toMutableList().also {
            it[listaActiva] = it[listaActiva].copy(productos = productosActualizados)
        }
    }

    LaunchedEffect(historial) {
        guardarHistorial(contexto, historial)
    }

    LaunchedEffect(listas) {
        guardarListas(contexto, listas)
    }

    LaunchedEffect(listas) {
        val tiempoVencimiento = listas.minOfOrNull { it.creada + 72 * 60 * 60 * 1000L }
        if (tiempoVencimiento != null) {
            delay((tiempoVencimiento - System.currentTimeMillis()).coerceAtLeast(0))
            val caducadas = listas.filter { System.currentTimeMillis() - it.creada >= 72 * 60 * 60 * 1000L }
            if (caducadas.isNotEmpty()) {
                historial = caducadas.map { ListaGuardada(it.creada, it.productos, it.nombre) } + historial
                listas = listas - caducadas.toSet()
                listaAbierta = false
                listaActiva = 0
            }
        }
    }

    LaunchedEffect(Unit) {
        ProductosFirebase.cargar(
            ok = { productos = it },
            error = { error = it.localizedMessage ?: "Error al leer Firebase" }
        )
    }

    val estadoMenuLateral = rememberDrawerState(DrawerValue.Closed)
    val ambito = rememberCoroutineScope()
    val nombreUsuario = autenticacion.currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: autenticacion.currentUser?.email?.substringBefore("@") ?: "usuario"

    ModalNavigationDrawer(
        drawerState = estadoMenuLateral,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text("Bienvenido/a", style = MaterialTheme.typography.titleMedium)
                    Text(nombreUsuario, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                NavigationDrawerItem(
                    label = { Text("✏️ Editar perfil") },
                    selected = pantalla == "Editar perfil",
                    onClick = {
                        pantalla = "Editar perfil"
                        ambito.launch { estadoMenuLateral.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val opcionesConEmojis = mapOf(
                    "Listas" to "🛒 Listas",
                    "Historial" to "📜 Historial",
                    "Gráfica" to "📊 Gráfica",
                    "Comunidad" to "🌍 Comunidad"
                )

                opcionesConEmojis.forEach { (opcion, etiqueta) ->
                    NavigationDrawerItem(
                        label = { Text(etiqueta) },
                        selected = pantalla == opcion,
                        onClick = {
                            pantalla = opcion
                            ambito.launch { estadoMenuLateral.close() }

                            if (opcion == "Historial") {
                                val preferencias = contexto.getSharedPreferences("app", 0)
                                val primeraVisita = !preferencias.getBoolean("historialVisitado", false)
                                preferencias.edit { putBoolean("historialVisitado", true) }

                                if ((primeraVisita || Random.nextInt(10) == 0) && actividad != null) {
                                    cargando = true
                                    RewardedAd.load(
                                        actividad,
                                        "ca-app-pub-3940256099942544/5224354917",
                                        AdRequest.Builder().build(),
                                        object : RewardedAdLoadCallback() {
                                            override fun onAdLoaded(anuncioPremio: RewardedAd) {
                                                cargando = false
                                                anuncioPremio.show(actividad) { }
                                            }
                                            override fun onAdFailedToLoad(errorAnuncio: LoadAdError) {
                                                cargando = false
                                                error = "No se pudo cargar el anuncio: ${errorAnuncio.message}"
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    label = { Text("🚪 Cerrar sesión") },
                    selected = false,
                    onClick = {
                        autenticacion.signOut()
                        ambito.launch { estadoMenuLateral.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        },
        gesturesEnabled = true
    ) {
        if (mostrarNuevaLista) {
            AlertDialog(
                onDismissRequest = { mostrarNuevaLista = false },
                title = { Text("Nueva lista") },
                text = {
                    OutlinedTextField(
                        value = nombreLista,
                        onValueChange = { nombreLista = it },
                        label = { Text("Nombre") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (nombreLista.isNotBlank()) {
                                listas = listas + ListaUsuario(nombreLista.trim())
                                listaActiva = listas.lastIndex
                                nombreLista = ""
                                busqueda = ""
                                listaAbierta = false
                            }
                            mostrarNuevaLista = false
                        }
                    ) {
                        Text("Crear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarNuevaLista = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (listaParaEliminar >= 0) {
            val listaAEliminar = listas[listaParaEliminar]
            AlertDialog(
                onDismissRequest = { listaParaEliminar = -1 },
                title = { Text("Eliminar lista") },
                text = { Text("¿Eliminar «${listaAEliminar.nombre}»? Se guardará en el historial con sus productos.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (listaAEliminar.productos.isNotEmpty()) {
                                historial = listOf(
                                    ListaGuardada(
                                        fecha = System.currentTimeMillis(),
                                        productos = listaAEliminar.productos,
                                        nombre = listaAEliminar.nombre
                                    )
                                ) + historial
                            }
                            listas = listas.filterIndexed { i, _ -> i != listaParaEliminar }
                            listaActiva = 0
                            listaAbierta = false
                            listaParaEliminar = -1
                        }
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { listaParaEliminar = -1 }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(pantalla) },
                    navigationIcon = {
                        IconButton(onClick = { ambito.launch { estadoMenuLateral.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pantalla == "Listas") {
                    if (!listaAbierta) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Listas activas", style = MaterialTheme.typography.titleLarge)
                            Button(onClick = { mostrarNuevaLista = true }) {
                                Text("+ Nueva lista")
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listas.mapIndexed { i, l -> i to l }, key = { it.second.creada }) { (i, l) ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    listaActiva = i
                                                    listaAbierta = true
                                                    busqueda = ""
                                                }
                                        ) {
                                            Text(l.nombre, style = MaterialTheme.typography.titleMedium)
                                            val totalAprox = l.productos.sumOf { it.precio * it.cantidad }
                                            Text("${l.productos.size} productos · Aprox. ${"%.2f".format(totalAprox)} €")
                                        }
                                        TextButton(onClick = { listaParaEliminar = i }) {
                                            Text("🗑️")
                                        }
                                    }
                                }
                            }
                            if (listas.isEmpty()) {
                                item {
                                    Text("Aún no tienes listas activas. Crea una para empezar.")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Los precios establecidos son una aproximación, pueden fluctuar con el tiempo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Button(onClick = { listaAbierta = false; busqueda = "" }) {
                            Text(
                                text = "<",
                                fontSize = 20.sp
                            )
                        }

                        Text(listas[listaActiva].nombre, style = MaterialTheme.typography.titleLarge)

                        OutlinedTextField(
                            value = busqueda,
                            onValueChange = { busqueda = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Buscar producto (ej. pollo)") },
                            singleLine = true
                        )

                        if (productos.isEmpty() && error.isNotEmpty()) {
                            Text(text = error, color = MaterialTheme.colorScheme.error)
                        }

                        val productosFiltrados = productos.filter {
                            busqueda.isNotBlank() && (it.nombre.contains(busqueda, true) || it.categoria.contains(busqueda, true))
                        }.take(80)

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (busqueda.isBlank()) {
                                items(lista) { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = item.marcada,
                                            onCheckedChange = { marcada ->
                                                actualizarLista(lista.map { if (it.nombre == item.nombre) it.copy(marcada = marcada) else it })
                                            }
                                        )

                                        val precioSubtotal = item.precio * item.cantidad
                                        Text(
                                            text = "${item.nombre} - ${"%.2f".format(precioSubtotal)} €",
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        TextButton(
                                            onClick = {
                                                actualizarLista(lista.map { if (it.nombre == item.nombre) it.copy(cantidad = it.cantidad + 1) else it })
                                            }
                                        ) {
                                            Text("+")
                                        }

                                        Text("${item.cantidad}")

                                        TextButton(
                                            onClick = {
                                                actualizarLista(
                                                    lista.mapNotNull {
                                                        if (it.nombre != item.nombre) it
                                                        else if (it.cantidad > 1) it.copy(cantidad = it.cantidad - 1)
                                                        else null
                                                    }
                                                )
                                            }
                                        ) {
                                            Text("−")
                                        }
                                    }
                                }
                                if (lista.isEmpty()) {
                                    item {
                                        Text("Busca un producto y añádelo a tu lista.")
                                    }
                                }
                            } else {
                                items(productosFiltrados) { producto ->
                                    val estaEnLista = lista.any { it.nombre == producto.nombre }
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Checkbox(
                                                checked = estaEnLista,
                                                onCheckedChange = { añadir ->
                                                    actualizarLista(
                                                        if (añadir) lista + Compra(producto.nombre, producto.categoria, producto.precio)
                                                        else lista.filterNot { it.nombre == producto.nombre }
                                                    )
                                                }
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(producto.nombre, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                                                Text("${producto.categoria} - ${"%.2f".format(producto.precio)} €", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                                if (productosFiltrados.isEmpty()) {
                                    item {
                                        Text("No hay productos que coincidan.")
                                    }
                                }
                            }
                        }

                        val precioTotalGeneral = lista.sumOf { it.precio * it.cantidad }
                        Text("Total: ${"%.2f".format(precioTotalGeneral)} €", style = MaterialTheme.typography.titleMedium)

                        Button(
                            onClick = {
                                if (lista.isNotEmpty()) {
                                    ambito.launch {
                                        try {
                                            val uid = autenticacion.currentUser?.uid ?: ""
                                            val userDoc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                                            val handleUser = userDoc.getString("handle") ?: "usuario"

                                            val publicacionMap = mapOf(
                                                "nombre" to listas[listaActiva].nombre,
                                                "autorUid" to uid,
                                                "autorNombre" to (autenticacion.currentUser?.displayName ?: "Usuario"),
                                                "autorHandle" to handleUser,
                                                "fecha" to System.currentTimeMillis(),
                                                "productos" to lista.map { mapOf("nombre" to it.nombre, "categoria" to it.categoria, "precio" to it.precio, "cantidad" to it.cantidad) },
                                                "likes" to emptyList<String>(),
                                                "comentarios" to emptyList<Map<String, Any>>()
                                            )
                                            FirebaseFirestore.getInstance().collection("community_lists").add(publicacionMap).await()
                                            Toast.makeText(contexto, "¡Lista publicada en la comunidad con éxito!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(contexto, "Error al publicar: ${e.localizedMessage}. (Configura tus Reglas de Firestore)", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🌐 Publicar en la Comunidad")
                        }
                    }
                } else if (pantalla == "Historial") {
                    PantallaHistorial(historial, cargando, error)
                } else if (pantalla == "Gráfica") {
                    PantallaGrafica(historial, periodo) { periodo = it }
                } else if (pantalla == "Comunidad") {
                    ComunidadScreen(autenticacion) { nombreListaNueva, productosNuevos ->
                        listas = listas + ListaUsuario(nombre = nombreListaNueva, productos = productosNuevos)
                    }
                } else if (pantalla == "Editar perfil") {
                    PerfilScreen(autenticacion)
                }
            }
        }
    }
}
