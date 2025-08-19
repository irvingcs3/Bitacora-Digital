// 📁 ui/screens/RegistroVisitaWizardScreen.kt
package com.example.bitacoradigital.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
    val nombre by viewModel.nombre.collectAsState()
    val paterno by viewModel.apellidoPaterno.collectAsState()
    val materno by viewModel.apellidoMaterno.collectAsState()
    val cargandoRegistro by viewModel.cargandoRegistro.collectAsState()
    val registroCompleto by viewModel.registroCompleto.collectAsState()


    var verificando by remember { mutableStateOf(false) }
    var errorVerificacion by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current


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
                                    val datosOk = viewModel.cargarDatosCredencial()
                                    if (datosOk) {
                                        viewModel.numeroVerificado.value = true
                                    } else {
                                        errorVerificacion = "Error obteniendo credencial"
                                        viewModel.reiniciar()
                                    }
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
                    if (!registroCompleto) {
                        Spacer(Modifier.height(24.dp))
                        Text("Verifica tus Datos", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { viewModel.nombre.value = it },
                            label = { Text("Nombre(s)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = paterno,
                            onValueChange = { viewModel.apellidoPaterno.value = it },
                            label = { Text("Apellido Paterno") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = materno,
                            onValueChange = { viewModel.apellidoMaterno.value = it },
                            label = { Text("Apellido Materno") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(24.dp))
                        Text("Confirma tu Registro", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📱 Teléfono: $telefono")
                                Text("👤 Nombre: $nombre $paterno $materno")
                            }
                        }
                        Spacer(Modifier.height(24.dp))
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
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !cargandoRegistro
                        ) {
                            Text(if (cargandoRegistro) "Registrando..." else "Registrar visita")
                        }
                    } else {
                        Spacer(Modifier.height(24.dp))
                        Text("✅ Registro completado", style = MaterialTheme.typography.titleLarge)
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
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

