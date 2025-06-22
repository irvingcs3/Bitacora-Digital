package com.example.bitacoradigital.ui.screens.qr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import com.example.bitacoradigital.model.CodigoQR
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.viewmodel.CodigosQRViewModel
import com.example.bitacoradigital.viewmodel.CodigosQRViewModelFactory

@Composable
fun CodigosQRScreen(
    perimetroId: Int,
    permisos: List<String>,
    navController: NavHostController
) {
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: CodigosQRViewModel = viewModel(factory = CodigosQRViewModelFactory(prefs, perimetroId))

    val codigos by viewModel.codigos.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    val puedeVer = "Ver Códigos QR" in permisos
    val puedeEliminar = "Eliminar Código QR" in permisos
    val puedeModificar = "Modificar Código QR" in permisos

    LaunchedEffect(Unit) { if (puedeVer) viewModel.cargarCodigos() }

    var modificar by remember { mutableStateOf<CodigoQR?>(null) }
    var diasExtra by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    error?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg)
            viewModel.cargarCodigos()
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
                    text = "Códigos QR",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.cargarCodigos() }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
            }
            if (!puedeVer) {
                Text("Sin permisos para ver códigos")
            } else if (cargando) {
                CircularProgressIndicator()
            } else {
                if (codigos.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin códigos activos")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(codigos, key = { it.id_invitacion }) { qr ->
                            QRCard(
                                qr = qr,
                                puedeModificar = puedeModificar,
                                puedeEliminar = puedeEliminar,
                                onSeguimiento = {
                                    navController.navigate("qr/seguimiento/${qr.id_invitacion}")
                                },
                                onModificar = { modificar = qr; diasExtra = "" },
                                onEliminar = { viewModel.borrarCodigo(qr.id_invitacion) }
                            )
                        }
                    }
                }
            }
        }
    }

    modificar?.let { qr ->
        AlertDialog(
            onDismissRequest = { modificar = null },
            confirmButton = {
                TextButton(onClick = {
                    diasExtra.toIntOrNull()?.let { viewModel.modificarCaducidad(qr.id_invitacion, it) }
                    modificar = null
                }, enabled = diasExtra.toIntOrNull() != null) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { modificar = null }) { Text("Cancelar") }
            },
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
}

@Composable
private fun QRCard(
    qr: CodigoQR,
    puedeModificar: Boolean,
    puedeEliminar: Boolean,
    onSeguimiento: () -> Unit,
    onModificar: () -> Unit,
    onEliminar: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)
    val estadoColor = if (qr.estado.lowercase() == "activo") Color(0xFFC8F7C5) else Color(0xFFF7C5C5)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(qr.nombre_invitado, style = MaterialTheme.typography.titleMedium)
                    Text(
                        qr.destino,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Surface(
                        color = estadoColor,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            qr.estado,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            AnimatedVisibility(expanded) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Periodo activo: ${qr.periodo_activo}", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onSeguimiento, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ver Seguimiento")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (puedeModificar) {
                            IconButton(onClick = onModificar) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        }
                        if (puedeEliminar) {
                            IconButton(onClick = onEliminar) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}
