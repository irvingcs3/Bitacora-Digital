// 📁 ui/screens/RegistroVisitaWizardScreen.kt
package com.example.bitacoradigital.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.ui.components.Stepper
import com.example.bitacoradigital.ui.screens.registrovisita.PasoAutorizacion
import com.example.bitacoradigital.ui.screens.registrovisita.PasoConfirmacion
import com.example.bitacoradigital.ui.screens.registrovisita.PasoDestino
import com.example.bitacoradigital.ui.screens.registrovisita.PasoDocumento

import com.example.bitacoradigital.ui.screens.registrovisita.PasoFinal
import com.example.bitacoradigital.ui.screens.registrovisita.PasoFotos
import com.example.bitacoradigital.ui.screens.registrovisita.PasoTelefono
import com.example.bitacoradigital.ui.screens.registrovisita.PasoVerificacion
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModelFactory



@Composable
fun RegistroVisitaWizardScreen(
    perimetroId: Int,
    navController: NavHostController,
    isLomasCountry: Boolean = false
) {
    val context = LocalContext.current
    val apiService = remember { ApiService.create() }
    val sessionPrefs = remember { SessionPreferences(context) }

    val viewModel: RegistroVisitaViewModel = viewModel(
        factory = RegistroVisitaViewModelFactory(apiService, sessionPrefs, perimetroId, isLomasCountry)
    )

    if (isLomasCountry) {
        LomasCountryRegistroContent(viewModel, navController)
    } else {
        val pasoActual by viewModel.pasoActual.collectAsState()

        Scaffold(
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
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Stepper(pasoActual, totalPasos = 8)
                Spacer(Modifier.height(24.dp))

                when (pasoActual) {
                    1 -> PasoTelefono(viewModel, navController, false)
                    2 -> PasoDocumento(viewModel)
                    3 -> PasoVerificacion(viewModel)
                    4 -> PasoDestino(viewModel)
                    5 -> PasoFotos(viewModel)
                    6 -> PasoConfirmacion(viewModel)
                    7 -> PasoAutorizacion(viewModel)
                    8 -> PasoFinal(viewModel, navController)
                }
            }
        }
    }
}

@Composable
private fun LomasCountryRegistroContent(
    viewModel: RegistroVisitaViewModel,
    navController: NavHostController
) {
    val telefono by viewModel.telefono.collectAsState()
    val numeroVerificado by viewModel.numeroVerificado.collectAsState()
    val cargandoRegistro by viewModel.cargandoRegistro.collectAsState()
    val registroCompleto by viewModel.registroCompleto.collectAsState()


    var verificando by remember { mutableStateOf(false) }
    var errorVerificacion by remember { mutableStateOf<String?>(null) }


    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.cargarJerarquiaDestino()
    }

    Scaffold(
        bottomBar = {
            HomeConfigNavBar(
                current = "",
                onHomeClick = { navController.navigate("home") },
                onConfigClick = { navController.navigate("configuracion") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = telefono,
                    onValueChange = {
                        if (it.length <= 10) viewModel.telefono.value =
                            it.filter { c -> c.isDigit() }
                    },
                    label = { Text("Número de WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                if (!numeroVerificado) {
                    Button(
                        onClick = {
                            verificando = true
                            errorVerificacion = null
                            coroutineScope.launch {
                                val existe = viewModel.verificarNumeroWhatsApp(telefono)
                                if (existe) {
                                    viewModel.numeroVerificado.value = true
                                    viewModel.cargarJerarquiaDestino()
                                } else {
                                    errorVerificacion = "Número inválido o no verificado"
                                    viewModel.reiniciar()
                                }
                                verificando = false
                            }
                        },
                        enabled = telefono.length == 10 && !verificando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (verificando) "Verificando..." else "Verificar número")
                    }
                    if (verificando) {
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }
                } else {
                    Text("✅ Número verificado", color = MaterialTheme.colorScheme.primary)
                }

                errorVerificacion?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    LaunchedEffect(msg) {
                        snackbarHostState.showSnackbar(msg)
                        errorVerificacion = null
                    }
                }

                if (numeroVerificado) {
                    Spacer(Modifier.height(24.dp))
                    val destinos by viewModel.nodosHoja.collectAsState()
                    val destino by viewModel.destinoLomasSeleccionado.collectAsState()
                    val cargandoDestino by viewModel.cargandoDestino.collectAsState()
                    val errorDestino by viewModel.errorDestino.collectAsState()
                    val residentes by viewModel.residentesDestino.collectAsState()
                    val residenteSeleccionado by viewModel.residenteSeleccionado.collectAsState()
                    val cargandoResidentes by viewModel.cargandoResidentes.collectAsState()
                    val errorResidentes by viewModel.errorResidentes.collectAsState()
                    val invitanteId by viewModel.invitanteId.collectAsState()
                    val cargandoCredencial by viewModel.cargandoCredencial.collectAsState()
                    val codigoCredencial by viewModel.codigoErrorCredencial.collectAsState()

                    when {
                        cargandoDestino -> {
                            CircularProgressIndicator()
                        }
                        errorDestino != null -> {
                            val msg = errorDestino
                            LaunchedEffect(msg) {
                                snackbarHostState.showSnackbar(msg)
                                viewModel.clearDestinoError()
                            }
                        }
                        registroCompleto -> {
                            Text("Código fue enviado", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.reiniciar()
                                    verificando = false
                                    errorVerificacion = null
                                    navController.navigate("lomascountry/manual") {
                                        popUpTo("lomascountry/manual") { inclusive = true }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Registrar otra visita")
                            }
                        }
                        destinos.isEmpty() -> {
                            Text("No hay destinos disponibles")
                        }
                        else -> {
                            destinos.forEach { nodo ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = destino?.id == nodo.id,
                                            onClick = { viewModel.seleccionarDestinoLomas(nodo) }
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = destino?.id == nodo.id,
                                        onClick = { viewModel.seleccionarDestinoLomas(nodo) }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(nodo.name)
                                }
                            }
                            destino?.let {
                                Spacer(Modifier.height(16.dp))
                                when {
                                    cargandoResidentes -> {
                                        CircularProgressIndicator()
                                    }
                                    errorResidentes != null -> {
                                        val msg = errorResidentes ?: ""
                                        LaunchedEffect(msg) {
                                            snackbarHostState.showSnackbar("Error al cargar residentes: $msg")
                                            viewModel.clearErrorResidentes()
                                        }
                                        Text(
                                            text = "Error al cargar residentes",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    residentes.isEmpty() -> {
                                        Text("No hay residentes registrados para este destino")
                                    }
                                    residentes.size == 1 -> {
                                        val seleccionado = residenteSeleccionado ?: residentes.first()
                                        Text(
                                            text = "Residente asignado: ${seleccionado.name}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    else -> {
                                        Text(
                                            "Selecciona el residente que autoriza la visita",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        residentes.forEach { res ->
                                            val selected = residenteSeleccionado?.idPersona == res.idPersona
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .selectable(
                                                        selected = selected,
                                                        onClick = { viewModel.seleccionarResidenteLomas(res) }
                                                    )
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = selected,
                                                    onClick = { viewModel.seleccionarResidenteLomas(res) }
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(res.name)
                                                    if (res.email.isNotBlank()) {
                                                        Text(
                                                            res.email,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (residenteSeleccionado == null) {
                                            Text(
                                                "Selecciona un residente para continuar",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                if (cargandoCredencial) {
                                    Spacer(Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.registrarVisita(context) { exito ->
                                        if (!exito) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Error al registrar")
                                            }
                                        }
                                    }
                                },
                                enabled = destino != null && invitanteId != null && !cargandoRegistro && !cargandoResidentes && !cargandoCredencial,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (cargandoRegistro) "Enviando..." else "Enviar código")
                            }
                        }
                    }
                    codigoCredencial?.let { code ->
                        LaunchedEffect(code) {
                            val message = when (code) {
                                422 -> "No se encontró credencial para el residente seleccionado"
                                -1 -> "Error al obtener la credencial. Intenta nuevamente."
                                else -> "Error al obtener credencial ($code)"
                            }
                            snackbarHostState.showSnackbar(message)
                            viewModel.limpiarErrorCredencial()
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
