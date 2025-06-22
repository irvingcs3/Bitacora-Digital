package com.example.bitacoradigital.ui.screens.qr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.HistorialQR
import com.example.bitacoradigital.viewmodel.SeguimientoQRViewModel
import com.example.bitacoradigital.viewmodel.SeguimientoQRViewModelFactory
import com.example.bitacoradigital.ui.components.HomeConfigNavBar

@Composable
fun SeguimientoQRScreen(
    idInvitacion: Int,
    permisos: List<String>,
    navController: NavHostController
) {
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: SeguimientoQRViewModel = viewModel(factory = SeguimientoQRViewModelFactory(prefs, idInvitacion))

    val info by viewModel.info.collectAsState()
    val historial by viewModel.historial.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    val puedeEliminar = "Eliminar Código QR" in permisos
    val puedeModificar = "Modificar Código QR" in permisos

    var modificar by remember { mutableStateOf(false) }
    var diasExtra by remember { mutableStateOf("") }

    var confirmarBorrar by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    error?.let { msg ->
        LaunchedEffect(msg) { snackbarHostState.showSnackbar(msg) }
    }

    LaunchedEffect(Unit) { viewModel.cargarTodo() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            HomeConfigNavBar(
                current = "",
                onHomeClick = { navController.navigate("home") },
                onConfigClick = { navController.navigate("configuracion") }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                info?.let { data ->
                    Text("Seguimiento de invitación", style = MaterialTheme.typography.titleMedium)
                    val estado = data.mensaje ?: data.fase
                    val badgeColor = if (estado == "La visita ha concluido") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                    AssistChip(
                        onClick = {},
                        label = { Text(estado) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = badgeColor)
                    )
                    if (data.checkpointActualNombre == null) {
                        Text("Aún no ha iniciado el recorrido", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Actual: ${data.checkpointActualNombre}")
                    }
                    data.siguientePerimetro?.let {
                        AssistChip(onClick = {}, label = { Text(it) })
                    }
                    if (data.siguiente.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            data.siguiente.forEach { cp ->
                                AssistChip(onClick = {}, label = { Text(cp.nombre) })
                            }
                        }
                    }
                }
                Text("Historial de Seguimiento", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historial, key = { it.fecha + it.checkpoint }) { h ->
                        Text("${h.fecha} - ${h.checkpoint} (${h.perimetro})", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (puedeModificar) {
                        IconButton(onClick = { modificar = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    }
                    if (puedeEliminar) {
                        IconButton(onClick = { confirmarBorrar = true }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    if (modificar) {
        AlertDialog(
            onDismissRequest = { modificar = false },
            confirmButton = {
                TextButton(onClick = {
                    diasExtra.toIntOrNull()?.let { viewModel.modificarCaducidad(it) }
                    modificar = false
                }, enabled = diasExtra.toIntOrNull() != null) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { modificar = false }) { Text("Cancelar") } },
            title = { Text("Modificar caducidad") },
            text = {
                OutlinedTextField(
                    value = diasExtra,
                    onValueChange = { diasExtra = it },
                    label = { Text("Días de duración") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        )
    }

    if (confirmarBorrar) {
        AlertDialog(
            onDismissRequest = { confirmarBorrar = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.borrarCodigo()
                    confirmarBorrar = false
                    navController.popBackStack()
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrar = false }) { Text("Cancelar") } },
            title = { Text("Eliminar QR") },
            text = { Text("¿Seguro que deseas eliminar este código?") }
        )
    }
}
