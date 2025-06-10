// ✅ PasoDestino.kt
package com.example.bitacoradigital.ui.screens.registrovisita

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.bitacoradigital.model.JerarquiaNodo

import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel

@Composable
fun PasoDestino(viewModel: RegistroVisitaViewModel) {
    val jerarquia by viewModel.jerarquia.collectAsState()
    val seleccionActual by viewModel.destinoSeleccionado.collectAsState()
    val cargando by viewModel.cargandoDestino.collectAsState()
    val error by viewModel.errorDestino.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarJerarquiaDestino()

    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Paso 4: Selección de Destino", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (cargando) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        } else {
            JerarquiaSelector(nodo = jerarquia, onSeleccionFinal = {
                viewModel.destinoSeleccionado.value = it // ✔️ Correcto
                viewModel.avanzarPaso()
            })
        }
    }
}

@Composable
fun JerarquiaSelector(nodo: JerarquiaNodo?, onSeleccionFinal: (JerarquiaNodo) -> Unit) {
    if (nodo == null) return

    var seleccionado by remember { mutableStateOf<JerarquiaNodo?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Selecciona en: ${nodo.nombre}", style = MaterialTheme.typography.bodyMedium)
        nodo.children.forEach { child ->
            Button(
                onClick = { seleccionado = child },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(child.nombre)
            }
        }
        seleccionado?.let {
            Spacer(modifier = Modifier.height(8.dp))
            JerarquiaSelector(nodo = it, onSeleccionFinal = onSeleccionFinal)
        } ?: if (nodo.children.isEmpty()) {
            Button(
                onClick = { onSeleccionFinal(nodo) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Seleccionar ${nodo.nombre}")
            }
        } else {

        }
    }
}
