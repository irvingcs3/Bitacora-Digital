// ✅ PasoDestinoLC.kt
package com.example.bitacoradigital.ui.screens.registrovisitalc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.example.bitacoradigital.model.JerarquiaNodo
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.viewmodel.RegistroVisitaLCViewModel
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.clickable

@Composable
fun PasoDestinoLC(viewModel: RegistroVisitaLCViewModel) {
    val jerarquia by viewModel.jerarquia.collectAsState()
    val ruta by viewModel.rutaDestino.collectAsState()
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

        OutlinedTextField(
            value = destinoGeneral,
            onValueChange = { viewModel.destinoGeneral.value = it },
            label = { Text("Ingrese Destino") },
            modifier = Modifier.fillMaxWidth()
        )
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
                onConfirmar = {
                    viewModel.destinoSeleccionado.value = it
                    viewModel.avanzarPaso()
                },
                onRetroceder = { viewModel.retrocederNivel() }
            )
        }
    }
    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun NavegacionJerarquia(
    ruta: List<JerarquiaNodo>,
    onSeleccion: (JerarquiaNodo) -> Unit,
    onConfirmar: (JerarquiaNodo) -> Unit,
    onRetroceder: () -> Unit
) {
    val nodoActual = ruta.lastOrNull() ?: return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ruta.forEachIndexed { index, nodo ->
                Text(nodo.nombre)
                if (index < ruta.lastIndex) {
                    Text(" > ")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        nodoActual.children.forEach { child ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeleccion(child) },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(child.nombre, modifier = Modifier.padding(16.dp))
            }
        }

        if (nodoActual.children.isEmpty()) {
            ElevatedButton(
                onClick = { onConfirmar(nodoActual) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Seleccionar ${nodoActual.nombre}")
            }
        }

        if (ruta.size > 1) {
            TextButton(onClick = onRetroceder) {
                Text("← Regresar")
            }
        }
    }
}
