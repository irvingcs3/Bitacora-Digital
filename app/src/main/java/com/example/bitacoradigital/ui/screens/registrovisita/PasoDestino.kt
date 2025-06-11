// ✅ PasoDestino.kt
package com.example.bitacoradigital.ui.screens.registrovisita

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.example.bitacoradigital.model.JerarquiaNodo
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel

@Composable
fun PasoDestino(viewModel: RegistroVisitaViewModel) {
    val jerarquia by viewModel.jerarquia.collectAsState()
    val ruta by viewModel.rutaDestino.collectAsState()
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
            Button(
                onClick = { onSeleccion(child) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(child.nombre)
            }
        }

        if (nodoActual.children.isEmpty()) {
            Button(
                onClick = { onConfirmar(nodoActual) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
