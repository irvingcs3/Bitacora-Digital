package com.example.bitacoradigital.ui.screens.amenidades

import android.app.TimePickerDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import java.time.*
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
enum class AreaComun(val label: String, val icon: ImageVector) {
    SalonRecreativo("Salón Recreativo", Icons.Default.Celebration),
    AreaDeJuegos("Área de Juegos", Icons.Default.SportsEsports),
    CampoDeFutbol("Campo de Fútbol", Icons.Default.SportsSoccer),
    Alberca("Alberca", Icons.Default.Pool)
}

@RequiresApi(Build.VERSION_CODES.O)
data class Actividad(
    val fecha: LocalDate,
    val hora: LocalTime,
    val titulo: String,
    val area: AreaComun,
    val registradoPor: String
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioActividadesScreen(navController: NavHostController) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDialog by remember { mutableStateOf(false) }
    var titulo by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf(LocalTime.now()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var area by remember { mutableStateOf(AreaComun.SalonRecreativo) }
    var nombre by remember { mutableStateOf("") }
    val actividades = remember { mutableStateListOf<Actividad>() }
    var filtroMes: Month? by remember { mutableStateOf(null) }

    val actividadesFiltradas = actividades.filter { filtroMes == null || it.fecha.month == filtroMes }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Calendario de Actividades") })
        },
        bottomBar = {
            HomeConfigNavBar(
                current = "",
                onHomeClick = { navController.navigate("home") },
                onConfigClick = { navController.navigate("configuracion") }
            )
        }
    ) { innerPadding ->
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        var skipInitialSelection by remember { mutableStateOf(true) }
        LaunchedEffect(datePickerState.selectedDateMillis) {
            val millis = datePickerState.selectedDateMillis
            if (millis != null) {
                val date = Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                if (!skipInitialSelection) {
                    selectedDate = date
                    titulo = ""
                    hora = LocalTime.now()
                    area = AreaComun.SalonRecreativo
                    nombre = ""
                    showDialog = true
                }
                skipInitialSelection = false
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Filtrar por mes:")
                    Spacer(Modifier.width(8.dp))
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(filtroMes?.name ?: "Todos")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Todos") }, onClick = {
                                filtroMes = null
                                expanded = false
                            })
                            Month.values().forEach { m ->
                                DropdownMenuItem(text = { Text(m.name) }, onClick = {
                                    filtroMes = m
                                    expanded = false
                                })
                            }
                        }
                    }
                }
            }
            items(actividadesFiltradas) { actividad ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(actividad.area.icon, contentDescription = actividad.area.label)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(actividad.titulo, fontWeight = FontWeight.Bold)
                            Text(actividad.area.label)
                            Text(
                                "${actividad.fecha} - ${actividad.hora.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                            )
                            Text("Registrado por: ${actividad.registradoPor}")
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nueva actividad") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") }
                    )
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        label = { Text("Título") }
                    )
                    var expandedArea by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expandedArea = true }) {
                            Icon(area.icon, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(area.label)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = expandedArea, onDismissRequest = { expandedArea = false }) {
                            AreaComun.values().forEach { a ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(a.icon, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(a.label)
                                        }
                                    },
                                    onClick = {
                                        area = a
                                        expandedArea = false
                                    }
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { showTimePicker = true }) {
                            Text(hora.format(DateTimeFormatter.ofPattern("HH:mm")))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    actividades.add(Actividad(selectedDate, hora, titulo, area, nombre))
                    showDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            context,
            { _, h, m ->
                hora = LocalTime.of(h, m)
                showTimePicker = false
            },
            hora.hour,
            hora.minute,
            true
        ).show()
    }
}
