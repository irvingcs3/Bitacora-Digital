package com.example.bitacoradigital.ui.screens.residentes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.animateItemPlacement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
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
                FloatingActionButton(
                    onClick = { mostrarDialogo = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Residentes",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.cargarResidentes() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                }
            }
            if (cargando) {
                CircularProgressIndicator()
            } else {
                if (residentes.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("No hay residentes registrados")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(residentes, key = { it.id }) { res ->
                            var visible by remember { mutableStateOf(true) }
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItemPlacement(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    elevation = CardDefaults.elevatedCardElevation(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(res.name, style = MaterialTheme.typography.titleMedium)
                                            Text(res.email, style = MaterialTheme.typography.bodySmall)
                                            Text(res.perimeterName, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (puedeEliminar) {
                                            IconButton(onClick = {
                                                visible = false
                                                viewModel.eliminarResidente(res.id, res.perimeterId)
                                            }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
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
        var emailError by remember { mutableStateOf(false) }
        val formValido = correo.contains("@") && nodoSeleccionado != null && !emailError
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            confirmButton = {
                Button(
                    onClick = {
                        val nodoId = nodoSeleccionado?.id ?: return@Button
                        viewModel.invitarResidente(correo, nodoId)
                        correo = ""
                        nodoSeleccionado = null
                        emailError = false
                        mostrarDialogo = false
                    },
                    enabled = formValido,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Invitar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            },
            title = { Text("Invitar Residente") },
            shape = RoundedCornerShape(16.dp),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = correo,
                        onValueChange = {
                            correo = it
                            emailError = it.isNotBlank() && !it.contains("@")
                        },
                        label = { Text("Correo electrónico") },
                        isError = emailError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (emailError) {
                        Text(
                            "Correo inválido",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
