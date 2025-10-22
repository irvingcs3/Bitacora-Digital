package com.example.bitacoradigital.ui.screens.lomascountry

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModelFactory
import com.example.bitacoradigital.viewmodel.lomascountry.LomasCountryRegistroViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LomasCountryRegistroScreen(
    perimetroId: Int,
    navController: NavHostController,
    apiService: ApiService,
    sessionPrefs: SessionPreferences
) {
    val viewModel: LomasCountryRegistroViewModel = viewModel(
        factory = RegistroVisitaViewModelFactory(apiService, sessionPrefs, perimetroId, true)
    )

    LomasCountryRegistroContent(viewModel, navController)
}

@Composable
private fun LomasCountryRegistroContent(
    viewModel: LomasCountryRegistroViewModel,
    navController: NavHostController
) {
    val telefono by viewModel.telefono.collectAsState()
    val numeroVerificado by viewModel.numeroVerificado.collectAsState()
    val cargandoRegistro by viewModel.cargandoRegistro.collectAsState()
    val registroCompleto by viewModel.registroCompleto.collectAsState()
    val telefonosDisponibles by viewModel.telefonosDisponibles.collectAsState()
    val errorTelefonos by viewModel.errorTelefonos.collectAsState()

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
            AnimatedContent(
                targetState = numeroVerificado,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                label = "lomas_country_flow",
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { verified ->
                if (!verified) {
                    TelefonosDisponiblesSection(
                        telefonos = telefonosDisponibles,
                        error = errorTelefonos,
                        modifier = Modifier.fillMaxSize(),
                        onTelefonoSelected = { numero ->
                            viewModel.prepararRegistroConTelefono(numero)
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    message = "Número ${formatNumeroLegible(numero)} seleccionado"
                                )
                            }
                        }
                    )
                } else {
                    RegistroLomasCountryDetalle(
                        viewModel = viewModel,
                        navController = navController,
                        telefono = telefono,
                        cargandoRegistro = cargandoRegistro,
                        registroCompleto = registroCompleto,
                        snackbarHostState = snackbarHostState,
                        coroutineScope = coroutineScope,
                        scrollState = scrollState,
                        context = context,
                        onElegirOtroNumero = {
                            viewModel.reiniciar()
                        }
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun TelefonosDisponiblesSection(
    telefonos: List<String>,
    error: String?,
    modifier: Modifier = Modifier,
    onTelefonoSelected: (String) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Selecciona el número de WhatsApp",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "La lista se actualiza automáticamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        if (telefonos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Esperando números disponibles...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(telefonos) { numero ->
                    NumeroWhatsAppCard(
                        numero = numero,
                        onClick = { onTelefonoSelected(numero) }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Actualizando cada 2 segundos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        error?.let { msg ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NumeroWhatsAppCard(
    numero: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val formatted = remember(numero) { formatNumeroLegible(numero) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "numero_card_scale")
    val containerColor by animateColorAsState(
        targetValue = if (pressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "numero_card_color"
    )
    val contentColor = if (pressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = if (pressed) 12.dp else 6.dp,
        shadowElevation = if (pressed) 12.dp else 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = MaterialTheme.colorScheme.primary),
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Número disponible",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = formatted,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Toca para iniciar el registro",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RegistroLomasCountryDetalle(
    viewModel: LomasCountryRegistroViewModel,
    navController: NavHostController,
    telefono: String,
    cargandoRegistro: Boolean,
    registroCompleto: Boolean,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    scrollState: ScrollState,
    context: android.content.Context,
    onElegirOtroNumero: () -> Unit,
) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SelectedPhoneHeader(
            telefono = telefono,
            onElegirOtroNumero = onElegirOtroNumero
        )
        Spacer(Modifier.height(24.dp))

        when {
            cargandoDestino -> {
                CircularProgressIndicator()
            }
            errorDestino != null -> {
                val msg = errorDestino
                LaunchedEffect(msg) {
                    snackbarHostState.showSnackbar(msg!!)
                    viewModel.clearDestinoError()
                }
                Text(
                    text = "No fue posible cargar los destinos",
                    color = MaterialTheme.colorScheme.error
                )
            }
            registroCompleto -> {
                Text("Código fue enviado", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        onElegirOtroNumero()
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
                Text(
                    text = "Selecciona el destino de la visita",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                destinos.forEach { nodo ->
                    val seleccionado = destino?.id == nodo.id
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = if (seleccionado) 8.dp else 2.dp,
                        shadowElevation = if (seleccionado) 8.dp else 0.dp,
                        color = if (seleccionado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { viewModel.seleccionarDestinoLomas(nodo) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                nodo.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (seleccionado) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            RadioButton(
                                selected = seleccionado,
                                onClick = { viewModel.seleccionarDestinoLomas(nodo) }
                            )
                        }
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
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    tonalElevation = if (selected) 6.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.seleccionarResidenteLomas(res) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = { viewModel.seleccionarResidenteLomas(res) }
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(res.name, style = MaterialTheme.typography.bodyLarge)
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

@Composable
private fun SelectedPhoneHeader(
    telefono: String,
    onElegirOtroNumero: () -> Unit,
) {
    val formatted = remember(telefono) { formatNumeroLegible(telefono) }
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Número seleccionado",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = formatted,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onElegirOtroNumero) {
                Text("Elegir otro número")
            }
        }
    }
}

private fun formatNumeroLegible(numero: String): String {
    val digits = numero.filter { it.isDigit() }
    if (digits.isEmpty()) return numero
    return when (digits.length) {
        13 -> "+${digits.substring(0, 2)} ${digits.substring(2, 3)} ${digits.substring(3, 6)} ${digits.substring(6, 9)} ${digits.substring(9)}"
        12 -> "+${digits.substring(0, 2)} ${digits.substring(2, 5)} ${digits.substring(5, 8)} ${digits.substring(8)}"
        10 -> "${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}"
        else -> digits.chunked(3).joinToString(" ")
    }
}
