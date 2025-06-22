package com.example.bitacoradigital.ui.screens.qr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                info?.let { data ->
                    Text(
                        "Seguimiento de invitación",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val estado = data.mensaje ?: data.fase
                    val chipColor = if (estado == "La visita ha concluido")
                        MaterialTheme.colorScheme.surfaceVariant else Color(0xFFD65930)

                    AssistChip(
                        onClick = {},
                        label = { Text(estado) },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = chipColor,
                            labelColor = if (estado == "La visita ha concluido")
                                MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                        )
                    )

                    if (data.checkpointActualNombre == null) {
                        Text(
                            "Aún no ha iniciado el recorrido",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "Actual: ${'$'}{data.checkpointActualNombre}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    data.siguientePerimetro?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text(it) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFD65930),
                                labelColor = Color.White
                            )
                        )
                    }

                    if (data.siguiente.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            data.siguiente.forEach { cp ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(cp.nombre) },
                                    leadingIcon = {
                                        Icon(Icons.Default.LocationOn, contentDescription = null)
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Divider()

                Text(
                    "Historial de Seguimiento",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                if (historial.isEmpty()) {
                    Text(
                        "No hay historial de seguimiento disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.5f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historial, key = { it.fecha + it.checkpoint }) { h ->
                            Text(
                                "${'$'}{h.fecha} - ${'$'}{h.checkpoint} (${ '$' }{h.perimetro })",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (puedeModificar) {
                        IconButton(onClick = { modificar = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar QR")
                        }
                    }
                    if (puedeEliminar) {
                        IconButton(onClick = { confirmarBorrar = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar QR")
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
