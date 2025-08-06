package com.example.bitacoradigital.ui.screens.registrovisita

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.model.JerarquiaNodo
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel

@Composable
fun PasoDestinoGeneral(viewModel: RegistroVisitaViewModel) {
    val ruta by viewModel.rutaDestino.collectAsState()
    val seleccionActual by viewModel.destinoSeleccionado.collectAsState()
    val cargando by viewModel.cargandoDestino.collectAsState()
    val error by viewModel.errorDestino.collectAsState()
    val destinoGeneral by viewModel.destinoGeneral.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.cargarJerarquiaDestino()
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text("Paso 4: Selección de Destino", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (cargando) {
                CircularProgressIndicator()
            } else if (error != null) {
                LaunchedEffect(error) {
                    snackbarHostState.showSnackbar(error ?: "")
                    viewModel.clearDestinoError()
                }
            } else {
                NavegacionJerarquia(
                    ruta = ruta,
                    onSeleccion = { viewModel.navegarHacia(it) },
                    onConfirmar = { viewModel.destinoSeleccionado.value = it },
                    onRetroceder = { viewModel.retrocederNivel() }
                )

                if (seleccionActual != null) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = destinoGeneral,
                        onValueChange = { viewModel.destinoGeneral.value = it },
                        label = { Text("Ingrese Destino") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.avanzarPaso() },
                        enabled = destinoGeneral.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Siguiente") }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
