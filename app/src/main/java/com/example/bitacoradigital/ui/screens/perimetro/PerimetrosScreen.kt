package com.example.bitacoradigital.ui.screens.perimetro

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.viewmodel.PerimetroViewModel
import com.example.bitacoradigital.viewmodel.PerimetroViewModelFactory
import com.example.bitacoradigital.model.JerarquiaNodo
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import androidx.navigation.NavHostController

@Composable
fun PerimetrosScreen(perimetroId: Int, permisos: List<String>, navController: NavHostController) {
    val context = LocalContext.current
    val apiService = remember { ApiService.create() }
    val prefs = remember { SessionPreferences(context) }

    val viewModel: PerimetroViewModel = viewModel(
        factory = PerimetroViewModelFactory(apiService, prefs, perimetroId)
    )

    val ruta by viewModel.ruta.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    var nuevoNombre by remember { mutableStateOf("") }
    val puedeCrear = "Crear Perímetro" in permisos
    val puedeEditar = "Editar Perímetro" in permisos
    val puedeEliminar = "Eliminar Perímetro" in permisos
    var editarNodo by remember { mutableStateOf<JerarquiaNodo?>(null) }
    var nombreEditar by remember { mutableStateOf("") }
    var nodoCrearSub by remember { mutableStateOf<JerarquiaNodo?>(null) }
    var nombreSubzona by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.cargarJerarquia() }
    val snackbarHostState = remember { SnackbarHostState() }
    error?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        bottomBar = {
            HomeConfigNavBar(
                current = "",
                onHomeClick = { navController.navigate("home") },
                onConfigClick = { navController.navigate("configuracion") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
    Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        if (cargando) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text("Error: ${'$'}error", color = MaterialTheme.colorScheme.error)
        } else {
            JerarquiaConAcciones(
                ruta = ruta,
                onSeleccion = { viewModel.navegarHacia(it) },
                onRetroceder = { viewModel.retroceder() },
                onEliminar = { viewModel.eliminarPerimetro(it.perimetro_id) },
                onEditar = { editarNodo = it; nombreEditar = it.nombre },
                onCrearSubzona = { nodoCrearSub = it },
                puedeCrear = puedeCrear,
                puedeEditar = puedeEditar,
                puedeEliminar = puedeEliminar
            )
        }

        if (puedeCrear) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = nuevoNombre,
                onValueChange = { nuevoNombre = it },
                label = { Text("Nombre del perímetro") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    viewModel.crearHijo(nuevoNombre)
                    nuevoNombre = ""
                }) {
                    Text("Agregar Subzona")
                }
                Button(onClick = {
                    viewModel.crearHermano(nuevoNombre)
                    nuevoNombre = ""
                }) {
                    Text("Agregar Zona")
                }
            }
        }

        editarNodo?.let { nodo ->
            AlertDialog(
                onDismissRequest = { editarNodo = null },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.editarPerimetro(nodo.perimetro_id, nombreEditar)
                        editarNodo = null
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { editarNodo = null }) { Text("Cancelar") }
                },
                title = { Text("Editar zona") },
                text = {
                    OutlinedTextField(
                        value = nombreEditar,
                        onValueChange = { nombreEditar = it },
                        label = { Text("Nombre") }
                    )
                }
            )
        }

        nodoCrearSub?.let { padre ->
            AlertDialog(
                onDismissRequest = { nodoCrearSub = null },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.crearSubzona(nombreSubzona, padre)
                        nombreSubzona = ""
                        nodoCrearSub = null
                    }) { Text("Crear") }
                },
                dismissButton = {
                    TextButton(onClick = { nodoCrearSub = null }) { Text("Cancelar") }
                },
                title = { Text("Nueva subzona en ${'$'}{padre.nombre}") },
                text = {
                    OutlinedTextField(
                        value = nombreSubzona,
                        onValueChange = { nombreSubzona = it },
                        label = { Text("Nombre") }
                    )
                }
            )
        }
    }
}

@Composable
fun JerarquiaConAcciones(
    ruta: List<JerarquiaNodo>,
    onSeleccion: (JerarquiaNodo) -> Unit,
    onRetroceder: () -> Unit,
    onEliminar: (JerarquiaNodo) -> Unit,
    onEditar: (JerarquiaNodo) -> Unit,
    onCrearSubzona: (JerarquiaNodo) -> Unit,
    puedeCrear: Boolean,
    puedeEditar: Boolean,
    puedeEliminar: Boolean
) {
    val nodoActual = ruta.lastOrNull() ?: return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ruta.forEachIndexed { index, nodo ->
                Text(nodo.nombre)
                if (index < ruta.lastIndex) {
                    Text(" > ")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        nodoActual.children.forEach { child ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(child.nombre, Modifier.weight(1f))
                    IconButton(onClick = { onSeleccion(child) }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                    if (puedeCrear) {
                        IconButton(onClick = { onCrearSubzona(child) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                    if (puedeEditar) {
                        IconButton(onClick = { onEditar(child) }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    }
                    if (puedeEliminar) {
                        IconButton(onClick = { onEliminar(child) }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            }
        }

        if (ruta.size > 1) {
            TextButton(onClick = onRetroceder) {
                Text("← Regresar")
            }
        }
    }
}
