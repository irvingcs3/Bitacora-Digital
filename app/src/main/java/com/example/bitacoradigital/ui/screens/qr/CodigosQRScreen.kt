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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import com.example.bitacoradigital.viewmodel.HomeViewModel

private enum class SortOption { NEWEST, OLDEST, NAME_ASC, NAME_DESC }

@Composable
fun CodigosQRScreen(
    homeViewModel: HomeViewModel,
    permisos: List<String>,
    navController: NavHostController
) {
    val perimetroSeleccionado by homeViewModel.perimetroSeleccionado.collectAsState()
    val perimetroId = perimetroSeleccionado?.perimetroId ?: return
    val empresaId = perimetroSeleccionado?.empresaId ?: return
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: CodigosQRViewModel =
        viewModel(factory = CodigosQRViewModelFactory(prefs, perimetroId, empresaId))

    val codigos by viewModel.codigos.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    val puedeVer = "Ver Códigos QR" in permisos
    val puedeEliminar = "Eliminar Código QR" in permisos
    val puedeModificar = "Modificar Código QR" in permisos

    LaunchedEffect(Unit) { if (puedeVer) viewModel.cargarCodigos() }

    var modificar by remember { mutableStateOf<CodigoQR?>(null) }
    var diasExtra by remember { mutableStateOf("") }

    val pageSize = 20
    var currentPage by remember { mutableStateOf(0) }
    var sortOption by remember { mutableStateOf(SortOption.NEWEST) }
    var nameFilter by remember { mutableStateOf("") }
    var dateFilter by remember { mutableStateOf("") }
    var timeFilter by remember { mutableStateOf("") }

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
                    val processedCodigos = remember(codigos, sortOption, nameFilter, dateFilter, timeFilter) {
                        codigos
                            .filter { nameFilter.isBlank() || it.nombre_invitado.contains(nameFilter, ignoreCase = true) }
                            .filter { dateFilter.isBlank() || it.periodo_activo.contains(dateFilter) }
                            .filter { timeFilter.isBlank() || it.periodo_activo.contains(timeFilter) }
                            .let { list ->
                                when (sortOption) {
                                    SortOption.NEWEST -> list.sortedByDescending { it.id_invitacion }
                                    SortOption.OLDEST -> list.sortedBy { it.id_invitacion }
                                    SortOption.NAME_ASC -> list.sortedBy { it.nombre_invitado }
                                    SortOption.NAME_DESC -> list.sortedByDescending { it.nombre_invitado }
                                }
                            }
                    }
                    val pageCount = (processedCodigos.size + pageSize - 1) / pageSize
                    val paginated = processedCodigos.drop(currentPage * pageSize).take(pageSize)

                    Column(Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nameFilter,
                                onValueChange = { nameFilter = it; currentPage = 0 },
                                label = { Text("Nombre") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = dateFilter,
                                onValueChange = { dateFilter = it; currentPage = 0 },
                                label = { Text("Fecha") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = timeFilter,
                                onValueChange = { timeFilter = it; currentPage = 0 },
                                label = { Text("Hora") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        var sortMenu by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { sortMenu = true }) {
                                Text(
                                    when (sortOption) {
                                        SortOption.NEWEST -> "Más recientes"
                                        SortOption.OLDEST -> "Más antiguos"
                                        SortOption.NAME_ASC -> "Nombre A-Z"
                                        SortOption.NAME_DESC -> "Nombre Z-A"
                                    }
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Más recientes") },
                                    onClick = {
                                        sortOption = SortOption.NEWEST
                                        currentPage = 0
                                        sortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Más antiguos") },
                                    onClick = {
                                        sortOption = SortOption.OLDEST
                                        currentPage = 0
                                        sortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nombre A-Z") },
                                    onClick = {
                                        sortOption = SortOption.NAME_ASC
                                        currentPage = 0
                                        sortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nombre Z-A") },
                                    onClick = {
                                        sortOption = SortOption.NAME_DESC
                                        currentPage = 0
                                        sortMenu = false
                                    }
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(paginated, key = { it.id_invitacion }) { qr ->
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                            }
                            Text("${currentPage + 1} / $pageCount", modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(
                                onClick = { if (currentPage < pageCount - 1) currentPage++ },
                                enabled = currentPage < pageCount - 1
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
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
