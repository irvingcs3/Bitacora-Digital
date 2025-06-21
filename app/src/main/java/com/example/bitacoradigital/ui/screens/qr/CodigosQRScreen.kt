package com.example.bitacoradigital.ui.screens.qr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.bitacoradigital.model.CodigoQR
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.util.toReadableDate
import com.example.bitacoradigital.viewmodel.CodigosQRViewModel
import com.example.bitacoradigital.viewmodel.CodigosQRViewModelFactory

@Composable
fun CodigosQRScreen(permisos: List<String>, navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: CodigosQRViewModel = viewModel(factory = CodigosQRViewModelFactory(prefs))

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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(codigos, key = { it.id_invitacion }) { qr ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(qr.telefono)
                                        Text("Inicio: ${qr.timestamp_inicio.toReadableDate()}", style = MaterialTheme.typography.bodySmall)
                                        Text("Fin: ${qr.timestamp_final.toReadableDate()}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (puedeModificar) {
                                        IconButton(onClick = { modificar = qr; diasExtra = "" }) {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        }
                                    }
                                    if (puedeEliminar) {
                                        IconButton(onClick = { viewModel.borrarCodigo(qr.id_invitacion) }) {
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
