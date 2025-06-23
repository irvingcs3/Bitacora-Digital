package com.example.bitacoradigital.ui.screens.qr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
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
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.ui.theme.BrandOrange
import com.example.bitacoradigital.viewmodel.SeguimientoQRViewModel
import com.example.bitacoradigital.viewmodel.SeguimientoQRViewModelFactory

@Composable
fun SeguimientoQRScreen(
    idInvitacion: Int,
    permisos: List<String>,
    navController: NavHostController
) {
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: SeguimientoQRViewModel =
        viewModel(factory = SeguimientoQRViewModelFactory(prefs, idInvitacion))

    val info by viewModel.info.collectAsState()
    val historial by viewModel.historial.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    val puedeEliminar = "Eliminar Código QR" in permisos
    val puedeModificar = "Modificar Código QR" in permisos

    var showModificar by remember { mutableStateOf(false) }
    var diasExtraText by remember { mutableStateOf("") }
    var showBorrar by remember { mutableStateOf(false) }

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
        if (cargando) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seguimiento de invitación",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                info?.let { data ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Estado Actual",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                val estado = data.mensaje ?: data.fase
                                val chipBg = when (estado) {
                                    "Ingresando" -> Color(0xFFD1FAE5)
                                    "Concluido", "La visita ha concluido" -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val chipText = when (estado) {
                                    "Ingresando" -> Color(0xFF065F46)
                                    "Concluido", "La visita ha concluido" -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                AssistChip(
                                    onClick = {},
                                    label = { Text(estado) },
                                    leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = chipBg,
                                        labelColor = chipText
                                    )
                                )
                            }

                            if (data.checkpointActualNombre == null) {
                                Text(
                                    text = "⚠️ Aún no ha iniciado el recorrido",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = "Actual: ${data.checkpointActualNombre}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    data.siguientePerimetro?.let { next ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Siguiente destino:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text(next) },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = BrandOrange,
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    if (data.siguiente.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Próximos checkpoints:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    data.siguiente.forEach { cp ->
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(cp.nombre) },
                                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "\uD83D\uDD50 Historial de seguimiento",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (historial.isEmpty()) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No hay historial de seguimiento disponible",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.5f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(historial, key = { it.fecha + it.checkpoint }) { h ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(),
                                        elevation = CardDefaults.cardElevation(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(h.fecha, style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "${h.checkpoint} (${h.perimetro})",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (puedeModificar) {
                        IconButton(onClick = { showModificar = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }
                    if (puedeEliminar) {
                        IconButton(onClick = { showBorrar = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }
        }
    }

    if (showModificar) {
        AlertDialog(
            onDismissRequest = { showModificar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        diasExtraText.toIntOrNull()?.let { viewModel.modificarCaducidad(it) }
                        showModificar = false
                    },
                    enabled = diasExtraText.toIntOrNull() != null
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showModificar = false }) { Text("Cancelar") } },
            title = { Text("Modificar caducidad") },
            text = {
                OutlinedTextField(
                    value = diasExtraText,
                    onValueChange = { diasExtraText = it },
                    label = { Text("Días extra") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        )
    }

    if (showBorrar) {
        AlertDialog(
            onDismissRequest = { showBorrar = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.borrarCodigo()
                    showBorrar = false
                    navController.popBackStack()
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { showBorrar = false }) { Text("Cancelar") } },
            title = { Text("Eliminar QR") },
            text = { Text("¿Seguro que deseas eliminar este código?") }
        )
    }
}

