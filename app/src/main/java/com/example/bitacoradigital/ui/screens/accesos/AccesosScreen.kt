package com.example.bitacoradigital.ui.screens.accesos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.viewmodel.CheckpointsViewModel
import com.example.bitacoradigital.viewmodel.CheckpointsViewModelFactory
import com.example.bitacoradigital.viewmodel.HomeViewModel
import com.example.bitacoradigital.model.Checkpoint
import com.example.bitacoradigital.ui.components.HomeConfigNavBar

@Composable
fun AccesosScreen(homeViewModel: HomeViewModel, permisos: List<String>, navController: NavHostController) {
    val perimetroSeleccionado by homeViewModel.perimetroSeleccionado.collectAsState()
    val perimetroId = perimetroSeleccionado?.perimetroId ?: return
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: CheckpointsViewModel = viewModel(factory = CheckpointsViewModelFactory(prefs, perimetroId))

    val checkpoints by viewModel.checkpoints.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    var nuevoNombre by remember { mutableStateOf("") }
    var nuevoTipo by remember { mutableStateOf("") }
    var editar by remember { mutableStateOf<Checkpoint?>(null) }
    var editarNombre by remember { mutableStateOf("") }
    var editarTipo by remember { mutableStateOf("") }

    val puedeCrear = "Crear Checkpoint" in permisos
    val puedeEditar = "Editar Checkpoint" in permisos
    val puedeEliminar = "Eliminar Checkpoint" in permisos

    LaunchedEffect(Unit) { viewModel.cargarCheckpoints() }

    val snackbarHostState = remember { SnackbarHostState() }
    error?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg)
            viewModel.cargarCheckpoints()
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
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(checkpoints) { cp ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(cp.nombre)
                                    Text(cp.tipo, style = MaterialTheme.typography.bodySmall)
                                }
                                if (puedeEditar) {
                                    IconButton(onClick = { editar = cp; editarNombre = cp.nombre; editarTipo = cp.tipo }) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    }
                                }
                                if (puedeEliminar) {
                                    IconButton(onClick = { viewModel.eliminarCheckpoint(cp.checkpoint_id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
                if (puedeCrear) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = nuevoNombre,
                        onValueChange = { nuevoNombre = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nuevoTipo,
                        onValueChange = { nuevoTipo = it },
                        label = { Text("Tipo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.crearCheckpoint(nuevoNombre, nuevoTipo); nuevoNombre = ""; nuevoTipo = "" }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar Checkpoint")
                    }
                }
            }

            editar?.let { cp ->
                AlertDialog(
                    onDismissRequest = { editar = null },
                    confirmButton = {
                        TextButton(onClick = { viewModel.actualizarCheckpoint(cp.checkpoint_id, editarNombre, editarTipo); editar = null }) { Text("Guardar") }
                    },
                    dismissButton = { TextButton(onClick = { editar = null }) { Text("Cancelar") } },
                    title = { Text("Editar checkpoint") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = editarNombre, onValueChange = { editarNombre = it }, label = { Text("Nombre") })
                            OutlinedTextField(value = editarTipo, onValueChange = { editarTipo = it }, label = { Text("Tipo") })
                        }
                    }
                )
            }
        }
    }
}
