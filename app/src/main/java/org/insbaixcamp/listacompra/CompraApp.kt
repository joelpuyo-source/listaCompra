package org.insbaixcamp.listacompra

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import org.insbaixcamp.listacompra.ui.theme.ListacompraTheme

// Pantalla principal: controla listas, busqueda, historial y grafica.

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CompraApp(auth: FirebaseAuth) {
    val activity = LocalActivity.current
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
    fun actualizarLista(productos: List<Compra>) {
        listas = listas.toMutableList().also { it[listaActiva] = it[listaActiva].copy(productos = productos) }
    }
    LaunchedEffect(historial) { guardarHistorial(contexto, historial) }
    LaunchedEffect(listas) { guardarListas(contexto, listas) }
    LaunchedEffect(listas) {
        val vencimiento = listas.minOfOrNull { it.creada + 72 * 60 * 60 * 1000L }
        if (vencimiento != null) {
            delay((vencimiento - System.currentTimeMillis()).coerceAtLeast(0))
            val caducadas = listas.filter { System.currentTimeMillis() - it.creada >= 72 * 60 * 60 * 1000L }
            if (caducadas.isNotEmpty()) {
                historial = historial + caducadas.map { ListaGuardada(it.creada, it.productos, it.nombre) }
                listas = listas - caducadas.toSet()
                listaAbierta = false
                listaActiva = 0
            }
        }
    }
    LaunchedEffect(Unit) { ProductosFirebase.cargar({ productos = it }, { error = it.localizedMessage ?: "Error al leer Firebase" }) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val nombreUsuario = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: auth.currentUser?.email?.substringBefore("@") ?: "usuario"
    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text("Bienvenido/a", style = MaterialTheme.typography.titleMedium)
                Text(nombreUsuario, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            listOf("Listas", "Historial", "Gráfica").forEach { opcion ->
                NavigationDrawerItem(label = { Text(opcion) }, selected = pantalla == opcion,
                    onClick = { pantalla = opcion; scope.launch { drawerState.close() }
                        if (opcion == "Historial") {
                            val preferencias = contexto.getSharedPreferences("app", 0)
                            val primeraVisita = !preferencias.getBoolean("historialVisitado", false)
                            preferencias.edit { putBoolean("historialVisitado", true) }
                            if ((primeraVisita || Random.nextInt(10) == 0) && activity != null) {
                                cargando = true
                                RewardedAd.load(activity, "ca-app-pub-3940256099942544/5224354917", AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                                    override fun onAdLoaded(ad: RewardedAd) { cargando = false; ad.show(activity) { } }
                                    override fun onAdFailedToLoad(errorAd: LoadAdError) { cargando = false; error = "No se pudo cargar el anuncio: ${errorAd.message}" }
                                })
                            }
                        }
                    }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
            }
            NavigationDrawerItem(label = { Text("Cerrar sesión") }, selected = false, onClick = { auth.signOut(); scope.launch { drawerState.close() } },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
        }
    }, gesturesEnabled = true) {
    if (mostrarNuevaLista) AlertDialog(
        onDismissRequest = { mostrarNuevaLista = false },
        title = { Text("Nueva lista") },
        text = { OutlinedTextField(nombreLista, { nombreLista = it }, label = { Text("Nombre") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            if (nombreLista.isNotBlank()) { listas = listas + ListaUsuario(nombreLista.trim()); listaActiva = listas.lastIndex; nombreLista = ""; busqueda = ""; listaAbierta = false }
            mostrarNuevaLista = false
        }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = { mostrarNuevaLista = false }) { Text("Cancelar") } }
    )
    if (listaParaEliminar >= 0) AlertDialog(
        onDismissRequest = { listaParaEliminar = -1 },
        title = { Text("Eliminar lista") },
        text = { Text("¿Eliminar «${listas[listaParaEliminar].nombre}»? No se guardará en el historial.") },
        confirmButton = { TextButton(onClick = {
            listas = listas.filterIndexed { i, _ -> i != listaParaEliminar }
            listaActiva = 0
            listaAbierta = false
            listaParaEliminar = -1
        }) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { listaParaEliminar = -1 }) { Text("Cancelar") } }
    )
    Scaffold(topBar = { TopAppBar(title = { Text(pantalla) }, navigationIcon = {
        IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Abrir menú") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (pantalla == "Listas") {
                if (!listaAbierta) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Listas activas", style = MaterialTheme.typography.titleLarge)
                        Button(onClick = { mostrarNuevaLista = true }) { Text("+ Nueva lista") }
                    }
                    LazyColumn(Modifier.weight(1f)) {
                        items(listas.mapIndexed { i, l -> i to l }, key = { it.second.creada }) { (i, l) ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f).clickable { listaActiva = i; listaAbierta = true; busqueda = "" }) {
                                        Text(l.nombre, style = MaterialTheme.typography.titleMedium)
                                        Text("${l.productos.size} productos · Aprox. ${"%.2f".format(l.productos.sumOf { it.precio * it.cantidad })} €")
                                    }
                                    TextButton(onClick = { listaParaEliminar = i }) { Text("🗑️ Eliminar") }
                                }
                            }
                        }
                        if (listas.isEmpty()) item { Text("Aún no tienes listas activas. Crea una para empezar.", Modifier.padding(12.dp)) }
                    }
                } else {
                TextButton(onClick = { listaAbierta = false; busqueda = "" }) { Text("‹ Todas las listas") }
                Text(listas[listaActiva].nombre, style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(busqueda, { busqueda = it }, Modifier.fillMaxWidth(), label = { Text("Buscar producto (ej. pollo)") }, singleLine = true)
                if (productos.isEmpty() && error.isNotEmpty()) Text(error)
                val resultados = productos.filter { busqueda.isNotBlank() && (it.nombre.contains(busqueda, true) || it.categoria.contains(busqueda, true)) }.take(80)
                LazyColumn(Modifier.weight(1f)) {
                    if (busqueda.isBlank()) {
                        items(lista) { item ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(checked = item.marcada, onCheckedChange = { marcada -> actualizarLista(lista.map { if (it.nombre == item.nombre) it.copy(marcada = marcada) else it }) })
                                Text("${item.nombre} · Aprox. ${"%.2f".format(item.precio)} €", Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                TextButton(onClick = { actualizarLista(lista.map { if (it.nombre == item.nombre) it.copy(cantidad = it.cantidad + 1) else it }) }) { Text("+") }
                                Text("${item.cantidad}")
                                TextButton(onClick = { actualizarLista(lista.mapNotNull { if (it.nombre != item.nombre) it else if (it.cantidad > 1) it.copy(cantidad = it.cantidad - 1) else null }) }) { Text("−") }
                            }
                        }
                        if (lista.isEmpty()) item { Text("Busca un producto y añádelo a tu lista.", Modifier.padding(12.dp)) }
                    } else {
                        items(resultados) { p ->
                            val yaEsta = lista.any { it.nombre == p.nombre }
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Checkbox(checked = yaEsta, onCheckedChange = { añadir ->
                                        actualizarLista(if (añadir) lista + Compra(p.nombre, p.categoria, p.precio) else lista.filterNot { it.nombre == p.nombre })
                                    })
                                    Column(Modifier.weight(1f)) {
                                        Text(p.nombre, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                                        Text("${p.categoria} · Aprox. ${"%.2f".format(p.precio)} €", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        if (resultados.isEmpty()) item { Text("No hay productos que coincidan.", Modifier.padding(12.dp)) }
                    }
                }
                Text("Total aprox.: ${"%.2f".format(lista.sumOf { it.precio * it.cantidad })} €", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { if (lista.isNotEmpty()) historial = historial + ListaGuardada(System.currentTimeMillis(), lista, listas[listaActiva].nombre); actualizarLista(emptyList()); listaAbierta = false }) { Text("Guardar compra") }
                }
            } else if (pantalla == "Historial") {
                PantallaHistorial(historial, cargando, error)
            } else if (pantalla == "Gráfica") {
                PantallaGrafica(historial, periodo) { periodo = it }
            }
        }
    }
    }
}
