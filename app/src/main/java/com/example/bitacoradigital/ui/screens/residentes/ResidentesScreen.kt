package com.example.bitacoradigital.ui.screens.residentes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.NodoHoja
import com.example.bitacoradigital.viewmodel.HomeViewModel
import com.example.bitacoradigital.viewmodel.ResidentesViewModel
import com.example.bitacoradigital.viewmodel.ResidentesViewModelFactory
import com.example.bitacoradigital.ui.components.HomeConfigNavBar

@Composable
fun ResidentesScreen(
    homeViewModel: HomeViewModel,
    permisos: List<String>,
    navController: NavHostController
) {
    val perimetroSeleccionado by homeViewModel.perimetroSeleccionado.collectAsState()
    val perimetroId = perimetroSeleccionado?.perimetroId ?: return
    val empresaId = perimetroSeleccionado?.empresaId ?: return
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: ResidentesViewModel = viewModel(
        factory = ResidentesViewModelFactory(prefs, perimetroId, empresaId)
    )

    val residentes by viewModel.residentes.collectAsState()
    val nodosHoja by viewModel.nodosHoja.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    val puedeCrear = "Crear Residente" in permisos
    val puedeEliminar = "Eliminar Residente" in permisos

    var mostrarDialogo by remember { mutableStateOf(false) }
    var correo by remember { mutableStateOf("") }
    var nodoSeleccionado by remember { mutableStateOf<NodoHoja?>(null) }

    LaunchedEffect(Unit) {
        viewModel.cargarResidentes()
        if (puedeCrear) viewModel.cargarNodosHoja()
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (puedeCrear) {
                FloatingActionButton(onClick = { mostrarDialogo = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Residentes",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (cargando) {
                CircularProgressIndicator()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(residentes) { res ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(res.name)
                                    Text(res.email, style = MaterialTheme.typography.bodySmall)
                                    Text(res.perimeterName, style = MaterialTheme.typography.bodySmall)
                                }
                                if (puedeEliminar) {
                                    IconButton(onClick = { viewModel.eliminarResidente(res.id, res.perimeterId) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            confirmButton = {
                TextButton(onClick = {
                    val nodoId = nodoSeleccionado?.id
                    if (!correo.contains("@") || nodoId == null) return@TextButton
                    viewModel.invitarResidente(correo, nodoId)
                    correo = ""
                    nodoSeleccionado = null
                    mostrarDialogo = false
                }) { Text("Invitar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") } },
            title = { Text("Invitar Residente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            readOnly = true,
                            value = nodoSeleccionado?.name ?: "",
                            onValueChange = {},
                            label = { Text("Perímetro") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            nodosHoja.forEach { nodo ->
                                DropdownMenuItem(
                                    text = { Text(nodo.name) },
                                    onClick = {
                                        nodoSeleccionado = nodo
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
